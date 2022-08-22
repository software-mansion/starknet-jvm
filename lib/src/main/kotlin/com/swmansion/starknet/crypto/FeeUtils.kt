package com.swmansion.starknet.crypto

import kotlin.math.roundToInt

object FeeUtils {
    @JvmStatic
    fun estimatedFeeToMaxFee(fee: Long, overhead: Double = 0.5): Long {
        val overheadPercent = ((1 + overhead) * 100).roundToInt()

        return fee * overheadPercent
    }
}
