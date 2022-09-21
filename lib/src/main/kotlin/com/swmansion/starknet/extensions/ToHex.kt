@file:JvmName("ToHex")

package com.swmansion.starknet.extensions

import java.math.BigInteger

@JvmSynthetic
internal fun BigInteger.toHex(): String = "0x" + this.toString(16)
