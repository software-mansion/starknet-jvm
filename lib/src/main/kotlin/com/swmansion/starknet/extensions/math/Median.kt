package com.swmansion.starknet.extensions.math

import com.swmansion.starknet.data.types.Uint64
import java.math.BigInteger

/**
 * Calculate the median of a list of Uint64 values.
 *
 */
fun List<Uint64>.median(): Uint64 {
    require(isNotEmpty()) { "Cannot calculate median of an empty list" }

    val sorted = this.sorted()
    println("SORTED: ${sorted.map { it.value }}")
    val mid = sorted.size / 2
    return if (sorted.size % 2 == 1) {
        sorted[mid]
    } else {
        Uint64((sorted[mid - 1].value + sorted[mid].value) / BigInteger.valueOf(2))
    }
}
