package starknet.data.types

import com.swmansion.starknet.data.serializers.BlockIdSerializer
import com.swmansion.starknet.data.types.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigInteger

internal class TransactionsTest {
    @Test
    fun `serialize blockId with hash`() {
        val json = Json.encodeToJsonElement(BlockIdSerializer, BlockId.Hash(Felt.fromHex("0x859")))
        assertEquals("{\"block_hash\":\"0x859\"}", json.toString())
    }

    @Test
    fun `serialize blockId with number`() {
        val json = Json.encodeToJsonElement(BlockIdSerializer, BlockId.Number(20))
        assertEquals("{\"block_number\":20}", json.toString())
    }

    @Test
    fun `serialize blockId with tag`() {
        val json = Json.encodeToJsonElement(BlockIdSerializer, BlockId.Tag(BlockTag.LATEST))
        assertEquals("\"latest\"", json.toString())
    }

    @Test
    fun `serialize class with blockId number`() {
        @Serializable
        data class MyClass(
            @SerialName("block_id")
            val blockId: BlockId,
        )

        val myClassInstance = MyClass(BlockId.Number(20))
        val json = Json.encodeToJsonElement(MyClass.serializer(), myClassInstance)
        assertEquals("{\"block_id\":{\"block_number\":20}}", json.toString())
    }

    @Test
    fun `serialize class with blockId hash`() {
        @Serializable
        data class MyClass(
            @SerialName("block_id")
            val blockId: BlockId,
        )

        val myClassInstance = MyClass(BlockId.Hash(Felt.fromHex("0x1")))
        val json = Json.encodeToJsonElement(MyClass.serializer(), myClassInstance)
        assertEquals("{\"block_id\":{\"block_hash\":\"0x1\"}}", json.toString())
    }

    @Test
    fun `serialize class with blockId tag`() {
        @Serializable
        data class MyClass(
            @SerialName("block_id")
            val blockId: BlockId,
        )

        val myClassInstance = MyClass(BlockId.Tag(BlockTag.LATEST))
        val json = Json.encodeToJsonElement(MyClass.serializer(), myClassInstance)
        assertEquals("{\"block_id\":\"latest\"}", json.toString())
    }
}
