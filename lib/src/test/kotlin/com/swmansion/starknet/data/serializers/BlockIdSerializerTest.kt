import com.swmansion.starknet.data.serializers.BlockIdSerializer
import com.swmansion.starknet.data.types.BlockId
import com.swmansion.starknet.data.types.BlockTag
import com.swmansion.starknet.data.types.Felt
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BlockIdSerializerTest {
    @Test
    fun `deserialize Block Hash`() {
        val inputJson = "\"0xabc123\""
        val result = Json.decodeFromString(BlockIdSerializer, inputJson)
        val expected = Felt.fromHex("0xabc123")
        assertTrue(result is BlockId.Hash)
        assertEquals(expected, (result as BlockId.Hash).blockHash)
    }

    @Test
    fun `deserialize Block Number`() {
        val inputJson = "\"123\""
        val result = Json.decodeFromString(BlockIdSerializer, inputJson)
        assertTrue(result is BlockId.Number)
        assertEquals(123, (result as BlockId.Number).blockNumber)
    }

    @Test
    fun `deserialize Block Tag`() {
        val inputJson1 = "\"latest\""
        val inputJson2 = "\"pending\""
        val result1 = Json.decodeFromString(BlockIdSerializer, inputJson1)
        val result2 = Json.decodeFromString(BlockIdSerializer, inputJson2)
        assertTrue(result1 is BlockId.Tag)
        assertTrue(result2 is BlockId.Tag)
        assertEquals(BlockTag.LATEST, (result1 as BlockId.Tag).blockTag)
        assertEquals(BlockTag.PENDING, (result2 as BlockId.Tag).blockTag)
    }

    @Test
    fun `deserialize nonexistent BlockId`() {
        val inputJson = "\"nonexistentBlockTag\""
        assertThrows<IllegalArgumentException> { Json.decodeFromString(BlockIdSerializer, inputJson) }
    }
}
