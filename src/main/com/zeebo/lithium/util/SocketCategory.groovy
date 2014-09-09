package com.zeebo.lithium.util

/**
 * User: Eric
 * Date: 9/8/14
 */
@Category(Socket)
class SocketCategory {

	String getAddress() {
		return "${inetAddress.hostAddress}:${port}"
	}
}
