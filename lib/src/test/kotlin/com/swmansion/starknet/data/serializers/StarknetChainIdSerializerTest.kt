package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.StarknetChainId
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StarknetChainIdSerializerTest {
    @Test
    fun `serialize starknet chain ids`() {
        val chainIds = listOf(
            StarknetChainId.MAIN,
            StarknetChainId.SEPOLIA,
            StarknetChainId.INTEGRATION_SEPOLIA,
        )
        for (chainId in chainIds) {
            val expected = "\"${chainId.value.hexString()}\""
            val encoded = Json.encodeToString(StarknetChainId.serializer(), chainId)
            assertEquals(expected, encoded)
        }
    }
}
