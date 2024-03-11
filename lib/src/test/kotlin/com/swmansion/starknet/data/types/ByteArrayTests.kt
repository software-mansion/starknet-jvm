package com.swmansion.starknet.data.types

import com.swmansion.starknet.extensions.toFelt
import org.junit.jupiter.api.Assertions.assertEquals
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
        private fun getByteArrayFromString(): List<ByteArrayTestCase> {
            return listOf(
                ByteArrayTestCase(
                    input = "hello",
                    data = listOf(Felt.ZERO),
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
                    data = listOf(Felt.ZERO),
                    pendingWord = Felt.fromHex("0x4142434445464748494a4b4c4d4e4f505152535455565758595a31323334"),
                    pendingWordLen = 30,
                ),
                ByteArrayTestCase(
                    input = "",
                    data = listOf(Felt.ZERO),
                    pendingWord = Felt.ZERO,
                    pendingWordLen = 0,
                ),
            )
        }
    }

    @ParameterizedTest
    @MethodSource("getByteArrayFromString")
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
}
