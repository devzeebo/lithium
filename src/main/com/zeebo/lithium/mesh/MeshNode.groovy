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

	String serverId = UUID.randomUUID().toString()

	ServerSocket serverSocket
	def Message template

	Gson gson = new Gson()

	def sockets = [:] as ConcurrentHashMap

	private def messageHandlers = [:].withDefault { int k ->
		messageHandlers[k] = []
	}

	MeshNode() {
		this(40026)
	}

	MeshNode(int port) {
		serverSocket = new ServerSocket(port)
		template = new Message(serverId: serverId, serverPort: port)

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
		Socket socket = new Socket(hostname, port)
		log.info "${serverId}($socket.localPort) attempting to connect to /$hostname:$port"

		Thread.start {
			handleSocket socket
		}
	}

	private static def send(PrintWriter output, Message message) {
		output.print(message.toString() + DELIMETER)
		output.flush()
	}

	private def handleSocket = { Socket socket ->

		log.info "$serverId connected to ${socket.remoteSocketAddress}"

		BufferedReader input = socket.inputStream.newReader()
		PrintWriter output = socket.outputStream.newPrintWriter()

		Message serverInfo = template.clone()
		serverInfo.messageType = Message.MESSAGE_SERVER_INFO

		send(output, serverInfo)

		use(ReaderCategory) {
			Message message = Message.fromJson(input.readUntil(DELIMETER))

			String remoteServerId = message.serverId

			log.fine "${serverId} received server info from ${remoteServerId}"
			sockets[remoteServerId] = [socket: socket, input: input, output: output, listenPort: message.serverPort]

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

		log.fine "Received message from $serverId of type ${msg.messageType}"

		if (msg.messageType <= 128 && msg.messageType != 0) {
			this."${systemMessageTypeHandlers[msg.messageType]}"(msg)
		} else {
			messageHandlers[msg.messageType].each { it(msg) }
		}
	}

	private def connectHandler = { Message msg ->

		def contents = gson.fromJson(msg.message, Map)

		if (!sockets[contents.serverId]) {
			connect(contents.hostName, contents.port as int)
		}
	}

	private def queryForwardHandler = { Message msg ->
		// check if we've received the message
	}

	private def ackForwardHandler = { Message msg ->
		// forward the message
	}

	private def ackIgnoreHandler = { Message msg ->
		// ignore the message
	}

	public static void main(String[] args) {
		def neg1 = new MeshNode()
		neg1.listen()

		def neg2 = new MeshNode(40027)
		neg2.connect('127.0.0.1', 40026)

		def neg3 = new MeshNode(40028)
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
