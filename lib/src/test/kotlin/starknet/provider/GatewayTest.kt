package starknet.provider

import org.junit.Test
import starknet.data.selectorFromName
import starknet.data.types.*
import starknet.provider.gateway.GatewayProvider
import kotlin.test.assertEquals

class GatewayTest {
    private fun getProvider(): Provider {
        return GatewayProvider(
            "http://127.0.0.1:5050/feeder_gateway/",
            "http://127.0.0.1:5050/gateway",
            StarknetChainId.TESTNET
        )
    }

    @Test
    fun callContractTest() {
        val provider = getProvider()

        val call = Call(
            Felt.fromHex("0x055ce9b2379adaf3820d4877e7162a6e5bd8ac81cf9d00f026973b8647fbb615"),
            "get_balance",
            emptyList()
        )

        val request = provider.callContract(call, BlockTag.PENDING)

        val response = request.send()

        assertEquals(listOf(Felt(0)), response.result)
    }

    @Test
    fun getStorageAtTest() {
        val provider = getProvider()

        val request = provider.getStorageAt(
            Felt.fromHex("0x055ce9b2379adaf3820d4877e7162a6e5bd8ac81cf9d00f026973b8647fbb615"),
            selectorFromName("balance"),
            BlockTag.PENDING
        )

        val response = request.send()

        assertEquals(Felt(0), response)
    }

    @Test
    fun invokeTransactionTest() {
        val provider = getProvider()

        val call = Call(
            Felt.fromHex("0x055ce9b2379adaf3820d4877e7162a6e5bd8ac81cf9d00f026973b8647fbb615"),
            selectorFromName("increase_balance"),
            listOf(Felt(10))
        )

        val dummySig = listOf(Felt(0), Felt(0), Felt(0), Felt(0), Felt(0))

        val payload = InvokeFunctionPayload(call, dummySig, Felt(0), null)

        val request = provider.invokeFunction(payload)

        request.send()
    }
}