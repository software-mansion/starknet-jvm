package starknet.data.types

import starknet.data.parseHex
import starknet.data.toHex
import java.math.BigInteger

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
        return "Felt(${toHex(value)})"
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

val BigInteger.toFelt: Felt
    get() = Felt(this)