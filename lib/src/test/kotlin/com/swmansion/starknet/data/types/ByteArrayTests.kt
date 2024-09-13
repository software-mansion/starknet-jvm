package com.swmansion.starknet.data.types

import com.swmansion.starknet.extensions.toFelt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

internal class ByteArrayTests {
    data class ByteArrayTestCase(
        val input: String,
        val data: List<Felt>,
        val pendingWord: Felt,
        val pendingWordLen: Int,
    )

    companion object {
        @JvmStatic
        private fun getByteArrayTestCases(): List<ByteArrayTestCase> {
            return listOf(
                ByteArrayTestCase(
                    input = "hello",
                    data = emptyList(),
                    pendingWord = Felt.fromHex("0x68656c6c6f"),
                    pendingWordLen = 5,
                ),
                ByteArrayTestCase(
                    input = "Long string, more than 31 characters.",
                    data = listOf(Felt.fromHex("0x4c6f6e6720737472696e672c206d6f7265207468616e203331206368617261")),
                    pendingWord = Felt.fromHex("0x63746572732e"),
                    pendingWordLen = 6,
                ),
                ByteArrayTestCase(
                    input = "ABCDEFGHIJKLMNOPQRSTUVWXYZ12345AAADEFGHIJKLMNOPQRSTUVWXYZ12345A",
                    data = listOf(
                        Felt.fromHex("0x4142434445464748494a4b4c4d4e4f505152535455565758595a3132333435"),
                        Felt.fromHex("0x4141414445464748494a4b4c4d4e4f505152535455565758595a3132333435"),
                    ),
                    pendingWord = Felt.fromHex("0x41"),
                    pendingWordLen = 1,
                ),
                ByteArrayTestCase(
                    input = "ABCDEFGHIJKLMNOPQRSTUVWXYZ12345",
                    data = listOf(Felt.fromHex("0x4142434445464748494a4b4c4d4e4f505152535455565758595a3132333435")),
                    pendingWord = Felt.ZERO,
                    pendingWordLen = 0,
                ),
                ByteArrayTestCase(
                    input = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234",
                    data = listOf(),
                    pendingWord = Felt.fromHex("0x4142434445464748494a4b4c4d4e4f505152535455565758595a31323334"),
                    pendingWordLen = 30,
                ),
                ByteArrayTestCase(
                    input = "",
                    data = emptyList(),
                    pendingWord = Felt.ZERO,
                    pendingWordLen = 0,
                ),
                ByteArrayTestCase(
                    input = "\u0000",
                    data = emptyList(),
                    pendingWord = Felt.ZERO,
                    pendingWordLen = 1,
                ),
                ByteArrayTestCase(
                    input = "This is my string: \u0000",
                    data = emptyList(),
                    pendingWord = Felt.fromHex("0x54686973206973206d7920737472696e673a2000"),
                    pendingWordLen = 20,
                ),
                ByteArrayTestCase(
                    input = "This is my string, I like it!: \u0000",
                    data = listOf(Felt.fromHex("0x54686973206973206d7920737472696e672c2049206c696b65206974213a20")),
                    pendingWord = Felt.ZERO,
                    pendingWordLen = 1,
                ),
            )
        }
    }

    @ParameterizedTest
    @MethodSource("getByteArrayTestCases")
    fun `byte array from string`(testCase: ByteArrayTestCase) {
        val byteArray = StarknetByteArray.fromString(testCase.input)

        assertEquals(testCase.data, byteArray.data)
        assertEquals(testCase.pendingWord, byteArray.pendingWord)
        assertEquals(testCase.pendingWordLen, byteArray.pendingWordLen)

        assertEquals(
            listOf(testCase.data.size.toFelt) + testCase.data + listOf(testCase.pendingWord, testCase.pendingWordLen.toFelt),
            byteArray.toCalldata(),
        )
    }

    @ParameterizedTest
    @MethodSource("getByteArrayTestCases")
    fun `byte array to string`(testCase: ByteArrayTestCase) {
        val byteArray = StarknetByteArray(
            data = testCase.data,
            pendingWord = testCase.pendingWord,
            pendingWordLen = testCase.pendingWordLen,
        )

        assertEquals(testCase.input, byteArray.toString())
    }

    @Nested
    inner class InvalidByteArrayTests {
        @Test
        fun `byte array from string with invalid pending word length`() {
            val exception = assertThrows<IllegalArgumentException> {
                StarknetByteArray(
                    data = emptyList(),
                    pendingWord = Felt.fromHex("0x68656c6c6f"),
                    pendingWordLen = 31,
                )
            }

            assertEquals("The length of 'pendingWord' must be between 0 and 30. [31] given.", exception.message)
        }

        @Test
        fun `byte array from string with invalid pending word`() {
            val exception = assertThrows<IllegalArgumentException> {
                StarknetByteArray(
                    data = emptyList(),
                    pendingWord = Felt.fromHex("0x68656c6c6f"),
                    pendingWordLen = 4,
                )
            }

            assertEquals("The length of 'pendingWord' must be equal to 'pendingWordLen'. [0x68656c6c6f] of length [5] given.", exception.message)
        }

        @Test
        fun `byte array from string with invalid data`() {
            val exception = assertThrows<IllegalArgumentException> {
                StarknetByteArray(
                    data = listOf(
                        Felt.fromHex("0x4142434445464748494a4b4c4d4e4f505152535455565758595a3132333435"),
                        Felt.fromHex("0x68656c6c6f"),
                    ),
                    pendingWord = Felt.ZERO,
                    pendingWordLen = 0,
                )
            }

            assertEquals("All elements of 'data' must be 31 bytes long. [0x68656c6c6f] of length [5] given.", exception.message)
        }
    }
}
