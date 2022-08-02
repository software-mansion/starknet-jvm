package com.swmansion.starknet.data.responses.serializers

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import com.swmansion.starknet.data.responses.DeclareTransaction
import com.swmansion.starknet.data.responses.DeployTransaction
import com.swmansion.starknet.data.responses.InvokeTransaction
import com.swmansion.starknet.data.responses.Transaction

object JsonRpcTransactionPolymorphicSerializer : JsonContentPolymorphicSerializer<Transaction>(Transaction::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out Transaction> = when {
        "contract_class" in element.jsonObject -> DeclareTransaction.serializer()
        "entry_point_selector" in element.jsonObject && element.jsonObject["entry_point_selector"] !is JsonNull -> InvokeTransaction.serializer()
        else -> DeployTransaction.serializer()
    }
}
