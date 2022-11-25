package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.BlockId
import com.swmansion.starknet.data.types.BlockTag
import com.swmansion.starknet.data.types.Felt
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put

internal object BlockIdSerializer : KSerializer<BlockId> {
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
