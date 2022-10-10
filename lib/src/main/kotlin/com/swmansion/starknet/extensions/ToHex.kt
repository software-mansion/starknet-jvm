package com.swmansion.starknet.extensions

import java.math.BigInteger

@JvmSynthetic
internal fun BigInteger.toHex(): String = "0x" + this.toString(16)

@JvmSynthetic
internal fun String.addHexPrefix(): String {
    return "0x${this.removeHexPrefix()}"
}

@JvmSynthetic
internal fun String.removeHexPrefix(): String {
    return this.removePrefix("0x")
}