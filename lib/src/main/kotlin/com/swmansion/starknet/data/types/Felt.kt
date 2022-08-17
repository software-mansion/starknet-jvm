package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.parseHex
import com.swmansion.starknet.extensions.toHex
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigInteger

@Serializable(with = FeltSerializer::class)
data class Felt(val value: BigInteger) {
    constructor(value: Long) : this(BigInteger.valueOf(value))
    constructor(value: Int) : this(BigInteger.valueOf(value.toLong()))

    init {
        if (value < BigInteger.ZERO) {
            throw java.lang.IllegalArgumentException("Default Felt constructor does not accept negative numbers, [$value] given.")
        }
        if (value > PRIME) {
            throw java.lang.IllegalArgumentException("Default Felt constructor does not accept numbers higher than P, [$value] given.")
        }
    }

    override fun toString(): String {
        return "Felt(${value.toHex()})"
    }

    fun hexString(): String {
        return value.toHex()
    }

    fun decString(): String {
        return value.toString(10)
    }

    companion object {
        @field:JvmField
        val PRIME = BigInteger("800000000000011000000000000000000000000000000000000000000000001", 16)

        @field:JvmField
        val ZERO = Felt(BigInteger.ZERO)

        @field:JvmField
        val ONE = Felt(BigInteger.ONE)

        @JvmStatic
        fun fromHex(value: String): Felt = Felt(parseHex(value))
    }
}

internal object FeltSerializer : KSerializer<Felt> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Felt", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Felt {
        val hex = decoder.decodeString()

        return Felt.fromHex(hex)
    }

    override fun serialize(encoder: Encoder, value: Felt) {
        encoder.encodeString(value.value.toHex())
    }
}
