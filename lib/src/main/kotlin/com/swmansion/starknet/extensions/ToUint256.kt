package com.swmansion.starknet.extensions

import com.swmansion.starknet.data.types.NumAsHexBase
import com.swmansion.starknet.data.types.Uint256
import java.math.BigInteger

@get:JvmSynthetic
val BigInteger.toUint256: Uint256
    get() = Uint256(this)

@get:JvmSynthetic
val String.toUint256: Uint256
    get() = Uint256.fromHex(this)

@get:JvmSynthetic
val Int.toUint256: Uint256
    get() = Uint256(this)

@get:JvmSynthetic
val Long.toUint256: Uint256
    get() = Uint256(this)

@get:JvmSynthetic
val NumAsHexBase.toUint256: Uint256
    get() = Uint256(this.value)
