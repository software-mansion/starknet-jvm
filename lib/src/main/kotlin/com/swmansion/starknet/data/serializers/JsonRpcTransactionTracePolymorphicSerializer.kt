package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.transactions.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*
import java.lang.IllegalArgumentException

internal object JsonRpcTransactionTracePolymorphicSerializer :
    JsonContentPolymorphicSerializer<TransactionTrace>(TransactionTrace::class) {
    private fun selectInvokeTransactionTraceDeserializer(jsonObject: JsonObject): DeserializationStrategy<out InvokeTransactionTraceBase> {
        val executeInvocation = jsonObject["execute_invocation"]?.jsonObject ?: throw IllegalStateException("Response from node contains invalid INVOKE_TXN_TRACE: execute_invocation is missing.")
        val isReverted = "revert_reason" in executeInvocation

        return when (isReverted) {
            true -> RevertedInvokeTransactionTrace.serializer()
            false -> InvokeTransactionTrace.serializer()
        }
    }

    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out TransactionTrace> {
        val jsonObject = element.jsonObject

        val typeElement = jsonObject.getOrElse("type") { throw SerializationException("Input element does not contain mandatory field 'type'") }
        val type = Json.decodeFromJsonElement(TransactionType.serializer(), typeElement)

        return when (type) {
            TransactionType.INVOKE -> selectInvokeTransactionTraceDeserializer(jsonObject)
            TransactionType.DEPLOY_ACCOUNT -> DeployAccountTransactionTrace.serializer()
            TransactionType.L1_HANDLER -> L1HandlerTransactionTrace.serializer()
            TransactionType.DECLARE -> DeclareTransactionTrace.serializer()
            else -> throw IllegalArgumentException("Unknown transaction trace type '${typeElement.jsonPrimitive.content}'")
        }
    }
}
