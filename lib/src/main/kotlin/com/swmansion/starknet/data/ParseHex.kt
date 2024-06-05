@file:JvmName("ParseHex")

package com.swmansion.starknet.data

import java.math.BigInteger

@JvmSynthetic
internal fun parseHex(value: String): BigInteger {
    if (!value.startsWith("0x")) {
        throw IllegalArgumentException("Hex must start with 0x")
    }

    val trimmedValue = value.removePrefix("0x").trimStart('0')
    if (trimmedValue.isEmpty()) {
        return BigInteger.ZERO
    }
    return BigInteger(trimmedValue, 16)
}
