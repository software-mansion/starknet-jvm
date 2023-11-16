package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.parseHex
import com.swmansion.starknet.data.serializers.NumAsHexSerializer
import com.swmansion.starknet.extensions.toHex
import kotlinx.serialization.Serializable
import java.math.BigInteger

sealed class NumAsHexBase(open val value: BigInteger) : Comparable<NumAsHexBase> {
    override fun compareTo(other: NumAsHexBase): Int {
        return value.compareTo(other.value)
    }

    abstract fun hexString(): String
    abstract fun decString(): String
}

@Serializable(with = NumAsHexSerializer::class)
data class NumAsHex(override val value: BigInteger) : NumAsHexBase(value) {
    constructor(value: Long) : this(BigInteger.valueOf(value))
    constructor(value: Int) : this(BigInteger.valueOf(value.toLong()))

    init {
        if (value < BigInteger.ZERO) {
            throw IllegalArgumentException("Default NumAsHex constructor does not accept negative numbers, [$value] given.")
        }
    }

    override fun toString(): String {
        return "NumAsHex(${value.toHex()})"
    }

    /**
     * Encode as hexadecimal string, including "0x" prefix.
     */
    override fun hexString(): String {
        return value.toHex()
    }

    /**
     * Encode as decimal string.
     */
    override fun decString(): String {
        return value.toString(10)
    }

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

        /**
         * Create NumAsHex from Felt.
         *
         * @param value Felt.
         */
        @JvmStatic
        fun fromFelt(value: Felt): NumAsHex = NumAsHex(value.value)
    }
}
