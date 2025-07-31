package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

internal object MerkleNodePolymorphicSerializer :
    JsonContentPolymorphicSerializer<NodeHashToNodeMappingItem.MerkleNode>(NodeHashToNodeMappingItem.MerkleNode::class) {
    @OptIn(ExperimentalSerializationApi::class)
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<NodeHashToNodeMappingItem.MerkleNode> {
        val jsonElement = element.jsonObject
        val binaryNodeKeys = NodeHashToNodeMappingItem.BinaryNode.serializer().descriptor.elementNames.toSet()
        val edgeNodeKeys = NodeHashToNodeMappingItem.EdgeNode.serializer().descriptor.elementNames.toSet()

        val binaryMatch = jsonElement.keys.intersect(binaryNodeKeys).isNotEmpty()
        val edgeMatch = jsonElement.keys.intersect(edgeNodeKeys).isNotEmpty()

        return when {
            binaryMatch && !edgeMatch -> NodeHashToNodeMappingItem.BinaryNode.serializer()
            edgeMatch && !binaryMatch -> NodeHashToNodeMappingItem.EdgeNode.serializer()
            binaryMatch && edgeMatch -> throw IllegalArgumentException("Ambiguous MerkleNode JSON object: $jsonElement")
            else -> throw IllegalArgumentException("Invalid MerkleNode JSON object: missing identifying keys in $jsonElement")
        }
    }
}
