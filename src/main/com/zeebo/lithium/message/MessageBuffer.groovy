package com.zeebo.lithium.message

import com.zeebo.lithium.mesh.Message
import com.zeebo.lithium.mortalis.Abaddon

import java.util.concurrent.ConcurrentHashMap

/**
 * User: Eric Siebeneich
 * Date: 9/12/14
 */
class MessageBuffer {

	// set the lifespan to 30 seconds
	static final def lifespan = 1000 * 30

	def messages = [:] as ConcurrentHashMap<String, Message>

	MessageBuffer() {
		Abaddon.registerMap(messages)
	}

	def getAt(String messageId) {
		return messages[messageId]
	}

	def addMessage(Message message) {
		if (messages.containsKey(message.messageId)) {
			return false
		}

		messages[message.messageId] = Abaddon.registerObject(message, lifespan)
		return true
	}

	def setMessage(Message message) {
		messages[message.messageId] = Abaddon.registerObject(message, lifespan)
	}
}
