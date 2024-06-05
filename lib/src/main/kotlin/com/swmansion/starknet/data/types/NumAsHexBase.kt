package com.swmansion.starknet.data.types

import java.math.BigInteger

sealed class NumAsHexBase(open val value: BigInteger) : Comparable<NumAsHexBase> {
    override fun compareTo(other: NumAsHexBase): Int {
        return value.compareTo(other.value)
    }

    /**
     * Encode as hexadecimal string, including "0x" prefix.
     */
    abstract fun hexString(): String

    /**
     * Encode as padded hexadecimal string, including "0x" prefix.
     */
    abstract fun hexStringPadded(): String

    /**
     * Encode as decimal string.
     */
    abstract fun decString(): String
}
