package com.swmansion.starknet.data

import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.Uint256
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigInteger

internal class Uint256Test {
    private val bigIntegerValue = BigInteger("115792089237316195423570985008687907853269984665640564039457584007913129639935")

    @Test
    fun `construct uint256 from BigInteger`() {
        val uint256 = Uint256(bigIntegerValue)

        assertEquals(uint256.value, bigIntegerValue)
    }

    @Test
    fun `construct uint256 from high and low felt`() {
        val uint256 = Uint256(bigIntegerValue)
        val low = uint256.low
        val high = uint256.high

        assertEquals(uint256, Uint256(low, high))
    }

    @Test
    fun `construct uint256 from high and low`() {
        val uint256 = Uint256(bigIntegerValue)
        val low = BigInteger(uint256.low.decString())
        val high = BigInteger(uint256.high.decString())

        assertEquals(uint256, Uint256(low, high))
    }

    @Test
    fun `construct uint256 from int`() {
        val uint256 = Uint256(23)

        assertEquals(uint256.low.decString(), "23")
        assertEquals(uint256.high.decString(), "0")
    }

    @Test
    fun `construct uint256 from long`() {
        val uint256 = Uint256(9223372036854775807L)

        assertEquals(uint256.low.decString(), "9223372036854775807")
        assertEquals(uint256.high.decString(), "0")
    }

    @Test
    fun `construct uint256 from felt`() {
        val uint256 = Uint256(Felt(4215215))

        assertEquals(uint256.low.decString(), "4215215")
        assertEquals(uint256.high.decString(), "0")
    }

    @Test
    fun `uint256 from hex string`() {
        val low = Felt.fromHex("0x92369239639725932969795495939596")
        val high = Felt.fromHex("0x2104395")
        val uint256 = Uint256.fromHex("0x210439592369239639725932969795495939596")

        assertEquals(uint256, Uint256(low, high))
    }

    @Test
    fun `construct uint256 below 0`() {
        assertThrows(IllegalArgumentException::class.java) {
            Uint256(BigInteger("-1"))
        }
    }

    @Test
    fun `construct uint256 above max`() {
        assertThrows(IllegalArgumentException::class.java) {
            Uint256(BigInteger.valueOf(2).pow(256))
        }
    }

    @Test
    fun `convert uint256 to felt list`() {
        val uint256 = Uint256.fromHex("0x210439592369239639725932969795495939596")
        val low = Felt.fromHex("0x92369239639725932969795495939596")
        val high = Felt.fromHex("0x2104395")

        val feltList = uint256.toCalldata()
        assertEquals(listOf(low, high), feltList)
    }
}
