package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.transactions.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*

internal object JsonRpcTransactionReceiptPolymorphicSerializer :
    JsonContentPolymorphicSerializer<TransactionReceipt>(TransactionReceipt::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out TransactionReceipt> {
        val jsonElement = element.jsonObject
        val isPending = "status" !in jsonElement
        return if (isPending) selectPendingDeserializer(jsonElement) else selectDeserializer(jsonElement)
    }

    private fun selectPendingDeserializer(element: JsonObject): DeserializationStrategy<out TransactionReceipt> =
        when {
            "contract_address" in element -> PendingRpcDeployTransactionReceipt.serializer()
            else -> PendingRpcTransactionReceipt.serializer()
        }

    private fun selectDeserializer(element: JsonObject): DeserializationStrategy<out TransactionReceipt> =
        when (element["type"]?.jsonPrimitive?.content) {
            "DEPLOY" -> DeployRpcTransactionReceipt.serializer()
            "DEPLOY_ACCOUNT" -> DeployRpcTransactionReceipt.serializer()
            null -> throw SerializationException("Input element does not contain mandatory field 'type'")
            else -> RpcTransactionReceipt.serializer()
        }
}
