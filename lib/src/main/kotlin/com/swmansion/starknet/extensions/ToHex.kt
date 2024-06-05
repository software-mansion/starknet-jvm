package com.swmansion.starknet.extensions

import java.math.BigInteger

@JvmSynthetic
internal fun BigInteger.toHex(): String = "0x" + this.toString(16)

@JvmSynthetic
internal fun BigInteger.toHexPadded(): String {
    val hexString = this.toString(16)
    val zeros = "0".repeat(64 - hexString.length)
    return "0x" + zeros + hexString
}
