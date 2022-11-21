package com.swmansion.starknet.data.types

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class FeltTest {
    @Test
    fun `fromShortString short string`() {
        val encoded = Felt.fromShortString("hello")

        assertEquals(Felt.fromHex("0x68656c6c6f"), encoded)
    }

    @Test
    fun `fromShortString too long string should fail`() {
        assertThrows<Error>("Short string cannot be longer than 31 characters") {
            Felt.fromShortString("a".repeat(32))
        }
    }

    @Test
    fun `fromShortString non ascii string should fail`() {
        assertThrows<Error>("String to be encoded must be an ascii string") {
            Felt.fromShortString("hello\uD83D\uDE00")
        }
    }

    @Test
    fun `toShortString short string`() {
        val decoded = Felt.fromHex("0x68656c6c6f").toShortString()

        assertEquals("hello", decoded)
    }

    @Test
    fun `toShortString short string - start pad with 0`() {
        val decoded = Felt.fromHex("0xa68656c6c6f").toShortString()

        assertEquals("\nhello", decoded)
    }
}
