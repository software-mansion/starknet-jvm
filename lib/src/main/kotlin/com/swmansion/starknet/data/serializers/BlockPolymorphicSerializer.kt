package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.data.types.transactions.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
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
            false -> ProcessedBlockWithReceiptsSerializer
        }
    }
}

internal object ProcessedBlockWithReceiptsSerializer : KSerializer<ProcessedBlockWithReceipts> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ProcessedBlockWithReceipts", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ProcessedBlockWithReceipts) {
        ProcessedBlockWithReceipts.serializer().serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): ProcessedBlockWithReceipts {
        val block = ProcessedBlockWithReceipts.serializer().deserialize(decoder)

        val transactionsWithReceipts = block.transactionsWithReceipts.map { transactionWithReceipt ->
            val updatedReceipt = when (val receipt = transactionWithReceipt.receipt) {
                is InvokeTransactionReceipt -> receipt.copy(blockHash = block.blockHash, blockNumber = block.blockNumber)
                is DeclareTransactionReceipt -> receipt.copy(blockHash = block.blockHash, blockNumber = block.blockNumber)
                is DeployAccountTransactionReceipt -> receipt.copy(blockHash = block.blockHash, blockNumber = block.blockNumber)
                is DeployTransactionReceipt -> receipt.copy(blockHash = block.blockHash, blockNumber = block.blockNumber)
                is L1HandlerTransactionReceipt -> receipt.copy(blockHash = block.blockHash, blockNumber = block.blockNumber)
            }
            transactionWithReceipt.copy(receipt = updatedReceipt)
        }

        return block.copy(transactionsWithReceipts = transactionsWithReceipts)
    }
}

private fun isPendingBlock(element: JsonObject): Boolean {
    return listOf("block_hash", "block_number", "new_root").any { it !in element }
}
