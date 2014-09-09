package com.zeebo.lithium.mesh

import com.google.gson.Gson

/**
 * User: Eric
 * Date: 9/8/14
 */
class Message {

	static final Gson GSON = new Gson()

	static final int MESSAGE_SERVER_INFO = 1
	static final int MESSAGE_CONNECT = 2
	static final int MESSAGE_DISCONNECT = 3
	static final int MESSAGE_REQUEST_CONNECTIONS = 4
	static final int MESSAGE_SERVER_LIST = 5

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
