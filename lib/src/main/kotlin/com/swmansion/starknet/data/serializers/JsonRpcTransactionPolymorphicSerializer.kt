package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.transactions.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.*

internal object JsonRpcTransactionPolymorphicSerializer : JsonContentPolymorphicSerializer<Transaction>(Transaction::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out Transaction> =
        when (element.jsonObject["type"]!!.jsonPrimitive.content) {
            "INVOKE" -> InvokeTransaction.serializer()
            "DECLARE" -> DeclareTransaction.serializer()
            "DEPLOY" -> DeployTransaction.serializer()
            "DEPLOY_ACCOUNT" -> DeployAccountTransaction.serializer()
            "L1_HANDLER" -> L1HandlerTransaction.serializer()
            else -> throw IllegalArgumentException("Invalid transaction type")
        }
}
