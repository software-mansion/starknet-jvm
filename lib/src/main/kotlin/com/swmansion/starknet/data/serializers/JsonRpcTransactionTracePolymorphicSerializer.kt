package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.transactions.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.*

internal object JsonRpcTransactionTracePolymorphicSerializer :
    JsonContentPolymorphicSerializer<TransactionTrace>(TransactionTrace::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out TransactionTrace> {
        val jsonObject = element.jsonObject

        return when (jsonObject.keys) {
            setOf("validate_invocation", "execute_invocation", "fee_transfer_invocation") -> InvokeTransactionTrace.serializer()
            setOf("validate_invocation", "fee_transfer_invocation") -> DeclareTransactionTrace.serializer()
            setOf("validate_invocation", "constructor_invocation", "fee_transfer_invocation") -> DeployAccountTransactionTrace.serializer()
            setOf("functionInvocation") -> L1HandlerTransactionTrace.serializer()
            else -> throw IllegalArgumentException("Invalid transaction trace type")
        }
    }
}
