@file:JvmName("ParseHex")

package com.swmansion.starknet.data

import java.math.BigInteger

@JvmSynthetic
internal fun parseHex(value: String): BigInteger {
    if (!value.startsWith("0x")) {
        throw IllegalArgumentException("Hex must start with 0x")
    }
    val result = value.removePrefix("0x").trimStart('0')

    if (result.isEmpty()) {
        return BigInteger.ZERO
    }
    return BigInteger(result, 16)
}
