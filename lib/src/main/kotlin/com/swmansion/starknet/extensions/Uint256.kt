@file:JvmName("UInt256")

package com.swmansion.starknet.extensions

import com.swmansion.starknet.data.types.Uint256
import java.math.BigInteger

internal val BigInteger.toUint256: Uint256
    get() = Uint256(this)
