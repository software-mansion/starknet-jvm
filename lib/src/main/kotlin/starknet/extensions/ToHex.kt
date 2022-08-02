package starknet.extensions

import java.math.BigInteger

fun BigInteger.toHex(): String = "0x" + this.toString(16)
fun Int.toHex(): String = "0x" + this.toString(16)
