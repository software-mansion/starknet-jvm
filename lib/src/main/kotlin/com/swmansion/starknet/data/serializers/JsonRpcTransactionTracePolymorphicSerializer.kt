package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.transactions.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.*

internal object JsonRpcTransactionTracePolymorphicSerializer :
    JsonContentPolymorphicSerializer<TransactionTrace>(TransactionTrace::class) {
    private fun selectInvokeTransactionTraceDeserializer(jsonObject: JsonObject): DeserializationStrategy<out InvokeTransactionTrace> {
        val executeInvocation = jsonObject["execute_invocation"]?.jsonObject ?: throw IllegalStateException("Response from node contains invalid INVOKE_TXN_TRACE: execute_invocation is missing.")
        val isReverted = "revert_reason" in executeInvocation

        return when (isReverted) {
            true -> RevertedInvokeTransactionTrace.serializer()
            false -> CommonInvokeTransactionTrace.serializer()
        }
    }

    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out TransactionTrace> {
        val jsonObject = element.jsonObject

        return when {
            "execute_invocation" in jsonObject -> selectInvokeTransactionTraceDeserializer(jsonObject)
            "constructor_invocation" in jsonObject -> DeployAccountTransactionTrace.serializer()
            "function_invocation" in jsonObject -> L1HandlerTransactionTrace.serializer()
            listOf("validate_invocation", "fee_transfer_invocation").any { it in jsonObject } -> DeclareTransactionTrace.serializer()
            else -> throw IllegalStateException("Unknown transaction trace type.")
        }
    }
}
