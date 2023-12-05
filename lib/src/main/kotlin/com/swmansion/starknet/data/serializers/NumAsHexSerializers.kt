package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal object NumAsHexSerializer : HexSerializer<NumAsHex>(
    NumAsHex::fromHex,
    NumAsHex::hexString,
    PrimitiveSerialDescriptor("NumAsHex", PrimitiveKind.STRING),
)

internal object FeltSerializer : HexSerializer<Felt>(
    Felt::fromHex,
    Felt::hexString,
    PrimitiveSerialDescriptor("Felt", PrimitiveKind.STRING),
)

internal object Uint64Serializer : HexSerializer<Uint64>(
    Uint64::fromHex,
    Uint64::hexString,
    PrimitiveSerialDescriptor("Uint64", PrimitiveKind.STRING),
)

internal object Uint128Serializer : HexSerializer<Uint128>(
    Uint128::fromHex,
    Uint128::hexString,
    PrimitiveSerialDescriptor("Uint128", PrimitiveKind.STRING),
)

internal sealed class HexSerializer<T : NumAsHexBase>(
    private val fromHex: (String) -> T,
    private val hexString: (T) -> String,
    override val descriptor: SerialDescriptor,
) : KSerializer<T> {
    override fun deserialize(decoder: Decoder): T {
        val hex = decoder.decodeString()
        return fromHex(hex)
    }

    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeString(hexString(value))
    }
}
