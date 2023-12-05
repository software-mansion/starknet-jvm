package com.swmansion.starknet.extensions

import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.NumAsHexBase
import java.math.BigInteger

@get:JvmSynthetic
val BigInteger.toFelt: Felt
    get() = Felt(this)

@get:JvmSynthetic
val String.toFelt: Felt
    get() = Felt.fromHex(this)

@get:JvmSynthetic
val Int.toFelt: Felt
    get() = Felt(this)

@get:JvmSynthetic
val Long.toFelt: Felt
    get() = Felt(this)

@get:JvmSynthetic
val NumAsHexBase.toFelt: Felt
    get() = Felt(this.value)
