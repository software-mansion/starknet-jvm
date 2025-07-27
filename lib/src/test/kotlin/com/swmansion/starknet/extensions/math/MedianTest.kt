package com.swmansion.starknet.extensions.math
import com.swmansion.starknet.data.types.Uint64
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigInteger

internal class MedianTest {
    @Test
    fun `median of odd-sized list`() {
        val list = listOf(
            Uint64(BigInteger.valueOf(3)),
            Uint64(BigInteger.valueOf(1)),
            Uint64(BigInteger.valueOf(2)),
        )
        // sorted: 1,2,3 => median = 2
        assertEquals(
            Uint64(BigInteger.valueOf(2)),
            list.median(),
        )
    }

    @Test
    fun `median of even-sized list`() {
        val list = listOf(
            Uint64(BigInteger.valueOf(5)),
            Uint64(BigInteger.valueOf(1)),
            Uint64(BigInteger.valueOf(4)),
            Uint64(BigInteger.valueOf(2)),
        )
        // sorted: 1,2,4,5; (2 + 4) / 2 = 3
        assertEquals(
            Uint64(BigInteger.valueOf(3)),
            list.median(),
        )
    }

    @Test
    fun `median of single-element list returns that element`() {
        val list = listOf(Uint64(BigInteger.valueOf(42)))
        assertEquals(
            Uint64(BigInteger.valueOf(42)),
            list.median(),
        )
    }

    @Test
    fun `median throws on empty list`() {
        assertThrows<IllegalArgumentException> {
            emptyList<Uint64>().median()
        }
    }
}
