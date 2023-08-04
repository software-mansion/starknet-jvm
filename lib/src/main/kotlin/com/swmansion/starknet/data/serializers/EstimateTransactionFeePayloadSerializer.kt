package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.EstimateTransactionFeePayload
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
object EstimateTransactionFeePayloadSerializer : KSerializer<EstimateTransactionFeePayload> {

    override val descriptor: SerialDescriptor
        get() = EstimateTransactionFeePayload.serializer().descriptor

    override fun serialize(encoder: Encoder, value: EstimateTransactionFeePayload) {
        require(encoder is JsonEncoder)

        val jsonObject = buildJsonObject {
            putJsonArray("request") { value.request.forEach {
                add(Json.encodeToJsonElement(JsonRpcTransactionPayloadSerializer, it)) }
            }
            put("block_id", value.blockId.toString())
        }

        encoder.encodeJsonElement(jsonObject)
    }

    override fun deserialize(decoder: Decoder): EstimateTransactionFeePayload {
        throw SerializationException("Class used for serialization only.")
    }
}