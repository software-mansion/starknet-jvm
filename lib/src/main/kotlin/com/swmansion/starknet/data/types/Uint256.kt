package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.parseHex
import com.swmansion.starknet.data.types.conversions.ConvertibleToCalldata
import com.swmansion.starknet.extensions.toHex
import com.swmansion.starknet.extensions.toHexPadded
import java.math.BigInteger

private const val SHIFT = 128
private val SHIFT_MOD: BigInteger = BigInteger.valueOf(2).pow(128)

data class Uint256(override val value: BigInteger) : NumAsHexBase(value), ConvertibleToCalldata {
    constructor(value: Long) : this(BigInteger.valueOf(value))
    constructor(value: Int) : this(BigInteger.valueOf(value.toLong()))
    constructor(value: Felt) : this(value.value)
    constructor(low: BigInteger, high: BigInteger) : this(low.add(high.shiftLeft(SHIFT)))
    constructor(low: Felt, high: Felt) : this(low.value, high.value)

    init {
        if (value < BigInteger.ZERO) {
            throw IllegalArgumentException("Default Uint256 constructor does not accept negative numbers, [$value] given.")
        }
        if (value > MAX) {
            throw IllegalArgumentException("Default Uint256 constructor does not accept numbers higher than 2^256-1, [$value] given.")
        }
    }

    /**
     * Get low 128 bits of Uint256
     */
    val low: Felt
        get() = Felt(value.mod(SHIFT_MOD))

    /**
     * Get high 128 bits of Uint256
     */
    val high: Felt
        get() = Felt(value.shiftRight(SHIFT))

    override fun toCalldata() = listOf(low, high)

    override fun toString() = "Uint256($value)"

    override fun hexString() = value.toHex()

    override fun hexStringPadded() = value.toHexPadded()

    override fun decString(): String = value.toString(10)

    companion object {
        @field:JvmField
        val MAX = BigInteger.valueOf(2).pow(256).minus(BigInteger.ONE)

        @field:JvmField
        val ZERO = Uint256(BigInteger.ZERO)

        @field:JvmField
        val ONE = Uint256(BigInteger.ONE)

        @JvmStatic
        fun fromHex(value: String) = Uint256(parseHex(value))
    }
}
