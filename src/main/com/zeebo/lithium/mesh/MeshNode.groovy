package com.zeebo.lithium.mesh

import com.google.gson.Gson
import com.zeebo.lithium.message.ChatMessageHandler
import com.zeebo.lithium.message.MessageBuffer
import com.zeebo.lithium.message.MessageHandler
import com.zeebo.lithium.message.SystemMessageHandler
import com.zeebo.lithium.util.ReaderCategory

import groovy.util.logging.Log

import java.util.concurrent.ConcurrentHashMap

/**
 * User: Eric
 * Date: 9/8/14
 */
@Log
class MeshNode {

	static final Gson gson = new Gson()
	static String delimiter = '\0'

	ServerSocket serverSocket
	String serverId

	def sockets = [:] as ConcurrentHashMap
	def messages = new MessageBuffer()

	private List<MessageHandler> messageHandlers = []

	MeshNode() {
		this(40026)
	}

	MeshNode(int port) {
		this(InetAddress.localHost.hostAddress + ':' + port, port)
	}

	MeshNode(String serverName, int port) {

		serverId = serverName
		serverSocket = new ServerSocket(port)

		addMessageHandler(new SystemMessageHandler())

		log.info "$serverId started on port $port"
	}

	def addMessageHandler(MessageHandler handler) {
		def collision = null
		if (!(collision = messageHandlers.find { it.typeRange.intersect(handler.typeRange) })) {
			handler.node = this
			messageHandlers << handler
		} else {
			log.severe "Cannot add handler ${handler.class}. Collision on type range with ${collision.class}: ${collision.typeRange.from}-${collision.typeRange.to}"
		}
	}

	def listen() {
		Thread.start {
			while (true) {
				serverSocket.accept(true, handleSocket)
			}
		}
	}

	def connect(String hostname, int port) {

		log.info "$serverId: attempting to connect to $hostname:$port"

		Socket socket = new Socket(hostname, port)
		log.fine "$serverId: connection established ${socket.localSocketAddress} to $hostname:$port"

		Thread.start {
			handleSocket socket
		}
	}

	def send(PrintWriter output, Message message) {

		if (!message.sender) {
			message.sender = serverId
		}

		if (message.messageType) {
			messages.setMessage(message)
		}

		output.print(message.toString() + delimiter)
		output.flush()
	}

	def send(String remoteId, Message message) {
		if (sockets.containsKey(remoteId)) {
			send(sockets[remoteId].output as PrintWriter, message)
		}
	}

	def sendAll(String[] remoteIds, Message message) {
		remoteIds.each {
			send(sockets[it].output as PrintWriter, message)
		}
	}

	def sendAll(Message message) {
		sendAll(sockets.keySet() as String[], message)
	}

	private def handleSocket = { Socket socket ->

		log.info "$serverId: connected to ${socket.remoteSocketAddress}"

		BufferedReader input = socket.inputStream.newReader()
		PrintWriter output = socket.outputStream.newPrintWriter()

		Message serverInfo = new Message()

		log.finest "$serverId: sending server info to ${socket.remoteSocketAddress}"
		send(output, serverInfo)

		use(ReaderCategory) {

			log.finest "$serverId: awaiting response from ${socket.remoteSocketAddress}"

			String messageString = input.readUntil(delimiter)
			Message message = Message.fromJson(messageString)
			String remoteServerId = message.sender

			log.info "$serverId: received server info from ${socket.remoteSocketAddress}. Identifying as $remoteServerId"

			if (!sockets[remoteServerId]) {

				log.info "$serverId: connection confirmed to $remoteServerId"
				sockets[remoteServerId] = [socket: socket, input: input, output: output]

				while (true) {
					messageString = input.readUntil(delimiter)
					if (messageString) {
						handleMessage remoteServerId, Message.fromJson(messageString)
					}
				}
			} else {
				log.fine "${serverId}: Connection to $remoteServerId already exists. Closing ${socket.localSocketAddress}"
				input.close()
				output.close()
				socket.close()
			}
		}
	}

	private def handleMessage(String remoteServerId, Message message) {

		assert message != null
		log.fine "$serverId: received message from $remoteServerId> ${message.sender} : ${message.messageType}"

		MessageHandler handler = messageHandlers.find { it.typeRange.contains(message.messageType) }
		handler?.handleMessage(message)

		messages.setMessage(message)

		if (handler?.class != SystemMessageHandler) {
			log.fine "$serverId: rebroadcasting message to ${sockets.keySet().findAll { it != remoteServerId }}"
			Message msg = new Message()
			msg.messageId = message.messageId

			sendAll(sockets.keySet().findAll { it != remoteServerId } as String[], msg)
		}
	}

	public static void main(String[] args) {

		def argMap = [serverName: UUID.randomUUID().toString(), port: 40026]
		args.each {
			it.split('=').with {
				argMap[it[0]] = it[1]
			}
		}

		def neg1 = new MeshNode(argMap.serverName, argMap.port as int)
		neg1.addMessageHandler(new ChatMessageHandler())
		neg1.listen()

		Scanner scan = new Scanner(System.in)

		while(true) {
			String s = scan.nextLine()

			switch( s.substring(0, (s.indexOf(' ') + 1 ?: s.length()) - 1) ) {
				case 'q':
					System.exit(0)
				case 'connect':
					def host = s.substring(s.indexOf(' ') + 1).split(':')
					neg1.connect(host[0], host[1] as int)
					break;
				default:
					Message msg = new Message(messageType: ChatMessageHandler.TYPE_CHAT_MESSAGE)
					msg.data.contents = s
					neg1.sendAll(neg1.sockets.keySet() as String[], msg)
			}
		}
	}
}
