package com.swmansion.starknet.extensions

import com.swmansion.starknet.data.types.Uint256
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigInteger

class ToUint256Test {
    @Test
    fun `test BigInteger toUint256`() {
        val uint256Number = BigInteger("a", 16).toUint256
        val expected = Uint256(BigInteger("a", 16))
        assertEquals(uint256Number, expected)
    }

    @Test
    fun `test String toUint256`() {
        val uint256Number = "0xa".toUint256
        val expected = Uint256(BigInteger("a", 16))
        assertEquals(uint256Number, expected)
    }

    @Test
    fun `test invalid String toUint256`() {
        assertThrows<IllegalArgumentException> { "Example".toUint256 }
    }

    @Test
    fun `test Int toUint256`() {
        val uint256Number = 10.toUint256
        val expected = Uint256(BigInteger("a", 16))
        assertEquals(uint256Number, expected)
    }

    @Test
    fun `test negative Int toUint256`() {
        assertThrows<IllegalArgumentException> { (-10).toUint256 }
    }

    @Test
    fun `test Long toUint256`() {
        val uint256Number = 10L.toUint256
        val expected = Uint256(BigInteger("a", 16))
        assertEquals(uint256Number, expected)
    }

    @Test
    fun `test negative Long toUint256`() {
        assertThrows<IllegalArgumentException> { (-10L).toUint256 }
    }
}
