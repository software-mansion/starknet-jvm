package com.swmansion.starknet.data.types

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigInteger

internal class FeltTest {
    @Test
    fun `Felt can't be created with a negative value`() {
        assertThrows<java.lang.IllegalArgumentException>("Default Felt constructor does not accept negative numbers, [-1] given.") {
            Felt(BigInteger.valueOf(-1))
        }
    }

    @Test
    fun `Felt can't be created with a value bigger than PRIME`() {
        assertThrows<java.lang.IllegalArgumentException>(
            "Default Felt constructor accepts values smaller than Felt.PRIME, [3618502788666131213697322783095070105623107215331596699973092056135872020481] given.",
        ) {
            Felt(Felt.PRIME)
        }
    }

    @Test
    fun feltToString() {
        assertEquals(
            "Felt(0xabcdef01234567890)",
            Felt.fromHex("0xabcdef01234567890").toString(),
        )
    }

    @Test
    fun `fromShortString short string`() {
        val encoded = Felt.fromShortString("hello")

        assertEquals(Felt.fromHex("0x68656c6c6f"), encoded)
    }

    @Test
    fun `fromShortString too long string should fail`() {
        assertThrows<java.lang.IllegalArgumentException>("Short string cannot be longer than 31 characters") {
            Felt.fromShortString("a".repeat(32))
        }
    }

    @Test
    fun `fromShortString non ascii string should fail`() {
        assertThrows<java.lang.IllegalArgumentException>("String to be encoded must be an ascii string") {
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
