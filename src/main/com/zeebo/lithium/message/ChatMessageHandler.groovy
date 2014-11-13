package com.zeebo.lithium.message

import com.zeebo.lithium.mesh.Message

/**
 * User: eric
 * Date: 11/12/14
 */
class ChatMessageHandler extends MessageHandler {

	public static final int TYPE_CHAT_MESSAGE = 1000

	@Override
	IntRange getTypeRange() { (1000..1001) }

	def handleChatMessage(Message message) {
		println message.data.contents
	}
}
