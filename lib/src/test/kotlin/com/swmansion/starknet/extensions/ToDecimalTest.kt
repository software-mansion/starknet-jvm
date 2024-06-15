package com.swmansion.starknet.extensions

import com.swmansion.starknet.data.types.Felt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ToDecimalTest {
    @Test
    fun `test Felt list toDecimal`() {
        val result = listOf(Felt(0), Felt(10), Felt(123)).toDecimal()
        val expected = listOf("0", "10", "123")
        assertEquals(result, expected)
    }
}
