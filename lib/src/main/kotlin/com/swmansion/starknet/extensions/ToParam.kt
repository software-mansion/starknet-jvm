package com.swmansion.starknet.extensions

import com.swmansion.starknet.data.types.BlockId

@JvmSynthetic
internal fun BlockId.toParam(): Pair<String, String> = when (this) {
    is BlockId.Hash -> Pair("blockHash", this.blockHash.hexString())
    is BlockId.Number -> Pair("blockNumber", this.blockNumber.toString())
    is BlockId.Tag -> Pair("blockNumber", this.blockTag.tag)
}
