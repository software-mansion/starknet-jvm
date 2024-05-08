package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.StarknetString
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object StarknetStringSerializer : KSerializer<StarknetString> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("StarknetString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: StarknetString) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): StarknetString {
        return StarknetString(decoder.decodeString())
    }
}
