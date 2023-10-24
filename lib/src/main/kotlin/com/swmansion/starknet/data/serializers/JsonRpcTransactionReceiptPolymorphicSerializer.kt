package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.transactions.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*

internal object JsonRpcTransactionReceiptPolymorphicSerializer :
    JsonContentPolymorphicSerializer<TransactionReceipt>(TransactionReceipt::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out TransactionReceipt> {
        val jsonElement = element.jsonObject
        val typeElement = jsonElement.getOrElse("type") { throw SerializationException("Input element does not contain mandatory field 'type'") }

        val type = Json.decodeFromJsonElement(TransactionType.serializer(), typeElement)
        val isPending = listOf("block_hash", "block_number").any { it !in jsonElement }

        return when (type) {
            TransactionType.DEPLOY -> when (isPending) {
                false -> DeployRpcTransactionReceipt.serializer()
                true -> throw SerializationException("Pending deploy transaction receipt is not supported")
            }
            TransactionType.DEPLOY_ACCOUNT -> when (isPending) {
                false -> DeployAccountRpcTransactionReceipt.serializer()
                true -> PendingDeployAccountRpcTransactionReceipt.serializer()
            }
            TransactionType.INVOKE -> return when (isPending) {
                false -> InvokeRpcTransactionReceipt.serializer()
                true -> PendingInvokeRpcTransactionReceipt.serializer()
            }
            TransactionType.DECLARE -> return when (isPending) {
                false -> DeclareRpcTransactionReceipt.serializer()
                true -> PendingDeclareRpcTransactionReceipt.serializer()
            }
            TransactionType.L1_HANDLER -> return when (isPending) {
                false -> L1HandlerRpcTransactionReceipt.serializer()
                true -> PendingL1HandlerRpcTransactionReceipt.serializer()
            }
        }
    }
}
