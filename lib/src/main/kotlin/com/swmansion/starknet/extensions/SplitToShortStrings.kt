package com.swmansion.starknet.extensions

@JvmSynthetic
internal fun String.splitToShortStrings(): List<String> {
    val maxLen = 31
    return (indices step maxLen).map {
        substring(it, (it + maxLen).coerceAtMost(length))
    }
}
