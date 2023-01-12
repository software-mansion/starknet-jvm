package com.swmansion.starknet.data.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal object HexToIntDeserializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("HexToInt", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Int {
        val hex = decoder.decodeString()

        return Integer.decode(hex)
    }

    override fun serialize(encoder: Encoder, value: Int) {
        throw UnsupportedOperationException("Class used for deserialization only.")
    }
}
