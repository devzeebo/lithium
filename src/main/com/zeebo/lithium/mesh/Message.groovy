package com.zeebo.lithium.mesh

import com.google.gson.Gson

/**
 * User: Eric
 * Date: 9/8/14
 */
class Message {

	static final Gson GSON = new Gson()

	static final int MESSAGE_CONNECT = 1
	static final int MESSAGE_DISCONNECT = 2
	static final int MESSAGE_QUERY_FORWARD = 3
	static final int MESSAGE_ACK_FORWARD = 4
	static final int MESSAGE_ACK_IGNORE = 5
	static final int MESSAGE_SERVER_INFO = 6

	int messageType

	String serverId
	int serverPort

	String message

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
