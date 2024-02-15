package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.StarknetChainId
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal object StarknetChainIdSerializer : KSerializer<StarknetChainId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("StarknetChainId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: StarknetChainId) {
        encoder.encodeString(value.value.hexString())
    }

    override fun deserialize(decoder: Decoder): StarknetChainId {
        val hexString = decoder.decodeString()
        return StarknetChainId.fromHex(hexString)
    }
}
