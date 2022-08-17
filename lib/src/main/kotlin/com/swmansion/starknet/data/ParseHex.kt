@file:JvmName("ParseHex")

package com.swmansion.starknet.data

import java.math.BigInteger

internal fun parseHex(value: String): BigInteger {
    if (!value.startsWith("0x")) {
        throw IllegalArgumentException("Hex must start with 0x")
    }
    return BigInteger(value.removePrefix("0x"), 16)
}
