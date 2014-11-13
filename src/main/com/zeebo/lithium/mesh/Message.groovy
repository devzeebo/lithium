package com.zeebo.lithium.mesh

import com.google.gson.Gson

/**
 * User: Eric
 * Date: 9/8/14
 */
class Message {

	static final Gson GSON = new Gson()

	String messageId = UUID.randomUUID().toString()

	int messageType

	String sender
	List<String> recipients

	Map data = [:]

	Message clone() {
		return GSON.fromJson(GSON.toJson(this), Message)
	}

	static Message fromJson(String message) {
		return GSON.fromJson(message, Message)
	}

	String toString() {
		return GSON.toJson(this)
	}
}
