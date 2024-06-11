package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.parseHex
import com.swmansion.starknet.data.serializers.Uint64Serializer
import com.swmansion.starknet.data.types.conversions.ConvertibleToCalldata
import com.swmansion.starknet.extensions.toFelt
import com.swmansion.starknet.extensions.toHex
import kotlinx.serialization.Serializable
import java.math.BigInteger

@Serializable(with = Uint64Serializer::class)
data class Uint64(override val value: BigInteger) : NumAsHexBase(value), ConvertibleToCalldata {
    constructor(value: Long) : this(BigInteger.valueOf(value))
    constructor(value: Int) : this(BigInteger.valueOf(value.toLong()))

    init {
        if (value < BigInteger.ZERO) {
            throw IllegalArgumentException("Default Uint64 constructor does not accept negative numbers, [$value] given.")
        }
        if (value >= MAX) {
            throw IllegalArgumentException("Default Uint64 constructor does not accept numbers higher than 2^64-1, [$value] given.")
        }
    }

    override fun toCalldata() = listOf(this.value.toFelt)

    override fun toString() = "Uint64(${value.toHex()})"

    override fun hexString() = value.toHex()

    override fun decString(): String = value.toString(10)

    companion object {
        @field:JvmField
        val MAX = BigInteger.valueOf(2).pow(64).minus(BigInteger.ONE)

        @field:JvmField
        val ZERO = Uint64(BigInteger.ZERO)

        @field:JvmField
        val ONE = Uint64(BigInteger.ONE)

        /**
         * Create Uint64 from hex string. It must start with "0x" prefix.
         *
         * @param value hex string.
         */
        @JvmStatic
        fun fromHex(value: String): Uint64 {
            return Uint64(parseHex(value))
        }
    }
}
