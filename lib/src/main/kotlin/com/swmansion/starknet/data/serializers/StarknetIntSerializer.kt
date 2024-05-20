package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.IntResponse
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal object IntResponseSerializer : KSerializer<IntResponse> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("IntResponse", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: IntResponse) {
        encoder.encodeInt(value.value)
    }

    override fun deserialize(decoder: Decoder): IntResponse {
        return IntResponse(decoder.decodeInt())
    }
}
