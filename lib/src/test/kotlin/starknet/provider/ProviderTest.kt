package starknet.provider

import com.swmansion.starknet.data.responses.*
import com.swmansion.starknet.data.responses.DeclareTransaction
import com.swmansion.starknet.data.responses.DeployTransaction
import com.swmansion.starknet.data.responses.InvokeTransaction
import com.swmansion.starknet.data.selectorFromName
import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.provider.Provider
import com.swmansion.starknet.provider.gateway.GatewayProvider
import com.swmansion.starknet.provider.rpc.JsonRpcProvider
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import starknet.utils.DevnetClient
import java.nio.file.Files
import java.nio.file.Path

class ProviderTest {
    companion object {
        @JvmStatic
        private val devnetClient = DevnetClient()
        private lateinit var contractAddress: Felt
        private lateinit var deployTransactionHash: Felt
        private lateinit var invokeTransactionHash: Felt
        private lateinit var declareTransactionHash: Felt

        @JvmStatic
        private fun getProviders(): List<Provider> {
            return listOf(
                GatewayProvider(
                    devnetClient.feederGatewayUrl,
                    devnetClient.gatewayUrl,
                    StarknetChainId.TESTNET,
                ),
                JsonRpcProvider(
                    devnetClient.rpcUrl,
                    StarknetChainId.TESTNET,
                ),
            )
        }

        @JvmStatic
        private fun gatewayProvider(): GatewayProvider {
            return GatewayProvider(devnetClient.feederGatewayUrl, devnetClient.gatewayUrl, StarknetChainId.TESTNET)
        }

        @JvmStatic
        private fun rpcProvider(): JsonRpcProvider {
            return JsonRpcProvider(devnetClient.rpcUrl, StarknetChainId.TESTNET)
        }

        @JvmStatic
        @BeforeAll
        fun before() {
            devnetClient.start()

            val (deployAddress, deployHash) = devnetClient.deployContract(Path.of("src/test/resources/compiled/providerTest.json"))
            val (_, invokeHash) = devnetClient.invokeTransaction(
                "increase_balance",
                deployAddress,
                Path.of("src/test/resources/compiled/providerTestAbi.json"),
                0,
            )
            val (_, declareHash) = devnetClient.declareContract(Path.of("src/test/resources/compiled/providerTest.json"))
            contractAddress = deployAddress
            deployTransactionHash = deployHash
            invokeTransactionHash = invokeHash
            declareTransactionHash = declareHash
        }

        @JvmStatic
        @AfterAll
        fun after() {
            devnetClient.destroy()
        }
    }

