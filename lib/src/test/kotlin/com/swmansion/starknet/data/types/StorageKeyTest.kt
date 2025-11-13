package com.swmansion.starknet.data.types

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class StorageKeyTest {

    companion object {
        @JvmStatic
        fun getHappyCaseData(): List<String> {
            return listOf(
                "0x0",
                "0x7abcdef",
            )
        }
    }

    @ParameterizedTest
    @MethodSource("getHappyCaseData")
    fun `happy case`(hex: String) {
        StorageKey(hex)
    }

    @Test
    fun `length exceeds limit`() {
        val hex = "0x7abcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdef\n"

        val exception = assertThrows(IllegalArgumentException::class.java) {
            StorageKey(hex)
        }
        assert(exception.message!!.contains("Invalid storage key format"))
    }
}