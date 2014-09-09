package com.zeebo.lithium.util

class ReaderCategoryTest extends GroovyTestCase {

	def String fox = 'The quick brown fox jumped over the lazy dog.'
	def Reader reader = new BufferedReader(new StringReader(fox))

	void testReadUntil() {
		use(ReaderCategory) {
			assert reader.readUntil('.', 2) == fox[0..<fox.length() - 1]
		}
	}

	void testReadUntilWithOversizedBuffer() {
		use(ReaderCategory) {
			assert reader.readUntil(' ', 5) == 'The'
			assert reader.readUntil(' ', 5) == 'quick'
		}
	}

	void testReadUntilWithExactBuffer() {
		use(ReaderCategory) {
			assert reader.readUntil(' ', 1) == 'The'
			assert reader.readUntil(' ', 1) == 'quick'
		}
	}

	void testReadUntilWithEmptyMatch() {
		use(ReaderCategory) {
			assert reader.readUntil('T', 5) == ''
			assert reader.readUntil('T', 5) == fox.substring(1)
		}
	}

	void testReadUntilWithNoMatch() {
		use(ReaderCategory) {
			assert reader.readUntil('\0', 5) == fox
		}
	}

	void testReadUntilWithLongSequence() {
		use(ReaderCategory) {
			assert reader.readUntil('fox', 1) == 'The quick brown '
			assert reader.readUntil('fox', 1) == ' jumped over the lazy dog.'
		}
	}

	void testReadUntilWithLongSequenceAndExactBuffer() {
		use(ReaderCategory) {
			assert reader.readUntil('fox', 3) == 'The quick brown '
			assert reader.readUntil('fox', 3) == ' jumped over the lazy dog.'
		}
	}

	void testReadUntilWithLongSequenceAndOversizedBuffer() {
		use(ReaderCategory) {
			assert reader.readUntil('fox', 19) == 'The quick brown '
			assert reader.readUntil('fox', 19) == ' jumped over the lazy dog.'
		}
	}
}