    private fun getBalance(provider: Provider): Felt {
        val call = Call(
            contractAddress,
            "get_balance",
            emptyList(),
        )

        val request = provider.callContract(call, BlockTag.PENDING)
        val response = request.send()

        assertEquals(1, response.result.size)

        return response.result.first()
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun callContractTest(provider: Provider) {
        // FIXME: Currently not supported in devnet
        if (provider is JsonRpcProvider) {
            return
        }

        val balance = getBalance(provider)

        assertEquals(Felt(0), balance)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun getStorageAtTest(provider: Provider) {
        // FIXME: Currently not supported in devnet
        if (provider is JsonRpcProvider) {
            return
        }

        val request = provider.getStorageAt(
            contractAddress,
            selectorFromName("balance"),
            BlockTag.PENDING,
        )

        val response = request.send()

        assertEquals(Felt(0), response)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun invokeTransactionTest(provider: Provider) {
        // FIXME: Currently not supported in devnet
        if (provider is JsonRpcProvider) {
            return
        }

        val call = Call(
            contractAddress,
            "increase_balance",
            listOf(Felt(10)),
        )

        val dummySig = listOf(Felt(0), Felt(0), Felt(0), Felt(0), Felt(0))
        val payload = InvokeFunctionPayload(call, dummySig, Felt(0), null)
        val request = provider.invokeFunction(payload)

        request.send()

        val balance = getBalance(provider)
        assertEquals(Felt(10), balance)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get class`(provider: Provider) {
        val hash = Felt.fromHex("0x1b322dd827d4579c10a08025b9d685c7ed16dcb25c7371dd06a65984cb5426")

        val request = provider.getClass(hash)
        val response = request.send()

        assertNotNull(response)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get class at block hash`(provider: Provider) {
        // FIXME: Rpc endpoint not supported in devnet, and no gateway endpoint for it
        return

        if (provider !is JsonRpcProvider) {
            return
        }

        val latestBlock = devnetClient.getLatestBlock()

        val request = provider.getClassAt(latestBlock.hash, contractAddress)
        val response = request.send()

        assertNotNull(response)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get class at block number`(provider: Provider) {
        // FIXME: Rpc endpoint not supported in devnet, and no gateway endpoint for it
        return

        if (provider !is JsonRpcProvider) {
            return
        }

        val latestBlock = devnetClient.getLatestBlock()

        val request = provider.getClassAt(latestBlock.number, contractAddress)
        val response = request.send()

        assertNotNull(response)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get class at latest block`(provider: Provider) {
        // FIXME: Rpc endpoint not supported in devnet, and no gateway endpoint for it
        return

        if (provider !is JsonRpcProvider) {
            return
        }

        val request = provider.getClassAt(BlockTag.LATEST, contractAddress)
        val response = request.send()

        assertNotNull(response)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get class at pending block`(provider: Provider) {
        // FIXME: Rpc endpoint not supported in devnet, and no gateway endpoint for it
        return

        if (provider !is JsonRpcProvider) {
            return
        }

        val request = provider.getClassAt(BlockTag.PENDING, contractAddress)
        val response = request.send()

        assertNotNull(response)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get class hash at pending block`(provider: Provider) {
        // FIXME: Currently not supported in devnet
        if (provider is JsonRpcProvider) {
            return
        }

        val request = provider.getClassHashAt(BlockTag.PENDING, contractAddress)
        val response = request.send()

        assertNotNull(response)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get class hash at latest block`(provider: Provider) {
        // FIXME: Currently not supported in devnet
        if (provider is JsonRpcProvider) {
            return
        }

        val request = provider.getClassHashAt(BlockTag.LATEST, contractAddress)
        val response = request.send()

        assertNotNull(response)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get class hash at block hash`(provider: Provider) {
        // FIXME: Currently not supported in devnet
        if (provider is JsonRpcProvider) {
            return
        }
        val latestBlock = devnetClient.getLatestBlock()

        val request = provider.getClassHashAt(latestBlock.hash, contractAddress)
        val response = request.send()

        assertNotNull(response)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get class hash at block number`(provider: Provider) {
        // FIXME: Currently not supported in devnet
        if (provider is JsonRpcProvider) {
            return
        }
        val latestBlock = devnetClient.getLatestBlock()

        val request = provider.getClassHashAt(latestBlock.number, contractAddress)
        val response = request.send()

        assertNotNull(response)
    }

    @Test
    fun `get deploy transaction receipt gateway`() {
        val request = gatewayProvider().getTransactionReceipt(deployTransactionHash)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is GatewayTransactionReceipt)
    }

    @Test
    fun `get declare transaction receipt gateway`() {
        val request = gatewayProvider().getTransactionReceipt(declareTransactionHash)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is GatewayTransactionReceipt)
    }

    @Test
    fun `get invoke transaction receipt gateway`() {
        val request = gatewayProvider().getTransactionReceipt(invokeTransactionHash)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is GatewayTransactionReceipt)
    }

    // FIXME(This test will fail until devnet is updated to the newest rpc spec)
    @Test
    fun `get deploy transaction receipt rpc`() {
        val request = rpcProvider().getTransactionReceipt(deployTransactionHash)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is DeployTransactionReceipt)
    }

    // FIXME(This test will fail until devnet is updated to the newest rpc spec)
    @Test
    fun `get declare transaction receipt rpc`() {
        val request = rpcProvider().getTransactionReceipt(declareTransactionHash)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is DeclareTransactionReceipt)
    }

    // FIXME(This test will fail until devnet is updated to the newest rpc spec)
    @Test
    fun `get invoke transaction receipt rpc`() {
        val request = rpcProvider().getTransactionReceipt(invokeTransactionHash)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is InvokeTransactionReceipt)
    }

    // FIXME(This test will fail until devnet is updated to the newest rpc spec)
    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get deploy transaction`(provider: Provider) {
        val request = provider.getTransaction(deployTransactionHash)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is DeployTransaction)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get invoke transaction`(provider: Provider) {
        val request = provider.getTransaction(invokeTransactionHash)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is InvokeTransaction)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get declare transaction`(provider: Provider) {
        if (provider is JsonRpcProvider) {
            // FIXME current rpc spec has class_hash in declare txn, but the version currently supported in devnet
            //       doesn't.
            return
        }

        val request = provider.getTransaction(declareTransactionHash)
        val response = request.send()

        assertNotNull(response)
        assertNotEquals(declareTransactionHash, deployTransactionHash)
        assertTrue(response is DeclareTransaction)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `deploy contract`(provider: Provider) {
        val contractPath = Path.of("src/test/resources/compiled/providerTest.json")
        val contents = Files.readString(contractPath)
        val payload = DeployTransactionPayload(ContractDefinition(contents), Felt(1), emptyList(), Felt(0))

        val request = provider.deployContract(payload)
        val response = request.send()

        assertNotNull(response)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `deploy with constructor calldata`(provider: Provider) {
        val contractPath = Path.of("src/test/resources/compiled/contractWithConstructor.json")
        val contents = Files.readString(contractPath)
        val payload =
            DeployTransactionPayload(ContractDefinition(contents), Felt(1), listOf(Felt(123), Felt(456)), Felt(0))

        val request = provider.deployContract(payload)
        val response = request.send()

        assertNotNull(response)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `declare contract`(provider: Provider) {
        val contractPath = Path.of("src/test/resources/compiled/providerTest.json")
        val contents = Files.readString(contractPath)
        val payload =
            DeclareTransactionPayload(ContractDefinition(contents), Felt.ZERO, Felt.ZERO, emptyList(), Felt(0))

        val request = provider.declareContract(payload)
        val response = request.send()

        assertNotNull(response)
    }

    @Test
    fun `make contract definition with invalid json`() {
        assertThrows(InvalidContractException::class.java) {
            ContractDefinition("{}")
        }
    }
}
