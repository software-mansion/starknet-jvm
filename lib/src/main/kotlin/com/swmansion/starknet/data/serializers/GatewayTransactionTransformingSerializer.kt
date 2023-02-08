package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.transactions.Transaction
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.jsonObject

object GatewayTransactionTransformingSerializer :
    JsonTransformingSerializer<Transaction>(TransactionPolymorphicSerializer) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        return element.jsonObject["transaction"]
            ?: throw SerializationException("Provided input does not contain a transaction - \"transaction\" key missing")
    }
}
