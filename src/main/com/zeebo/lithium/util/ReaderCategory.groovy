package com.zeebo.lithium.util

/**
 * User: Eric
 * Date: 9/8/14
 */
@Category(Reader)
class ReaderCategory {

	String readUntil(String sequence, int bufferSize = 1024) {
		StringBuilder builder = new StringBuilder()

		int seqMark = 0, numChars = 0
		char[] buffer = new char[bufferSize]

		boolean done = false

		while(!done) {
			mark(bufferSize)

			numChars = read(buffer)
			if (numChars > 0) {

				for (int i = 0; i < numChars; i++) {
					if (buffer[i] == sequence[seqMark]) {
						seqMark++
						if (seqMark == sequence.length()) {
							numChars = i + 1
							done = true
							break
						}
					}
					else {
						seqMark = 0
					}
				}

				builder.append(buffer, 0, numChars)
			}
			else if (numChars == -1) {
				break
			}
		}

		if (done) {
			reset()
			read(buffer, 0, numChars)

			builder.delete(builder.length() - sequence.length(), builder.length())
		}

		return builder.toString()
	}
}
