package com.swmansion.starknet.crypto

import com.swmansion.starknet.data.types.Felt
import java.math.BigInteger
import kotlin.math.roundToInt

@JvmSynthetic
internal fun estimatedFeeToMaxFee(fee: Felt, overhead: Double = 0.1): Felt {
    val multiplier = ((1 + overhead) * 100).roundToInt().toBigInteger()
    val result = fee.value.multiply(multiplier).divide(BigInteger.valueOf(100))

    return Felt(result)
}
