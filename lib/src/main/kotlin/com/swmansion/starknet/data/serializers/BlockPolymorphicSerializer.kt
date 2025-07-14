package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.*
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
        return when (isProcessedBlock(element.jsonObject)) {
            true -> ProcessedBlockWithTransactions.serializer()
            false -> PreConfirmedBlockWithTransactions.serializer()
        }
    }
}

internal object BlockWithTransactionHashesPolymorphicSerializer : JsonContentPolymorphicSerializer<BlockWithTransactionHashes>(BlockWithTransactionHashes::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<BlockWithTransactionHashes> {
        return when (isProcessedBlock(element.jsonObject)) {
            true -> ProcessedBlockWithTransactionHashes.serializer()
            false -> PreConfirmedBlockWithTransactionHashes.serializer()
        }
    }
}

internal object BlockWithReceiptsPolymorphicSerializer : JsonContentPolymorphicSerializer<BlockWithReceipts>(BlockWithReceipts::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<BlockWithReceipts> {
        return when (isProcessedBlock(element.jsonObject)) {
            true -> ProcessedBlockWithReceiptsSerializer
            false -> PreConfirmedBlockWithReceipts.serializer()
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

private fun isProcessedBlock(element: JsonObject): Boolean {
    // Only processed blocks have "parent_hash"
    return "parent_hash" in element
}
