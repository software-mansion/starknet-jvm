package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.parseHex
import com.swmansion.starknet.data.serializers.NumAsHexSerializer
import com.swmansion.starknet.extensions.toHex
import com.swmansion.starknet.extensions.toHexPadded
import kotlinx.serialization.Serializable
import java.math.BigInteger

@Serializable(with = NumAsHexSerializer::class)
data class NumAsHex(override val value: BigInteger) : NumAsHexBase(value) {
    constructor(value: Long) : this(BigInteger.valueOf(value))
    constructor(value: Int) : this(BigInteger.valueOf(value.toLong()))

    init {
        if (value < BigInteger.ZERO) {
            throw IllegalArgumentException("Default NumAsHex constructor does not accept negative numbers, [$value] given.")
        }
    }

    override fun toString() = "NumAsHex(${value.toHex()})"

    override fun hexString() = value.toHex()

    override fun hexStringPadded() = value.toHexPadded()

    override fun decString(): String = value.toString(10)

    companion object {
        @field:JvmField
        val ZERO = NumAsHex(BigInteger.ZERO)

        @field:JvmField
        val ONE = NumAsHex(BigInteger.ONE)

        /**
         * Create NumAsHex from hex string. It must start with "0x" prefix.
         *
         * @param value hex string.
         */
        @JvmStatic
        fun fromHex(value: String): NumAsHex = NumAsHex(parseHex(value))
    }
}
