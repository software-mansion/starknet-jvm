package com.swmansion.starknet.data.types

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
enum class BlockTag(val tag: String) {
    LATEST("latest"), PENDING("pending")
}

@Serializable(with = BlockHashOrTagSerializer::class)
sealed class BlockHashOrTag() {
    data class Hash(
        val blockHash: Felt,
    ) : BlockHashOrTag() {
        override fun string(): String {
            return blockHash.hexString()
        }
    }

    data class Tag(
        val blockTag: BlockTag,
    ) : BlockHashOrTag() {
        override fun string(): String {
            return blockTag.tag
        }
    }

    abstract fun string(): String
}

class BlockHashOrTagSerializer() : KSerializer<BlockHashOrTag> {
    override fun deserialize(decoder: Decoder): BlockHashOrTag {
        val value = decoder.decodeString()

        if (BlockTag.values().map { it.tag }.contains(value)) {
            val tag = BlockTag.valueOf(value)
            return BlockHashOrTag.Tag(tag)
        }

        return BlockHashOrTag.Hash(Felt.fromHex(value))
    }

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("BlockHashOrTag", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BlockHashOrTag) {
        encoder.encodeString(value.string())
    }
}
