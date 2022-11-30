package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.transactions.*
import kotlinx.serialization.DeserializationStrategy
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
        when (element["type"]!!.jsonPrimitive) {
            JsonPrimitive(TransactionReceiptType.DEPLOY.name) -> DeployRpcTransactionReceipt.serializer()
            else -> RpcTransactionReceipt.serializer()
        }
}
