package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.transactions.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.*

internal object TransactionPolymorphicSerializer : JsonContentPolymorphicSerializer<Transaction>(Transaction::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out Transaction> =
        when (element.jsonObject["type"]?.jsonPrimitive?.content) {
            "INVOKE_FUNCTION" -> selectInvokeDeserializer(element)
            "INVOKE" -> selectInvokeDeserializer(element)
            "DECLARE" -> DeclareTransaction.serializer()
            "DEPLOY" -> DeployTransaction.serializer()
            "DEPLOY_ACCOUNT" -> DeployAccountTransaction.serializer()
            "L1_HANDLER" -> L1HandlerTransaction.serializer()
            else -> throw IllegalArgumentException("Invalid transaction type")
        }

    private fun selectInvokeDeserializer(element: JsonElement): DeserializationStrategy<out InvokeTransaction> =
        when (element.jsonObject["version"]?.jsonPrimitive?.content) {
            Felt.ONE.hexString() -> InvokeTransactionV1.serializer()
            Felt.ZERO.hexString() -> InvokeTransactionV0.serializer()
            else -> throw IllegalArgumentException("Invalid invoke transaction version")
        }
}
