package starknet.utils.data.serializers

import com.swmansion.starknet.data.types.Uint128
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object Uint128DecimalSerializer : KSerializer<Uint128> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Uint128(decimal)", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Uint128) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Uint128 {
        val str = decoder.decodeString()
        return if (str.startsWith("0x")) {
            Uint128.fromHex(str)
        } else {
            Uint128(str.toBigInteger())
        }
    }
}
