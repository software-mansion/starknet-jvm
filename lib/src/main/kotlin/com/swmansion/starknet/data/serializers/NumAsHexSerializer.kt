package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.NumAsHex
import com.swmansion.starknet.extensions.toHex
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal object NumAsHexSerializer : KSerializer<NumAsHex> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("NumAsHex", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): NumAsHex {
        val hex = decoder.decodeString()

        return NumAsHex.fromHex(hex)
    }

    override fun serialize(encoder: Encoder, value: NumAsHex) {
        encoder.encodeString(value.value.toHex())
    }
}
