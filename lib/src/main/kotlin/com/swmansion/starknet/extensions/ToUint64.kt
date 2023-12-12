package com.swmansion.starknet.extensions

import com.swmansion.starknet.data.types.NumAsHexBase
import com.swmansion.starknet.data.types.Uint64
import java.math.BigInteger

@get:JvmSynthetic
val BigInteger.toUint64: Uint64
    get() = Uint64(this)

@get:JvmSynthetic
val Int.toUint64: Uint64
    get() = Uint64(this)

@get:JvmSynthetic
val Long.toUint64: Uint64
    get() = Uint64(this)

@get:JvmSynthetic
val NumAsHexBase.toUint64: Uint64
    get() = Uint64(this.value)
