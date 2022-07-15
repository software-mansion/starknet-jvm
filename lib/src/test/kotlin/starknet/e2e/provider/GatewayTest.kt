package starknet.e2e.provider

import org.junit.BeforeClass
import org.junit.Test
import starknet.data.selectorFromName
import starknet.data.types.*
import starknet.e2e.utils.DevnetClient
import starknet.provider.Provider
import starknet.provider.gateway.GatewayProvider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GatewayTest {
    private val devnetClient = DevnetClient()

    private var contractAddress: Felt? = null

    @BeforeTest
    fun before() {
        devnetClient.start()
        contractAddress = devnetClient.deployContract("testContract")
    }

    @AfterTest
    fun after() {
        devnetClient.destroy()
    }

    private fun getProvider(): Provider {
        return GatewayProvider(
            devnetClient.feederGatewayUrl,
            devnetClient.gatewayUrl,
            StarknetChainId.TESTNET
        )
    }

    private fun getBalance(provider: Provider): Felt {
        val call = Call(
            contractAddress!!,
            "get_balance",
            emptyList()
        )

        val request = provider.callContract(call, BlockTag.PENDING)

        val response = request.send()

        assertEquals(1, response.result.size)

        return response.result.first()
    }

    @Test
    fun callContractTest() {
        val provider = getProvider()

        val balance = getBalance(provider)

        assertEquals(Felt(0), balance)
    }

    @Test
    fun getStorageAtTest() {
        val provider = getProvider()

        val request = provider.getStorageAt(
            contractAddress!!,
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
            contractAddress!!,
            "increase_balance",
            listOf(Felt(10))
        )

        val dummySig = listOf(Felt(0), Felt(0), Felt(0), Felt(0), Felt(0))
        val payload = InvokeFunctionPayload(call, dummySig, Felt(0), null)
        val request = provider.invokeFunction(payload)

        request.send()

        val balance = getBalance(provider)
        assertEquals(Felt(10), balance)
    }
}
