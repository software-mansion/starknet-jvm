package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.IntWrapper
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object IntWrapperSerializer : KSerializer<IntWrapper> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("IntWrapper", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: IntWrapper) {
        encoder.encodeInt(value.value)
    }

    override fun deserialize(decoder: Decoder): IntWrapper {
        return IntWrapper(decoder.decodeInt())
    }
}
