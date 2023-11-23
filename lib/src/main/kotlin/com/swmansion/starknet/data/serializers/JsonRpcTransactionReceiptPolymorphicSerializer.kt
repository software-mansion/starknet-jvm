package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.transactions.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*

internal object JsonRpcTransactionReceiptPolymorphicSerializer :
    JsonContentPolymorphicSerializer<TransactionReceipt>(TransactionReceipt::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<TransactionReceipt> {
        val jsonElement = element.jsonObject
        val typeElement = jsonElement.getOrElse("type") { throw SerializationException("Input element does not contain mandatory field 'type'") }

        val type = Json.decodeFromJsonElement(TransactionType.serializer(), typeElement)
        val isPending = listOf("block_hash", "block_number").any { it !in jsonElement }

        return when (type) {
            TransactionType.INVOKE -> when (isPending) {
                false -> ProcessedInvokeTransactionReceipt.serializer()
                true -> PendingInvokeTransactionReceipt.serializer()
            }
            TransactionType.DECLARE -> when (isPending) {
                false -> ProcessedDeclareTransactionReceipt.serializer()
                true -> PendingDeclareTransactionReceipt.serializer()
            }
            TransactionType.DEPLOY_ACCOUNT -> when (isPending) {
                false -> ProcessedDeployAccountTransactionReceipt.serializer()
                true -> PendingDeployAccountTransactionReceipt.serializer()
            }
            TransactionType.DEPLOY -> ProcessedDeployTransactionReceipt.serializer()
            TransactionType.L1_HANDLER -> when (isPending) {
                false -> ProcessedL1HandlerTransactionReceipt.serializer()
                true -> PendingL1HandlerTransactionReceipt.serializer()
            }
        }
    }
}
