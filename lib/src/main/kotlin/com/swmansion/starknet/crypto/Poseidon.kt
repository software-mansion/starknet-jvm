package com.swmansion.starknet.crypto

import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.extensions.toFelt
import java.math.BigInteger

/**
 * Split a BigInteger into an array of 4 Long values, each representing a 64-bit chunk of the original BigInteger.
 *
 * @param bigInt the BigInteger to split
 * @return a LongArray containing the 64-bit chunks of the original BigInteger
 */
private fun splitBigInteger(bigInt: BigInteger): LongArray {
    val result = LongArray(4)

    // if the input BigInteger is zero, return an array of zeros
    if (bigInt == BigInteger.ZERO) {
        return result
    }
    // mask has all bits set to 1 except the least significant one
    val mask = BigInteger.valueOf(2).pow(64).subtract(BigInteger.ONE)

    // loop through the 64-bit chunks of the BigInteger, shift them and store in the LongArray
    for (i in 0 until 4) {
        result[i] = bigInt.shiftRight(i * 64).and(mask).toLong()
    }

    return result
}

/**
 * Convert a LongArray to an Array of BigIntegers.
 *
 * @param longArray the LongArray to convert
 * @return an Array of BigIntegers corresponding to the input LongArray
 */
@OptIn(ExperimentalUnsignedTypes::class)
private fun toBigIntegerArray(longArray: LongArray): Array<BigInteger> {
    val ulongArray = longArray.map { it.toULong() }.toULongArray()
    return ulongArray.map { BigInteger(it.toString()) }.toTypedArray()
}

/**
 * Combine a LongArray of 64-bit chunks into a single BigInteger.
 *
 * @param arr the LongArray containing the 64-bit chunks
 * @return a BigInteger representing the combined value of the input LongArray
 */
private fun unsplitBigInteger(arr: LongArray): BigInteger {
    val powersOfTwo = listOf(
        BigInteger.valueOf(2).pow(0),
        BigInteger.valueOf(2).pow(64),
        BigInteger.valueOf(2).pow(128),
        BigInteger.valueOf(2).pow(192),
    )
    val barr = toBigIntegerArray(arr)
    // w * 2**0 + x * 2**64 + y * 2**128 + z * 2**192
    return barr.zip(powersOfTwo).fold(BigInteger.ZERO) { acc, (b, p) -> acc + b * p }
}

/**
 * Starknet poseidon utilities.
 *
 * Class with utility methods related to starknet poseidon hash calculation.
 */
object Poseidon {

    private val m = 3
    private val r = 2

    init {
        NativeLoader.load("poseidon_jni")
    }

    /**
     * Native hades permutation.
     */
    @JvmStatic
    private external fun hades(
        values: Array<LongArray>,
    ): Array<LongArray>

    /**
     * Compute poseidon hash on single Felt.
     *
     * @param x single Felt
     */
    @JvmStatic
    fun poseidonHash(x: Felt): Felt {
        return unsplitBigInteger(
            hades(
                arrayOf(
                    splitBigInteger(BigInteger(x.decString())),
                    longArrayOf(0, 0, 0, 0),
                    longArrayOf(1, 0, 0, 0),
                ),
            )[0],
        ).toFelt
    }

    /**
     * Compute poseidon hash on two Felts.
     *
     * @param x Felt
     * @param y Felt
     */
    @JvmStatic
    fun poseidonHash(x: Felt, y: Felt): Felt {
        return unsplitBigInteger(
            hades(
                arrayOf(
                    splitBigInteger(BigInteger(x.decString())),
                    splitBigInteger(BigInteger(y.decString())),
                    longArrayOf(2, 0, 0, 0),
                ),
            )[0],
        ).toFelt
    }

    /**
     * Compute poseidon hash on many Felts.
     *
     * @param values List of Felts
     */
    @JvmStatic
    fun poseidonHash(values: List<Felt>): Felt {
        val inputValues = values.toMutableList().apply {
            add(Felt.ONE)
            if (size % r == 1) add(Felt.ZERO)
        }

        var state = Array(m) {longArrayOf(0, 0, 0, 0)}

        for (iter in 0 until inputValues.size step 2) {
            state = hades(
                arrayOf(
                    splitBigInteger(unsplitBigInteger(state[0]) + BigInteger(inputValues[iter].decString())),
                    splitBigInteger(unsplitBigInteger(state[1]) + BigInteger(inputValues[iter + 1].decString())),
                    state[2],
                ),
            )
        }

        return unsplitBigInteger(state[0]).toFelt
    }

    /**
     * Compute poseidon hash on variable number of arguments.
     *
     * @param values any number of Felts
     */
    @JvmStatic
    fun poseidonHash(vararg values: Felt): Felt = poseidonHash(values.asList())
}
