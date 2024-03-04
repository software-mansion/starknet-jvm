package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

internal object BlockWithTransactionsPolymorphicSerializer : JsonContentPolymorphicSerializer<BlockWithTransactions>(BlockWithTransactions::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<BlockWithTransactions> {
        return when (isPendingBlock(element.jsonObject)) {
            true -> PendingBlockWithTransactions.serializer()
            false -> ProcessedBlockWithTransactions.serializer()
        }
    }
}

internal object BlockWithTransactionHashesPolymorphicSerializer : JsonContentPolymorphicSerializer<BlockWithTransactionHashes>(BlockWithTransactionHashes::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<BlockWithTransactionHashes> {
        return when (isPendingBlock(element.jsonObject)) {
            true -> PendingBlockWithTransactionHashes.serializer()
            false -> ProcessedBlockWithTransactionHashes.serializer()
        }
    }
}

internal object BlockWithReceiptsPolymorphicSerializer : JsonContentPolymorphicSerializer<BlockWithReceipts>(BlockWithReceipts::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<BlockWithReceipts> {
        return when (isPendingBlock(element.jsonObject)) {
            true -> PendingBlockWithReceipts.serializer()
            false -> ProcessedBlockWithReceipts.serializer()
        }
    }
}

private fun isPendingBlock(element: JsonObject): Boolean {
    return listOf("block_hash", "block_number", "new_root").any { it !in element }
}
