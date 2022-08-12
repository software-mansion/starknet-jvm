package com.swmansion.starknet.data.types

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

@Serializable
enum class BlockTag(val tag: String) {
    LATEST("latest"), PENDING("pending")
}

@Serializable(with = BlockIdSerializer::class)
sealed class BlockId() {
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

internal class BlockIdSerializer() : KSerializer<BlockId> {
    override fun deserialize(decoder: Decoder): BlockId {
        val value = decoder.decodeString()

        if (BlockTag.values().map { it.tag }.contains(value)) {
            val tag = BlockTag.valueOf(value)
            return BlockId.Tag(tag)
        }

        return BlockId.Hash(Felt.fromHex(value))
    }

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("BlockHashOrTag", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BlockId) {
        require(encoder is JsonEncoder)
        val element = when (value) {
            is BlockId.Tag -> encoder.json.encodeToJsonElement(value.toString())
            is BlockId.Hash -> buildJsonObject { put("block_hash", value.toString()) }
            is BlockId.Number -> buildJsonObject { put("block_number", value.blockNumber) }
        }
        encoder.encodeJsonElement(element)
    }
}
