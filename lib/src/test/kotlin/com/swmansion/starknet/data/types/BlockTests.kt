package com.swmansion.starknet.data.types

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class BlockTests {
    private fun loadJsonString(path: String): String {
        return File("src/test/resources/types/block/$path").readText()
    }

    @Test
    fun `deserialize processed block with transactions`() {
        val jsonString = loadJsonString("processed_block_with_transactions.json")
        val blockWithTransactions = Json.decodeFromString(ProcessedBlockWithTransactions.serializer(), jsonString)
        assertTrue(blockWithTransactions is ProcessedBlockWithTransactions)
    }
}
