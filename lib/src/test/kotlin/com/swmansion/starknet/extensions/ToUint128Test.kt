package com.swmansion.starknet.extensions

import com.swmansion.starknet.data.types.Uint128
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigInteger

class ToUint128Test {
    @Test
    fun `test String toUint128`() {
        val result = "0xa".toUint128
        val expected = Uint128(BigInteger("a", 16))
        assertEquals(result, expected)
    }

    @Test
    fun `test Int toUint128`() {
        val result = 10.toUint128
        val expected = Uint128(BigInteger("a", 16))
        assertEquals(result, expected)
    }

    @Test
    fun `test Long toUint128`() {
        val result = 10L.toUint128
        val expected = Uint128(BigInteger("a", 16))
        assertEquals(result, expected)
    }
}
