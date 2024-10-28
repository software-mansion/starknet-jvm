import com.swmansion.starknet.data.types.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

internal object MerkleNodePolymorphicSerializer :
    JsonContentPolymorphicSerializer<NodeHashToNodeMappingItem.MerkleNode>(NodeHashToNodeMappingItem.MerkleNode::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<NodeHashToNodeMappingItem.MerkleNode> {
        val jsonElement = element.jsonObject
        val binaryNodeKeys = NodeHashToNodeMappingItem.BinaryNode.serializer().descriptor.elementNames.toSet()
        val edgeNodeKeys = NodeHashToNodeMappingItem.EdgeNode.serializer().descriptor.elementNames.toSet()

        return when (jsonElement.keys) {
            binaryNodeKeys -> NodeHashToNodeMappingItem.BinaryNode.serializer()
            edgeNodeKeys -> NodeHashToNodeMappingItem.EdgeNode.serializer()
            else -> throw IllegalArgumentException("Invalid MerkleNode JSON object: $jsonElement")
        }
    }
}
