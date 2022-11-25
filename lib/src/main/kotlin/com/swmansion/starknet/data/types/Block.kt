package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.serializers.BlockIdSerializer
import kotlinx.serialization.Serializable

@Serializable
enum class BlockTag(val tag: String) {
    LATEST("latest"), PENDING("pending")
}

@Serializable(with = BlockIdSerializer::class)
sealed class BlockId {
    data class Hash(
        val blockHash: Felt,
    ) : BlockId() {
        override fun toString(): String {
            return blockHash.hexString()
        }
    }

    data class Number(
        val blockNumber: Int,
    ) : BlockId()

    data class Tag(
        val blockTag: BlockTag,
    ) : BlockId() {
        override fun toString(): String {
            return blockTag.tag
        }
    }
}

