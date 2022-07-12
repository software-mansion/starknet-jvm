package starknet.provider

import org.junit.Test
import starknet.data.types.BlockTag
import starknet.data.types.Call
import starknet.data.types.Felt
import starknet.data.types.StarknetChainId
import starknet.provider.gateway.GatewayProvider
import kotlin.test.assertEquals

class GatewayTest {
    @Test
    fun callContractTest() {
        val provider = GatewayProvider("http://127.0.0.1:5050/feederGateway/", "http://127.0.0.1:5050/gateway", StarknetChainId.TESTNET)

        val call = Call(Felt.fromHex("0x055ce9b2379adaf3820d4877e7162a6e5bd8ac81cf9d00f026973b8647fbb615"), "get_balance", emptyList())

        val request = provider.callContract(call, BlockTag.PENDING)

//        val response = request.send()
//
//        assertEquals(response.result, listOf(Felt(0)))
    }
}