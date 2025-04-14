package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.parseHex
import com.swmansion.starknet.data.types.conversions.ConvertibleToCalldata
import com.swmansion.starknet.extensions.toCalldata
import com.swmansion.starknet.extensions.toFelt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigInteger

internal class NumAsHexBaseTests {
    @Test
    fun `numbers can't be created with a negative value`() {
        assertThrows<java.lang.IllegalArgumentException> {
            Felt(BigInteger.valueOf(-1))
        }
        assertThrows<java.lang.IllegalArgumentException> {
            Uint64(BigInteger.valueOf(-1))
        }
        assertThrows<java.lang.IllegalArgumentException> {
            Uint128(BigInteger.valueOf(-1))
        }
        assertThrows<java.lang.IllegalArgumentException> {
            Uint128(BigInteger.valueOf(-1))
        }
        assertThrows<java.lang.IllegalArgumentException> {
            NumAsHex(BigInteger.valueOf(-1))
        }
        assertThrows<java.lang.IllegalArgumentException> {
            Uint256(BigInteger.valueOf(-1))
        }
    }

    @Test
    fun `numbers overflow`() {
        assertThrows<java.lang.IllegalArgumentException> {
            Felt(Felt.PRIME)
        }
        assertThrows<java.lang.IllegalArgumentException> {
            Uint64(Uint64.MAX + BigInteger.ONE)
        }
        assertThrows<java.lang.IllegalArgumentException> {
            Uint128(Uint128.MAX + BigInteger.ONE)
        }
        assertThrows<java.lang.IllegalArgumentException> {
            Uint256(Uint256.MAX + BigInteger.ONE)
        }
    }

    @Test
    fun `numbers to string`() {
        assertEquals(
            "Felt(0xabcdef01234567890)",
            Felt.fromHex("0xabcdef01234567890").toString(),
        )
        assertEquals(
            "NumAsHex(0xabcdef01234567890)",
            NumAsHex.fromHex("0xabcdef01234567890").toString(),
        )
        assertEquals(
            "Uint64(0x1234567890abcdef)",
            Uint64.fromHex("0x1234567890abcdef").toString(),
        )
        assertEquals(
            "Uint128(0xabcdef01234567890)",
            Uint128.fromHex("0xabcdef01234567890").toString(),
        )
        assertEquals(
            "Uint256(58462017464642449753835857636044240746640)",
            Uint256.fromHex("0xabcdef01234567890abcdef01234567890").toString(),
        )
    }

    @Test
    fun `numbers are comparable`() {
        val felt1 = Felt.fromHex("0x123")
        val felt2 = Felt.fromHex("0x456")
        val uint64 = Uint64.fromHex("0x123")
        val uint128 = Uint128.fromHex("0x456")
        val uint256 = Uint256.fromHex("0x123")
        val numAsHex = NumAsHex.fromHex("0x456")

        assertEquals(felt1.compareTo(uint64), 0)
        assertEquals(felt1.compareTo(uint256), 0)
        assertEquals(felt2.compareTo(uint128), 0)
        assertEquals(felt2.compareTo(numAsHex), 0)
        assertTrue(felt1 < felt2)
        assertTrue(felt1 < uint128)
        assertTrue(felt1 < numAsHex)
        assertTrue(uint256 <= felt1)
    }

