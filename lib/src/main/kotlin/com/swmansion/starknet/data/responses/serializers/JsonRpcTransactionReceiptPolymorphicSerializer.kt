package com.swmansion.starknet.data.responses.serializers

import com.swmansion.starknet.data.types.transactions.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

object JsonRpcTransactionReceiptPolymorphicSerializer :
    JsonContentPolymorphicSerializer<TransactionReceipt>(TransactionReceipt::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out TransactionReceipt> {
        val jsonElement = element.jsonObject
        val isPending = "status" !in jsonElement
        return if (isPending) selectPendingDeserializer(jsonElement) else selectDeserializer(jsonElement)
    }

    private fun selectPendingDeserializer(element: JsonObject): DeserializationStrategy<out TransactionReceipt> =
        when {
            "messages_sent" in element -> PendingInvokeTransactionReceipt.serializer()
            else -> PendingTransactionReceipt.serializer()
        }

    private fun selectDeserializer(element: JsonObject): DeserializationStrategy<out TransactionReceipt> =
        // FIXME(we should be able to distinguish between declare and deploy receipts but it's impossible with the current rpc spec)
        when {
            "messages_sent" in element -> InvokeTransactionReceipt.serializer()
            else -> DeclareTransactionReceipt.serializer()
        }
}
