package com.swmansion.starknet.extensions

import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.NumAsHex

@get:JvmSynthetic
val String.toFelt: Felt
    get() = Felt.fromHex(this)

@get:JvmSynthetic
val String.toNumAsHex: NumAsHex
    get() = NumAsHex.fromHex(this)
