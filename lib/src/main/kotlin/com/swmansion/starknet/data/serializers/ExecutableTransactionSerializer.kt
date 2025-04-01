package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

internal object ExecutableTransactionSerializer : KSerializer<ExecutableTransaction> {
    override val descriptor = PrimitiveSerialDescriptor("ExecutableTransaction", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ExecutableTransaction {
        throw SerializationException("Class used for serialization only.")
    }

    override fun serialize(encoder: Encoder, value: ExecutableTransaction) {
        require(encoder is JsonEncoder)

        val jsonObject = when (value) {
            is InvokeTransactionV3 -> encoder.json.encodeToJsonElement(InvokeTransactionV3.serializer(), value).jsonObject
            is DeclareTransactionV3 -> encoder.json.encodeToJsonElement(DeclareTransactionV3Serializer, value).jsonObject
            is DeployAccountTransactionV3 -> encoder.json.encodeToJsonElement(DeployAccountTransactionV3.serializer(), value).jsonObject
        }
        val result = JsonObject(
            jsonObject.filter { (key, _) -> !transactionIgnoredKeys.contains(key) }.plus("type" to encoder.json.encodeToJsonElement(value.type)),
        )
        encoder.encodeJsonElement(result)
    }
}
