package starknet.provider

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import starknet.data.selectorFromName
import starknet.data.types.*
import starknet.utils.DevnetClient
import starknet.provider.gateway.GatewayProvider
import starknet.provider.rpc.JsonRpcProvider

class ProviderTest {
    private var contractAddress: Felt? = null

    companion object {
        @JvmStatic
        private val devnetClient = DevnetClient()

        @JvmStatic
        private fun getProviders(): List<Provider> {
            return listOf(
                GatewayProvider(
                    devnetClient.feederGatewayUrl,
                    devnetClient.gatewayUrl,
                    StarknetChainId.TESTNET
                ),
                JsonRpcProvider(
                    devnetClient.rpcUrl,
                    StarknetChainId.TESTNET
                )
            )
        }
    }

    @BeforeEach
    fun before() {
        devnetClient.start()
        contractAddress = devnetClient.deployContract("providerTest")
    }

    @AfterEach
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

    @ParameterizedTest
    @MethodSource("getProviders")
    fun callContractTest(provider: Provider) {
        // Currently not supported in devnet
        if (provider is JsonRpcProvider) {
            return
        }

        val balance = getBalance(provider)

        assertEquals(Felt(0), balance)
    }

    @ParameterizedTest()
    @MethodSource("getProviders")
    fun getStorageAtTest(provider: Provider) {
        // Currently not supported in devnet
        if (provider is JsonRpcProvider) {
            return
        }

        val request = provider.getStorageAt(
            contractAddress!!,
            selectorFromName("balance"),
            BlockTag.PENDING
        )

        val response = request.send()

        assertEquals(Felt(0), response)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun invokeTransactionTest(provider: Provider) {
        // Currently not supported in devnet
        if (provider is JsonRpcProvider) {
            return
        }

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
