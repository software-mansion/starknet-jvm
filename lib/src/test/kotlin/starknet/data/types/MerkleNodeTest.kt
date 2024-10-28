package starknet.data.types

import com.swmansion.starknet.data.types.NodeHashToNodeMappingItem
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class MerkleNodeTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `binary node`() {
        val jsonString = """
            {
               "left": "0x1",
               "right": "0x2"
            }
        """.trimIndent()

        val node = json.decodeFromString(NodeHashToNodeMappingItem.MerkleNode.serializer(), jsonString)

        assertTrue(node is NodeHashToNodeMappingItem.BinaryNode)
    }

    @Test
    fun `binary node with missing field`() {
        val jsonString = """
            {
               "left": "0x1"
            }
        """.trimIndent()

        assertThrows<IllegalArgumentException>("Invalid MerkleNode JSON object: {\"left\":\"0x1\"}") {
            json.decodeFromString<NodeHashToNodeMappingItem.BinaryNode>(jsonString)
        }
    }

    @Test
    fun `edge node`() {
        val jsonString = """
            {
               "path": 10,
               "length": 20,
               "child": "0x123"
            }
        """.trimIndent()

        val node = json.decodeFromString(NodeHashToNodeMappingItem.MerkleNode.serializer(), jsonString)
        assertTrue(node is NodeHashToNodeMappingItem.EdgeNode)
    }

    @Test
    fun `edge node with missing fields`() {
        val jsonString = """
            {
               "path": 10,
               "length": 20
            }
        """.trimIndent()

        assertThrows<IllegalArgumentException>("Invalid MerkleNode JSON object: {\"path\":10,\"length\":20}") {
            json.decodeFromString<NodeHashToNodeMappingItem.MerkleNode>(jsonString)
        }
    }

    @Test
    fun `node with mixed fields`() {
        val jsonString = """
            {
               "path": 10,
               "length": 20,
               "right": "0x123"
            }
        """.trimIndent()

        assertThrows<IllegalArgumentException>("Invalid MerkleNode JSON object: {\"path\":10,\"length\":20,\"right\":\"0x123\"}") {
            json.decodeFromString<NodeHashToNodeMappingItem.MerkleNode>(jsonString)
        }
    }
}
