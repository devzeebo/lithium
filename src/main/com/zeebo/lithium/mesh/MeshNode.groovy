package com.zeebo.lithium.mesh

import com.google.gson.Gson
import com.zeebo.lithium.util.ReaderCategory
import com.zeebo.lithium.util.SocketCategory

import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap

/**
 * User: Eric
 * Date: 9/8/14
 */
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

	ServerSocket serverSocket
	def Message serverInfo

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

		serverInfo = new Message(messageType: Message.MESSAGE_SERVER_INFO, message: gson.toJson(
				[port: port]
		))
	}

	def listen() {
		Thread.start {
			while (true) {

				def socketList = sockets.keySet().collect { it }

				serverSocket.accept(true, handleSocket)
			}
		}
	}

	def connect(String hostname, int port) {
		Socket socket = new Socket(hostname, port)
		println "${socket.localSocketAddress}(${serverSocket.localSocketAddress}) attempting to connect to /$hostname:$port"

		Thread.start {
			handleSocket socket
		}

		waitForSocket(socket) { address ->
			println "${serverSocket.localSocketAddress} sending server info to $address"
			Message socketInfo = serverInfo.clone()
			send(address, socketInfo)
		}
	}

	def send(Socket socket, Message message) {

		waitForSocket(socket) { address ->
			def output = sockets[address].output
			output.print(message.toString() + DELIMETER)
			output.flush()
		}
	}
	def send(String address, Message message) {
		message.socketInfo = sockets[address].socket.localSocketAddress.toString().replace('/','')
		send((Socket)sockets[address].socket, message)
	}

	def sendAll(Message message) {

		sockets.each { k, v ->
			send(v.socket, message)
		}
	}

	private def handleSocket = { Socket socket ->

		use(SocketCategory) {
			String socketAddress = socket.address
			println "${socket.localSocketAddress} connected to ${socketAddress}"

			BufferedReader input = socket.inputStream.newReader()
			PrintWriter output = socket.outputStream.newPrintWriter()
			sockets[socketAddress] = [socket: socket, input: input, output: output]

			use(ReaderCategory) {
				while (true) {
					handleMessage(Message.fromJson(input.readUntil(DELIMETER)))
				}
			}
		}
	}

	private def waitForSocket(String address, Closure closure) {
		while (!sockets[address]);
		if (sockets[address] != 'error') {
			closure(address)
		}
	}

	private def waitForSocket(Socket socket, Closure closure) {
		use(SocketCategory) {
			waitForSocket(socket.address, closure)
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

	private def handleMessage(Message msg) {

		println msg

		if (msg.messageType <= 128 && msg.messageType != 0) {
			this."${systemMessageTypeHandlers[msg.messageType]}"(msg)
		} else {
			messageHandlers[msg.messageType].each { it(msg) }
		}
	}

	private def connectHandler = { Message msg ->
		String hostName = msg.message[0..<msg.message.indexOf(':')]
		int port = msg.message.substring(msg.message.indexOf(':') + 1) as int

		connect(hostName, port)
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

	private def serverInfoHandler = { Message msg ->
		def serverInfo = gson.fromJson(msg.message, Map)
		int listenPort = serverInfo.port as int
		println "${serverSocket.localSocketAddress} received server info from ${msg.socketInfo}"
		sockets[msg.socketInfo].listenPort = listenPort

		sockets.each { String k, Map v ->
			Message connectMessage = new Message(messageType: Message.MESSAGE_CONNECT, message: "${k[0..<k.indexOf(':')]}:$listenPort")
			if (k != msg.socketInfo) {
				send(k, connectMessage)
			}
		}
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

		neg1.sendAll(new Message(message: 'Hello World'))
	}
}
