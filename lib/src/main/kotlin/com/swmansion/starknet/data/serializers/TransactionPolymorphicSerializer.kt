package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.transactions.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.*

internal object TransactionPolymorphicSerializer : JsonContentPolymorphicSerializer<Transaction>(Transaction::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out Transaction> =
        // pick deserializer based on transaction type field
        when (element.jsonObject["type"]?.jsonPrimitive?.content) {
            "INVOKE_FUNCTION" -> selectInvokeDeserializer(element)
            "INVOKE" -> selectInvokeDeserializer(element)
            "DECLARE" -> selectDeclareDeserializer(element)
            "DEPLOY" -> DeployTransaction.serializer()
            "DEPLOY_ACCOUNT" -> DeployAccountTransaction.serializer()
            "L1_HANDLER" -> L1HandlerTransaction.serializer()
            else -> throw IllegalArgumentException("Invalid transaction type '${element.jsonObject["type"]?.jsonPrimitive?.content}'")
        }

    private fun selectInvokeDeserializer(element: JsonElement): DeserializationStrategy<out InvokeTransaction> =
        when (element.jsonObject["version"]?.jsonPrimitive?.content) {
            Felt.ONE.hexString() -> InvokeTransactionV1.serializer()
            Felt.ZERO.hexString() -> InvokeTransactionV0.serializer()
            else -> throw IllegalArgumentException("Invalid invoke transaction version")
        }

    private fun selectDeclareDeserializer(element: JsonElement): DeserializationStrategy<out DeclareTransaction> {
        // TODO: add additional condition that checks that either contract_class or class_hash is present

        if ((element.jsonObject["contract_class"] == null) && (element.jsonObject["class_hash"] == null)) {
            throw IllegalArgumentException("Either classHash or contractDefinition must be present.")
        }

        when (element.jsonObject["version"]?.jsonPrimitive?.content) {
            Felt.ONE.hexString() -> return DeclareTransactionV1.serializer()
            Felt(2).hexString() -> return DeclareTransactionV2.serializer()
            else -> throw IllegalArgumentException("Invalid invoke transaction version")
        }
    }
}
