@file:JvmName("Fee")

package com.swmansion.starknet.crypto

import kotlin.math.roundToInt

fun estimatedFeeToMaxFee(fee: Long, overhead: Double = 0.5): Long {
    val overheadPercent = ((1 + overhead) * 100).roundToInt()

    return fee * overheadPercent
}
