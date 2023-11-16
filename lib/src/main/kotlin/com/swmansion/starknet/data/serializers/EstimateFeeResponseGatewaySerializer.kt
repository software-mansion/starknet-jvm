package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.EstimateFeeResponse
import com.swmansion.starknet.extensions.toFelt
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = EstimateFeeResponse::class)
internal object EstimateFeeResponseGatewaySerializer : KSerializer<EstimateFeeResponse> {
    override fun deserialize(decoder: Decoder): EstimateFeeResponse {
        val input = decoder as? JsonDecoder ?: throw SerializationException("Expected JsonInput for ${decoder::class}")

        val jsonObject = input.decodeJsonElement().jsonObject

        val gasUsage = jsonObject.getValue("gas_usage").jsonPrimitive.content.toBigInteger().toFelt
        val gasPrice = jsonObject.getValue("gas_price").jsonPrimitive.content.toBigInteger().toFelt
        val overallFee = jsonObject.getValue("overall_fee").jsonPrimitive.content.toBigInteger().toFelt

        return EstimateFeeResponse(
            gasConsumed = gasUsage,
            gasPrice = gasPrice,
            overallFee = overallFee,
        )
    }

    override val descriptor: SerialDescriptor
        get() = EstimateFeeResponse.serializer().descriptor

    override fun serialize(encoder: Encoder, value: EstimateFeeResponse) {
        val jsonObject = buildJsonObject {
            put("gas_usage", value.gasConsumed.value.toString(10))
            put("gas_price", value.gasPrice.value.toString(10))
            put("overall_fee", value.overallFee.value.toString(10))
        }

        val output = encoder as? JsonEncoder ?: throw SerializationException("")

        output.encodeJsonElement(jsonObject)
    }
}
