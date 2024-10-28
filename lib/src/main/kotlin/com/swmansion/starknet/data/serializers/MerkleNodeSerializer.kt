package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.NodeHashToNodeMappingItem
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.jsonObject

internal object MerkleNodeSerializer : KSerializer<NodeHashToNodeMappingItem.MerkleNode> {
    override val descriptor = NodeHashToNodeMappingItem.MerkleNode.serializer().descriptor

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): NodeHashToNodeMappingItem.MerkleNode {
        require(decoder is JsonDecoder)
        val element = decoder.decodeJsonElement()
        val jsonElement = element.jsonObject

        val binaryNodeKeys = NodeHashToNodeMappingItem.BinaryNode.serializer().descriptor.elementNames.toSet()
        val edgeNodeKeys = NodeHashToNodeMappingItem.EdgeNode.serializer().descriptor.elementNames.toSet()

        return when (jsonElement.keys) {
            binaryNodeKeys -> {
                decoder.json.decodeFromJsonElement(NodeHashToNodeMappingItem.BinaryNode.serializer(), element)
            }
            edgeNodeKeys -> {
                decoder.json.decodeFromJsonElement(NodeHashToNodeMappingItem.EdgeNode.serializer(), element)
            }
            else -> throw IllegalArgumentException("Invalid MerkleNode JSON object: $jsonElement")
        }
    }

    override fun serialize(encoder: Encoder, value: NodeHashToNodeMappingItem.MerkleNode) {
        require(encoder is JsonEncoder)

        val jsonObject = when (value) {
            is NodeHashToNodeMappingItem.BinaryNode -> encoder.json.encodeToJsonElement(NodeHashToNodeMappingItem.BinaryNode.serializer(), value).jsonObject
            is NodeHashToNodeMappingItem.EdgeNode -> encoder.json.encodeToJsonElement(NodeHashToNodeMappingItem.EdgeNode.serializer(), value).jsonObject
        }
        encoder.encodeJsonElement(jsonObject)
    }
}
