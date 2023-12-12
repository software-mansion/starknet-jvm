package com.swmansion.starknet.extensions

import com.swmansion.starknet.data.types.NumAsHexBase
import com.swmansion.starknet.data.types.Uint128
import java.math.BigInteger

@get:JvmSynthetic
val BigInteger.toUint128: Uint128
    get() = Uint128(this)

@get:JvmSynthetic
val Int.toUint128: Uint128
    get() = Uint128(this)

@get:JvmSynthetic
val Long.toUint128: Uint128
    get() = Uint128(this)

@get:JvmSynthetic
val NumAsHexBase.toUint128: Uint128
    get() = Uint128(this.value)
