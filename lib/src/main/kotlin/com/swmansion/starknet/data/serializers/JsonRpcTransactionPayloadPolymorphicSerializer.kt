package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.transactions.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.*

internal object JsonRpcTransactionPayloadPolymorphicSerializer :
    JsonContentPolymorphicSerializer<TransactionPayload>(TransactionPayload::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out TransactionPayload> =
        when (element.jsonObject["type"]?.jsonPrimitive?.content) {
            "INVOKE" -> InvokeTransactionPayload.serializer()
            "DECLARE" -> selectDeclareDeserializer(element)
            "DEPLOY_ACCOUNT" -> DeployAccountTransactionPayload.serializer()
            else -> throw IllegalArgumentException("Invalid transaction type '${element.jsonObject["type"]?.jsonPrimitive?.content}'")
        }

    private fun selectDeclareDeserializer(element: JsonElement): DeserializationStrategy<out DeclareTransactionPayload> =
            when (element.jsonObject["version"]?.jsonPrimitive?.content) {
                Felt.ONE.hexString() -> DeclareTransactionV1Payload.serializer()
                Felt(2).hexString() -> DeclareTransactionV2Payload.serializer()
                else -> throw IllegalArgumentException("Invalid invoke transaction version")
            }
}
