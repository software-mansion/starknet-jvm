package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.parseHex
import com.swmansion.starknet.data.serializers.Uint128Serializer
import com.swmansion.starknet.data.types.conversions.ConvertibleToCalldata
import com.swmansion.starknet.extensions.toFelt
import com.swmansion.starknet.extensions.toHex
import kotlinx.serialization.Serializable
import java.math.BigInteger

@Serializable(with = Uint128Serializer::class)
data class Uint128(override val value: BigInteger) : NumAsHexBase(value), ConvertibleToCalldata {
    constructor(value: Long) : this(BigInteger.valueOf(value))
    constructor(value: Int) : this(BigInteger.valueOf(value.toLong()))

    init {
        if (value < BigInteger.ZERO) {
            throw IllegalArgumentException("Default Uint128 constructor does not accept negative numbers, [$value] given.")
        }
        if (value >= MAX) {
            throw IllegalArgumentException("Default Uint128 constructor does not accept numbers higher than 2^128-1, [$value] given.")
        }
    }

    override fun toCalldata() = listOf(this.value.toFelt)

    override fun toString() = "Uint128(${value.toHex()})"

    override fun hexString() = value.toHex()

    override fun decString(): String = value.toString(10)

    companion object {
        @field:JvmField
        val MAX = BigInteger.valueOf(2).pow(128) - (BigInteger.ONE)

        @field:JvmField
        val ZERO = Uint128(BigInteger.ZERO)

        @field:JvmField
        val ONE = Uint128(BigInteger.ONE)

        /**
         * Create Uint128 from hex string. It must start with "0x" prefix.
         *
         * @param value hex string.
         */
        @JvmStatic
        fun fromHex(value: String): Uint128 {
            return Uint128(parseHex(value))
        }
    }
}
