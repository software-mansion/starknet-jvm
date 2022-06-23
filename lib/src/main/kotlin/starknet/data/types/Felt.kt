package types

import starknet.crypto.PRIME
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

    companion object {
        @field:JvmField
        val ZERO = Felt(BigInteger.ZERO)

        @field:JvmField
        val ONE = Felt(BigInteger.ONE)

        @JvmStatic
        fun fromHex(value: String): Felt {
            if (!value.startsWith("0x")) {
                throw IllegalArgumentException("Hex must start with 0x")
            }
            return Felt(BigInteger(value.removePrefix("0x"), 16))
        }
    }
}

val BigInteger.toFelt: Felt
    get() = Felt(this)