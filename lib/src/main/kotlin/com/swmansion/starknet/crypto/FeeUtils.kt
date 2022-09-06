package com.swmansion.starknet.crypto

import com.swmansion.starknet.data.types.Felt
import java.math.BigInteger
import kotlin.math.roundToInt

internal object FeeUtils {
    @JvmStatic
    fun estimatedFeeToMaxFee(fee: Felt, overhead: Double = 0.5): Felt {
        val multiplier = ((1 + overhead) * 100).roundToInt().toBigInteger()
        val result = fee.value.multiply(multiplier).divide(BigInteger.valueOf(100))

        return Felt(result)
    }
}
