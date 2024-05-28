package com.swmansion.starknet.extensions

import com.swmansion.starknet.data.types.Uint128
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigInteger

class ToUint128Test {
    @Test
    fun `test String toUint128`() {
        val uint128Number = "0xa".toUint128
        val expected = Uint128(BigInteger("a", 16))
        assertEquals(uint128Number, expected)
    }

    @Test
    fun `test invalid String toUint128`() {
        assertThrows<IllegalArgumentException> { "Example".toUint128 }
    }

    @Test
    fun `test Int toUint128`() {
        val uint128Number = 10.toUint128
        val expected = Uint128(BigInteger("a", 16))
        assertEquals(uint128Number, expected)
    }

    @Test
    fun `test negative Int toUint128`() {
        assertThrows<IllegalArgumentException> { (-10).toUint128 }
    }

    @Test
    fun `test Long toUint128`() {
        val uint128Number = 10L.toUint128
        val expected = Uint128(BigInteger("a", 16))
        assertEquals(uint128Number, expected)
    }

    @Test
    fun `test negative Long toUint128`() {
        assertThrows<IllegalArgumentException> { (-10L).toUint128 }
    }
}
