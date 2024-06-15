package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.StringResponse
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StringResponseSerializerTest {
    @Test
    fun `serialize string response`() {
        val expected = "\"Example text\""
        val encoded = Json.encodeToString(StringResponseSerializer, StringResponse("Example text"))
        assertEquals(expected, encoded)
    }
}
