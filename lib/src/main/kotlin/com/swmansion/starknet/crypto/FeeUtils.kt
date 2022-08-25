package com.swmansion.starknet.crypto

import kotlin.math.roundToLong

object FeeUtils {
    @JvmStatic
    fun estimatedFeeToMaxFee(fee: Long, overhead: Double = 0.5): Long {
        return ((1 + overhead) * fee).roundToLong()
    }
}
