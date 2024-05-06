package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.InlineStringWrapper
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object InlineStringWrapperSerializer : KSerializer<InlineStringWrapper> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("InlineStringWrapper", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: InlineStringWrapper) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): InlineStringWrapper {
        return InlineStringWrapper(decoder.decodeString())
    }
}
