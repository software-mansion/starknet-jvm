package com.swmansion.starknet.extensions

import com.swmansion.starknet.data.types.Felt
import java.math.BigInteger

@get:JvmSynthetic
val BigInteger.toFelt: Felt
    get() = Felt(this)
