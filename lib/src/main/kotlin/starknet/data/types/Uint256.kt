package starknet.data.types

import starknet.data.parseHex
import types.Felt
import java.math.BigInteger

private val MAX: BigInteger = BigInteger.valueOf(2).pow(256).minus(BigInteger.ONE)
private val SHIFT = 128
private val SHIFT_MOD: BigInteger = BigInteger.valueOf(2).pow(128)

data class Uint256(val value: BigInteger) {
    constructor(value: Long) : this(BigInteger.valueOf(value))
    constructor(value: Int) : this(BigInteger.valueOf(value.toLong()))
    constructor(value: Felt) : this(value.value)
    constructor(low: BigInteger, high: BigInteger) : this(low.add(high.shiftLeft(SHIFT)))
    constructor(low: Felt, high: Felt) : this(low.value, high.value)

    init {
        if (value < BigInteger.ZERO) {
            throw java.lang.IllegalArgumentException("Default Uint256 constructor does not accept negative numbers, [$value] given.")
        }
        if (value > MAX) {
            throw java.lang.IllegalArgumentException("Default Uint256 constructor does not accept numbers higher than 2^256-1, [$value] given.")
        }
    }

    val low: Felt
        get() = Felt(value.mod(SHIFT_MOD))

    val high: Felt
        get() = Felt(value.shiftRight(SHIFT))

    companion object {
        @field:JvmField
        val ZERO = Uint256(BigInteger.ZERO)

        @field:JvmField
        val ONE = Uint256(BigInteger.ONE)

        @JvmStatic
        fun fromHex(value: String) = Uint256(parseHex(value))
    }
}

val BigInteger.toUint256: Uint256
    get() = Uint256(this)