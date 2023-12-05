package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.parseHex
import com.swmansion.starknet.data.serializers.FeltSerializer
import com.swmansion.starknet.data.types.conversions.ConvertibleToCalldata
import com.swmansion.starknet.extensions.toHex
import kotlinx.serialization.Serializable
import java.math.BigInteger

@Serializable(with = FeltSerializer::class)
data class Felt(override val value: BigInteger) : NumAsHexBase(value), ConvertibleToCalldata {
    constructor(value: Long) : this(BigInteger.valueOf(value))
    constructor(value: Int) : this(BigInteger.valueOf(value.toLong()))

    init {
        if (value < BigInteger.ZERO) {
            throw IllegalArgumentException("Default Felt constructor does not accept negative numbers, [$value] given.")
        }
        if (value >= PRIME) {
            throw IllegalArgumentException("Default Felt constructor accepts values smaller than Felt.PRIME, [$value] given.")
        }
    }

    override fun toCalldata() = listOf(this)

    override fun toString() = "Felt(${value.toHex()})"

    override fun hexString() = value.toHex()

    override fun decString(): String = value.toString(10)

    /**
     * Encode as ASCII string, with up to 31 characters.
     * Example: 0x68656c6c6f -> "hello".
     */
    fun toShortString(): String {
        var hexString = this.value.toString(16)

        if (hexString.length % 2 == 1) {
            hexString = hexString.padStart(hexString.length + 1, '0')
        }

        return hexString.chunked(2).joinToString("") { hex ->
            hex.toInt(16).toChar().toString()
        }
    }

    companion object {
        @field:JvmField
        val PRIME = BigInteger("800000000000011000000000000000000000000000000000000000000000001", 16)

        @field:JvmField
        val MAX = PRIME.minus(BigInteger.ONE)

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
        fun fromHex(value: String): Felt {
            return Felt(parseHex(value))
        }

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

            val encoded = value.toCharArray().joinToString("") { c ->
                c.code.toString(16).padStart(2, '0')
            }

            return fromHex("0x$encoded")
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
