package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.StringWrapper
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object StringWrapperSerializer : KSerializer<StringWrapper> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("StringWrapper", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: StringWrapper) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): StringWrapper {
        return StringWrapper(decoder.decodeString())
    }
}
