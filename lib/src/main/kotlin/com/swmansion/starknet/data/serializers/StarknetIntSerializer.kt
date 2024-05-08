package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.StarknetInt
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object StarknetIntSerializer : KSerializer<StarknetInt> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("StarknetInt", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: StarknetInt) {
        encoder.encodeInt(value.value)
    }

    override fun deserialize(decoder: Decoder): StarknetInt {
        return StarknetInt(decoder.decodeInt())
    }
}
