package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.parseHex
import com.swmansion.starknet.extensions.*
import com.swmansion.starknet.extensions.addHexPrefix
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
data class Felt(val value: BigInteger) : Comparable<Felt> {
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

    override fun compareTo(other: Felt): Int {
        return value.compareTo(other.value)
    }

    override fun toString(): String {
        return "Felt(${value.toHex()})"
    }

    /**
     * Encode as hexadecimal string, including "0x" prefix.
     */
    fun hexString(): String {
        return value.toHex()
    }

    /**
     * Encode as decimal string.
     */
    fun decString(): String {
        return value.toString(10)
    }

    /**
     * Encode as ASCII string, with up to 31 characters.
     * Example: 0x68656c6c6f -> "hello".
     */
    fun toShortString(): String {
        var hexString = this.hexString().removeHexPrefix()

        if (hexString.length % 2 == 1) {
            hexString = hexString.padStart(hexString.length + 1, '0')
        }

        val decoded = hexString.replace(Regex(".{2}")) { hex ->
            hex.value.toInt(16).toChar().toString()
        }

        return decoded
    }

    companion object {
        @field:JvmField
        val PRIME = BigInteger("800000000000011000000000000000000000000000000000000000000000001", 16)

        @field:JvmField
        val ZERO = Felt(BigInteger.ZERO)

        @field:JvmField
        val ONE = Felt(BigInteger.ONE)

        /**
         * Create Felt from hex string. It must start with "0x" prefix.
         *
         * @param value hex string.
         */
        @JvmStatic
        fun fromHex(value: String): Felt = Felt(parseHex(value))

        /**
         * Create Felt from ASCII string. It must be shorter than 32 characters and only contain ASCII encoding.
         * Example: "hello" -> 0x68656c6c6f.
         *
         * @param value string transformed to felt.
         */
        @JvmStatic
        fun fromShortString(value: String): Felt {
            if (value.length > 31) {
                throw IllegalArgumentException("Short string cannot be longer than 31 characters.")
            }
            if (!isAscii(value)) {
                throw IllegalArgumentException("String to be encoded must be an ascii string.")
            }

            val encoded = value.replace(Regex(".")) { s ->
                s.value.first().code.toString(16).padStart(2, '0')
            }

            return fromHex(encoded.addHexPrefix())
        }

        private fun isAscii(string: String): Boolean {
            for (char in string) {
                if (char.code < 0 || char.code > 127) {
                    return false
                }
            }

            return true
        }
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
