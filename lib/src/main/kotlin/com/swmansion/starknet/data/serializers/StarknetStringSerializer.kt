package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.StringResponse
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal object StringResponseSerializer : KSerializer<StringResponse> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("StringResponse", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: StringResponse) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): StringResponse {
        return StringResponse(decoder.decodeString())
    }
}
