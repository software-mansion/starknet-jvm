package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.InlineIntWrapper
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object InlineIntWrapperSerializer : KSerializer<InlineIntWrapper> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("InlineIntWrapper", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: InlineIntWrapper) {
        encoder.encodeInt(value.value)
    }

    override fun deserialize(decoder: Decoder): InlineIntWrapper {
        return InlineIntWrapper(decoder.decodeInt())
    }
}
