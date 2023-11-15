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
                false -> ProcessedInvokeRpcTransactionReceipt.serializer()
                true -> PendingInvokeRpcTransactionReceipt.serializer()
            }
            TransactionType.DECLARE -> when (isPending) {
                false -> ProcessedDeclareRpcTransactionReceipt.serializer()
                true -> PendingDeclareRpcTransactionReceipt.serializer()
            }
            TransactionType.DEPLOY_ACCOUNT -> when (isPending) {
                false -> ProcessedDeployAccountRpcTransactionReceipt.serializer()
                true -> PendingDeployAccountRpcTransactionReceipt.serializer()
            }
            TransactionType.DEPLOY -> ProcessedDeployRpcTransactionReceipt.serializer()
            TransactionType.L1_HANDLER -> when (isPending) {
                false -> ProcessedL1HandlerRpcTransactionReceipt.serializer()
                true -> PendingL1HandlerRpcTransactionReceipt.serializer()
            }
        }
    }
}
