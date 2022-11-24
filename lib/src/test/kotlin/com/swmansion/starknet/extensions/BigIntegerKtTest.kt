package com.swmansion.starknet.extensions

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigInteger

internal class BigIntegerKtTest {
    @Test
    fun `toBytes should fail for negative number`() {
        assertThrows<IllegalArgumentException>("Creating ByteArray from negative numbers is not supported.") {
            BigInteger.valueOf(-1).toBytes()
        }
    }

    @Test
    fun `toBytes works on 0`() {
        val result = BigInteger.valueOf(0).toBytes()
        assertArrayEquals(ByteArray(1) { 0.toByte() }, result)
    }

    @Test
    fun `toBytes doesn't cut first byte when it is not required`() {
        val value = BigInteger.ONE
        assertArrayEquals(value.toByteArray(), value.toBytes())
    }

    @Test
    fun `toBytes removes first byte when bit length = 8n`() {
        // bit length is 16 here, toByteArray would return [0, 0xff]
        // as it adds one bit for the sign
        val value = BigInteger("ff", 16)
        assertArrayEquals(ByteArray(1) { 255.toByte() }, value.toBytes())
    }
}
