package com.zeebo.lithium.message

import com.zeebo.lithium.mesh.MeshNode
import com.zeebo.lithium.mesh.Message

import java.lang.reflect.Modifier

/**
 * User: eric
 * Date: 11/12/14
 */
abstract class MessageHandler {

	private static def getHandlerMethod(String constantName) {
		StringBuilder methodName = new StringBuilder('handle')
		def matcher = constantName =~ /([^_]+)_?/
		while(matcher.find()) {
			methodName.append(matcher.group(1).toLowerCase().capitalize())
		}
		return methodName.toString()
	}

	def messageTypeToHandler
	MeshNode node

	MessageHandler() {
		messageTypeToHandler = [:]
		this.class.declaredFields.findAll {
			Modifier.isFinal(it.modifiers) && Modifier.isStatic(it.modifiers) && it.type == int
		}.each {
			messageTypeToHandler[it.get(this)] = MessageHandler.getHandlerMethod(it.name.substring(5))
		}
	}

	abstract IntRange getTypeRange()

	final void handleMessage(Message message) {
		if (typeRange.contains(message.messageType)) {
			this."${messageTypeToHandler[message.messageType]}"(message)
		}
	}
}
