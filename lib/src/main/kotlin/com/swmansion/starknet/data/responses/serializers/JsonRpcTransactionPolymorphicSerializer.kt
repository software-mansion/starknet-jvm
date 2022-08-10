package com.swmansion.starknet.data.responses.serializers

import com.swmansion.starknet.data.responses.DeclareTransaction
import com.swmansion.starknet.data.responses.DeployTransaction
import com.swmansion.starknet.data.responses.InvokeTransaction
import com.swmansion.starknet.data.responses.Transaction
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.*
import java.lang.IllegalArgumentException

object JsonRpcTransactionPolymorphicSerializer : JsonContentPolymorphicSerializer<Transaction>(Transaction::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out Transaction> =
        when (element.jsonObject["type"]!!.jsonPrimitive.content) {
            "INVOKE" -> InvokeTransaction.serializer()
            "DECLARE" -> DeclareTransaction.serializer()
            "DEPLOY" -> DeployTransaction.serializer()
            else -> throw IllegalArgumentException("Invalid transaction type")
        }
}
