@file:JvmName("Hex")

package starknet.data

import java.math.BigInteger

fun parseHex(value: String): BigInteger {
    if (!value.startsWith("0x")) {
        throw IllegalArgumentException("Hex must start with 0x")
    }
    return BigInteger(value.removePrefix("0x"), 16)
}

fun BigInteger.toHex(): String = "0x" + this.toString(16)
fun Int.toHex(): String = "0x" + this.toString(16)
