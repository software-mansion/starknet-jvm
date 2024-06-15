package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.IntResponse
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class IntResponseSerializerTest {
    @Test
    fun `serialize response`() {
        val expected = "123"
        val encoded = Json.encodeToString(IntResponseSerializer, IntResponse(123))
        assertEquals(expected, encoded)
    }
}
