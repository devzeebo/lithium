package com.zeebo.lithium.mesh

import com.google.gson.Gson
import com.zeebo.lithium.util.ReaderCategory

import groovy.util.logging.Log

import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap

/**
 * User: Eric
 * Date: 9/8/14
 */
@Log
class MeshNode {

	static String DELIMETER = '\0'

	static final def systemMessageTypeHandlers = [:]
	static {
		Message.declaredFields.findAll { Modifier.isStatic(it.modifiers) && Modifier.isFinal(it.modifiers) && it.name.startsWith('MESSAGE_') }.each {
			String name = it.name.replace('MESSAGE_', '').toLowerCase()
			def matcher = name =~ /\_(\w)/
			while (matcher.find()) {
				name = name.replace(matcher.group(), matcher.group(1).toUpperCase())
			}

			systemMessageTypeHandlers[Message."${it.name}"] = name + 'Handler'
		}
	}

	String serverId = UUID.randomUUID().toString().hashCode()

	ServerSocket serverSocket
	int port

	Gson gson = new Gson()

	def sockets = [:] as ConcurrentHashMap

	private def messageHandlers = [:].withDefault { int k ->
		messageHandlers[k] = []
	}

	MeshNode() {
		this(40026)
	}

	MeshNode(int port) {
		this.port = port
		serverSocket = new ServerSocket(port)

		log.info "$serverId started on port $port"
	}

	def listen() {
		Thread.start {
			while (true) {
				serverSocket.accept(true, handleSocket)
			}
		}
	}

	def connect(String hostname, int port) {
		log.info "${serverId} attempting to connect to /$hostname:$port"
		Socket socket = new Socket(hostname, port)
		log.fine "${serverId} opened port ${socket.localPort}"

		Thread.start {
			handleSocket socket, { serverId ->
				Message requestConnections = new Message(messageType: Message.MESSAGE_REQUEST_CONNECTIONS)
				send(sockets[serverId].output, requestConnections)
			}
		}
	}

	private def send(PrintWriter output, Message message) {

		message.serverId = serverId
		message.serverPort = port

		output.print(message.toString() + DELIMETER)
		output.flush()
	}

	private def handleSocket = { Socket socket, Closure callback = null ->

		log.info "$serverId connected to ${socket.remoteSocketAddress}"

		BufferedReader input = socket.inputStream.newReader()
		PrintWriter output = socket.outputStream.newPrintWriter()

		Message serverInfo = new Message(messageType: Message.MESSAGE_SERVER_INFO)

		log.finest "${serverId} sending server info to ${socket.remoteSocketAddress}"
		send(output, serverInfo)

		use(ReaderCategory) {
			log.finest "${serverId} awaiting response from ${socket.remoteSocketAddress}"
			String messageString = input.readUntil(DELIMETER)
			log.finest "${serverId} received response ${messageString}"
			Message message = Message.fromJson(messageString)

			String remoteServerId = message.serverId

			log.fine "${serverId} received server info from ${remoteServerId}"
			log.finest "${serverId} contains entry for ${remoteServerId}? ${sockets[remoteServerId] != null}"
			sockets[remoteServerId] = [socket: socket, input: input, output: output, listenPort: message.serverPort]

			callback?.call(remoteServerId)

			while(true) {
				handleMessage remoteServerId, Message.fromJson(input.readUntil(DELIMETER))
			}
		}
	}

	def addMessageHandler(Closure handler) {
		messageHandlers[0] << handler
	}

	def addMessageHandler(int messageType, Closure handler) {
		if (messageType < 128 && messageType != 0) {
			throw new IllegalArgumentException('User message types must be greater than 128')
		}

		messageHandlers[messageType] << handler
	}

	private def handleMessage(String serverId, Message msg) {

		log.fine "${this.serverId} received message from $serverId of type ${msg.messageType}"

		if (msg.messageType <= 128 && msg.messageType != 0) {
			this."${systemMessageTypeHandlers[msg.messageType]}"(msg)
		} else {
			messageHandlers[msg.messageType].each { it(msg) }
		}
	}

	private def connectHandler = { Message msg ->

		def contents = gson.fromJson(msg.message, Map)

		if (serverId != contents.serverId && !sockets[contents.serverId]) {
			connect(contents.hostName, contents.port as int)
		}
	}

	private def requestConnectionsHandler = { Message msg ->

		Message connectMessage = new Message(messageType: Message.MESSAGE_CONNECT)

		sockets.each { serverId, map ->
			connectMessage.message = gson.toJson([
					serverId: serverId,
					hostName: map.socket.remoteSocketAddress.address,
					port: map.listenPort
			])
			send(map.output, connectMessage)
		}
	}

	public static void main(String[] args) {
		def neg1 = new MeshNode()
		neg1.listen()

		def neg2 = new MeshNode(40027)
		neg2.listen()
		neg2.connect('127.0.0.1', 40026)

		def neg3 = new MeshNode(40028)
		neg3.listen()
		neg3.connect('127.0.0.1', 40026)

		sleep(200)

		neg2.addMessageHandler { msg ->
			println msg
		}

		neg3.addMessageHandler { msg ->
			println msg
		}
	}
}