    @Nested
    inner class FeltTests {
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

        @Test
        fun `from signed integer`() {
            assertEquals(
                Felt.MAX.toFelt,
                Felt.fromSigned(-1),
            )
            assertEquals(
                Felt.ONE,
                Felt.fromSigned(-Felt.MAX),
            )
            assertEquals(
                (Felt.PRIME - Int.MAX_VALUE.toBigInteger()).toFelt,
                Felt.fromSigned(-Int.MAX_VALUE),
            )
            assertEquals(
                (Felt.PRIME - Long.MAX_VALUE.toBigInteger()).toFelt,
                Felt.fromSigned(-Long.MAX_VALUE),
            )

            assertEquals(Felt.ZERO, Felt.fromSigned(0))
            assertEquals(Felt.ONE, Felt.fromSigned(1))

            assertThrows<IllegalArgumentException> {
                Felt.fromSigned(Felt.PRIME)
            }
            assertThrows<IllegalArgumentException> {
                Felt.fromSigned(-Felt.PRIME)
            }
        }

        @Test
        fun `felt array is convertible to calldata`() {
            val convertibleToCalldata = ArrayList<ConvertibleToCalldata>()

            val feltArray1 = FeltArray(Felt(100), Felt(200))
            val feltArray2 = FeltArray(listOf(Felt(300), Felt(400)))
            feltArray2.add(Felt(500))
            val emptyFeltArray = FeltArray()

            convertibleToCalldata.add(Felt(15))
            convertibleToCalldata.add(feltArray1)
            convertibleToCalldata.add(feltArray2)
            convertibleToCalldata.add(emptyFeltArray)

            val calldata = convertibleToCalldata.toCalldata()

            val expectedCalldata = listOf(
                Felt(15),
                Felt(2), Felt(100), Felt(200),
                Felt(3), Felt(300), Felt(400), Felt(500),
                Felt(0),
            )
            assertEquals(expectedCalldata, calldata)
        }

        @Test
        fun `hexString`() {
            val hexString1 = Felt.ZERO.hexString()
            val hexString2 = Felt.ONE.hexString()
            val hexString3 = Felt(100).hexString()
            assertEquals("0x0", hexString1)
            assertEquals("0x1", hexString2)
            assertEquals("0x64", hexString3)
        }

        @Test
        fun `hexStringPadded`() {
            val hexStringPadded1 = Felt.ZERO.hexStringPadded()
            val hexStringPadded2 = Felt.ONE.hexStringPadded()
            val hexStringPadded3 = Felt(100).hexStringPadded()
            assertEquals("0x0000000000000000000000000000000000000000000000000000000000000000", hexStringPadded1)
            assertEquals("0x0000000000000000000000000000000000000000000000000000000000000001", hexStringPadded2)
            assertEquals("0x0000000000000000000000000000000000000000000000000000000000000064", hexStringPadded3)
        }
    }

    @Nested
    inner class Uint256Tests {
        @Test
        fun `create from two felts`() {
            val felt1 = Felt.fromHex("0x123abc")
            val felt2 = Felt.fromHex("0x456")
            val uint256 = Uint256(BigInteger("377713427282241694444345814249262715910844"))

            assertEquals(uint256, Uint256(felt1, felt2))
            assertEquals(uint256, Uint256(felt1.value, felt2.value))
            assertEquals(felt1, uint256.low)
            assertEquals(felt2, uint256.high)
        }

        @Test
        fun `get low and high`() {
            val bigUint256 = Uint256(BigInteger("377713427282241694444345814249262715910844"))

            assertEquals(Felt.fromHex("0x123abc"), bigUint256.low)
            assertEquals(Felt.fromHex("0x456"), bigUint256.high)

            val smallUint256 = Uint256(1000)
            assertEquals(Felt.fromHex("0x3e8"), smallUint256.low)
            assertEquals(Felt.fromHex("0x0"), smallUint256.high)
        }
    }

    @Test
    fun `parseHex`() {
        val hexString1 = Felt.ZERO.hexString()
        val hexString2 = Felt.ONE.hexString()
        val hexString3 = Felt(100).hexString()

        assertEquals((0).toBigInteger(), parseHex(hexString1))
        assertEquals((1).toBigInteger(), parseHex(hexString2))
        assertEquals((100).toBigInteger(), parseHex(hexString3))
    }

    @Test
    fun `parseHex padded`() {
        val hexStringPadded1 = Felt.ZERO.hexStringPadded()
        val hexStringPadded2 = Felt.ONE.hexStringPadded()
        val hexStringPadded3 = Felt(100).hexStringPadded()

        assertEquals((0).toBigInteger(), parseHex(hexStringPadded1))
        assertEquals((1).toBigInteger(), parseHex(hexStringPadded2))
        assertEquals((100).toBigInteger(), parseHex(hexStringPadded3))
    }
}
