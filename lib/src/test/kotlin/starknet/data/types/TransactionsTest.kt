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
    fun getHash() {
        val tx1 = InvokeTransactionV1(
            senderAddress = Felt.fromHex("0x2a"),
            calldata = listOf(),
            chainId = StarknetChainId.fromNetworkName("SN_GOERLI"),
            nonce = Felt.ZERO,
            maxFee = Felt.ZERO,
            version = TransactionVersion.V1,
        )

        assertEquals(Felt.fromHex("0x22294fe217f962c39e4cb694a5db3f71e1132988451a9b2abc2d2ea8512088e"), tx1.hash)

        val tx2 = InvokeTransactionV1(
            senderAddress = Felt(
                BigInteger("468485892896389608042320470922610020674017592380673471682128582128678525733"),
            ),
            calldata = listOf(
                Felt(BigInteger("1")),
                Felt(
                    BigInteger("468485892896389608042320470922610020674017592380673471682128582128678525733"),
                ),
                Felt(
                    BigInteger("1307260637166823203998179679098545329314629630090003875272134084395659334905"),
                ),
                Felt(BigInteger("0")),
                Felt(BigInteger("1")),
                Felt(BigInteger("1")),
                Felt(
                    BigInteger("807694187056572246556317413263910754299517324162860342603752464651582167489"),
                ),
                Felt(
                    BigInteger("2"),
                ),
            ),
            chainId = StarknetChainId.fromNetworkName("SN_GOERLI"),
            nonce = Felt.ZERO,
            maxFee = Felt(BigInteger("100000000")),
            version = TransactionVersion.V1,
        )

        assertEquals(Felt.fromHex("0xff63c84949d3ab0ced753c227528493dea3dc4680c65c1facb7f86ae0472df"), tx2.hash)
    }

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
