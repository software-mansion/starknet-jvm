package starknet.extensions

import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.extensions.decodeShortString
import com.swmansion.starknet.extensions.encodeShortString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ShortStringTest {

    @Test
    fun `encode short string`() {
        val encoded = "hello".encodeShortString()

        assertEquals(Felt.fromHex("0x68656c6c6f"), encoded)
    }

    @Test
    fun `encoding too long string should fail`() {
        assertThrows<Error>("Short string cannot be longer than 31 characters") {
            "a".repeat(32).encodeShortString()
        }
    }

    @Test
    fun `encoding non ascii string should fail`() {
        assertThrows<Error>("String to be encoded must be an ascii string") {
            "hello\uD83D\uDE00".encodeShortString()
        }
    }

    @Test
    fun `decode short string`() {
        val decoded = Felt.fromHex("0x68656c6c6f").decodeShortString()

        assertEquals("hello", decoded)
    }

    @Test
    fun `decode short string - start pad with 0`() {
        val decoded = Felt.fromHex("0xa68656c6c6f").decodeShortString()

        assertEquals("\nhello", decoded)
    }
}