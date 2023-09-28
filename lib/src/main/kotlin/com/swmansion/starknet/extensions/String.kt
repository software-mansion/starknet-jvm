package com.swmansion.starknet.extensions

import com.swmansion.starknet.data.types.Felt

@get:JvmSynthetic
val String.toFelt: Felt
    get() = Felt.fromHex(this)
