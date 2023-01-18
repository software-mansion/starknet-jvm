package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.transactions.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.*

internal object JsonRpcTransactionPayloadPolymorphicSerializer :
    JsonContentPolymorphicSerializer<TransactionPayload>(TransactionPayload::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out TransactionPayload> =
        when (element.jsonObject["type"]?.jsonPrimitive?.content) {
            "INVOKE" -> InvokeTransactionPayload.serializer()
            "DECLARE" -> DeclareTransactionPayload.serializer()
            "DEPLOY_ACCOUNT" -> DeployAccountTransactionPayload.serializer()
            else -> throw IllegalArgumentException("Invalid transaction type '${element.jsonObject["type"]?.jsonPrimitive?.content}'")
        }
}
