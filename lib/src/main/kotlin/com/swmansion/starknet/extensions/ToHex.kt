@file:JvmName("ToHex")

package com.swmansion.starknet.extensions

import java.math.BigInteger

internal fun BigInteger.toHex(): String = "0x" + this.toString(16)

internal fun Int.toHex(): String = "0x" + this.toString(16)
