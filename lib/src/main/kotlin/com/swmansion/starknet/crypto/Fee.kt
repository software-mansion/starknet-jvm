@file:JvmName("Fee")

package com.swmansion.starknet.crypto

import com.swmansion.starknet.data.types.Felt
import kotlin.math.roundToInt

fun estimatedFeeToMaxFee(fee: Felt, overhead: Double = 0.5): Felt {
    val overheadPercent = ((1 + overhead) * 100).roundToInt().toBigInteger()

    return Felt(fee.value.multiply(overheadPercent))
}
