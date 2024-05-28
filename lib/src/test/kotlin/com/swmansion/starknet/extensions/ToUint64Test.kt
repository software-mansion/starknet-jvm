package com.swmansion.starknet.extensions

import com.swmansion.starknet.data.types.Uint64
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigInteger

class ToUint64Test {
    @Test
    fun `test BigInteger toUint64`() {
        val uint64Number = BigInteger("a", 16).toUint64
        val expected = Uint64(BigInteger("a", 16))
        assertEquals(uint64Number, expected)
    }

    @Test
    fun `test String toUint64`() {
        val uint64Number = "0xa".toUint64
        val expected = Uint64(BigInteger("a", 16))
        assertEquals(uint64Number, expected)
    }

    @Test
    fun `test invalid String toUint64`() {
        assertThrows<IllegalArgumentException> { "Example".toUint64 }
    }

    @Test
    fun `test Int toUint64`() {
        val uint64Number = 10.toUint64
        val expected = Uint64(BigInteger("a", 16))
        assertEquals(uint64Number, expected)
    }

    @Test
    fun `test negative Int toUint64`() {
        assertThrows<IllegalArgumentException> { (-10).toUint64 }
    }

    @Test
    fun `test Long toUint64`() {
        val uint64Number = 10L.toUint64
        val expected = Uint64(BigInteger("a", 16))
        assertEquals(uint64Number, expected)
    }

    @Test
    fun `test negative Long toUint64`() {
        assertThrows<IllegalArgumentException> { (-10L).toUint64 }
    }
}
