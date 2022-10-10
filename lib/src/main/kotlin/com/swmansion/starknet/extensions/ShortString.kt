package com.swmansion.starknet.extensions

import com.swmansion.starknet.data.types.Felt

internal fun String.isShortString(): Boolean {
    return this.length <= 31
}

internal fun String.isAsciiString(): Boolean {
    for (char in this) {
        if (char.code < 0 || char.code > 127) {
            return false
        }
    }

    return true
}

internal fun String.encodeShortString(): Felt {
    if (!this.isShortString()) {
        throw Error("Short string cannot be longer than 31 characters")
    }
    if (!this.isAsciiString()) {
        throw Error("String to be encoded must be an ascii string")
    }

    val encoded = this.replace(Regex(".")) {
            s -> s.value.first().code.toString(16).padStart(2, '0')
    }

    return Felt.fromHex(encoded.addHexPrefix())
}

internal fun Felt.decodeShortString(): String {
    var hexString = this.hexString().removeHexPrefix()

    if (hexString.length % 2 == 1) {
        hexString = hexString.padStart(hexString.length + 1, '0')
    }

    val decoded = hexString.replace(Regex(".{2}")) { hex ->
        hex.value.toInt(16).toChar().toString()
    }

    return decoded
}