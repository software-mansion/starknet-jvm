package starknet.extensions

import com.swmansion.starknet.data.types.BlockId
import com.swmansion.starknet.data.types.BlockTag
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.extensions.toGatewayParam
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ToParamTest {
    @Test
    fun `block hash to param`() {
        val blockId = BlockId.Hash(Felt.fromHex("0x1234"))

        assertEquals(Pair("blockHash", "0x1234"), blockId.toGatewayParam())
    }

    @Test
    fun `block number to param`() {
        val blockId = BlockId.Number(1234)

        assertEquals(Pair("blockNumber", "1234"), blockId.toGatewayParam())
    }

    @Test
    fun `block tag latest to param`() {
        val blockId = BlockId.Tag(BlockTag.LATEST)

        assertEquals(Pair("blockNumber", "latest"), blockId.toGatewayParam())
    }

    @Test
    fun `block tag pending to param`() {
        val blockId = BlockId.Tag(BlockTag.PENDING)

        assertEquals(Pair("blockNumber", "pending"), blockId.toGatewayParam())
    }
}
