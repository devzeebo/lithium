package com.zeebo.lithium.message

import com.zeebo.lithium.mesh.MeshNode
import com.zeebo.lithium.mesh.Message

import java.lang.reflect.Modifier

/**
 * User: eric
 * Date: 11/12/14
 */
class SystemMessageHandler extends MessageHandler {

	public static final int TYPE_MESSAGE_NOTIFICATION = 0
	public static final int TYPE_CONNECT = 1
	public static final int TYPE_GET_MESSAGE = 2

	IntRange getTypeRange() { (0..128) }

	def handleConnect(Message message) {
		node.log.fine ("Connecting to ${message.data.host}:${message.data.port}.")

		node.connect(message.data.host, message.data.port)
	}

	def handleMessageNotification(Message message) {
		if (node.messages.addMessage(message)) {
			Message msg = new Message(messageType: TYPE_GET_MESSAGE)
			msg.data.messageId = message.messageId
			node.send(message.sender, msg)
		}
	}

	def handleGetMessage(Message message) {
		Message msg = node.messages[message.data.messageId].@delegate
		node.send(message.sender, msg)
	}
}
