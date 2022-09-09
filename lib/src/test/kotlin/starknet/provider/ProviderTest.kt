package starknet.provider

import com.swmansion.starknet.data.selectorFromName
import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.data.types.transactions.*
import com.swmansion.starknet.provider.Provider
import com.swmansion.starknet.provider.exceptions.GatewayRequestFailedException
import com.swmansion.starknet.provider.exceptions.RpcRequestFailedException
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
        private fun getProviders(): List<Provider> = listOf(gatewayProvider(), rpcProvider())

        @JvmStatic
        private fun gatewayProvider(): GatewayProvider =
            GatewayProvider(devnetClient.feederGatewayUrl, devnetClient.gatewayUrl, StarknetChainId.TESTNET)

        @JvmStatic
        private fun rpcProvider(): JsonRpcProvider = JsonRpcProvider(devnetClient.rpcUrl, StarknetChainId.TESTNET)

        @JvmStatic
        private fun isAccepted(receipt: TransactionReceipt): Boolean {
            if (receipt !is ProcessedTransactionReceipt) {
                return false
            }
            return receipt.status == TransactionStatus.ACCEPTED_ON_L2 || receipt.status == TransactionStatus.ACCEPTED_ON_L1
        }

        @JvmStatic
        @BeforeAll
        fun before() {
            try {
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
            } catch (ex: Exception) {
                devnetClient.close()
                throw ex
            }
        }

        @JvmStatic
        @AfterAll
        fun after() {
            devnetClient.close()
        }
    }

    private fun getBalance(provider: Provider): Felt {
        val call = Call(
            contractAddress,
            "get_balance",
            emptyList(),
        )

        val request = provider.callContract(call, BlockTag.LATEST)
        val response = request.send()

        assertEquals(1, response.size)

        return response.first()
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `call contract`(provider: Provider) {
        val balance = getBalance(provider)
        val expected = devnetClient.getStorageAt(contractAddress, selectorFromName("balance"))

        assertEquals(expected, balance)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get storage at`(provider: Provider) {
        val request = provider.getStorageAt(
            contractAddress,
            selectorFromName("balance"),
            BlockTag.LATEST,
        )

        val response = request.send()
        val expected = devnetClient.getStorageAt(contractAddress, selectorFromName("balance"))

        assertEquals(expected, response)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `invoke transaction`(provider: Provider) {
        val invokeValue = Felt(10)

        val call = Call(
            contractAddress,
            "increase_balance",
            listOf(invokeValue),
        )

        val oldBalance = devnetClient.getStorageAt(contractAddress, selectorFromName("balance"))

        val dummySig = listOf(Felt(0), Felt(0), Felt(0), Felt(0), Felt(0))
        val payload = InvokeFunctionPayload(call, dummySig, Felt.ZERO, Felt.ZERO)
        val request = provider.invokeFunction(payload)

        request.send()

        val newBalance = devnetClient.getStorageAt(contractAddress, selectorFromName("balance"))

        assertEquals(oldBalance.value + invokeValue.value, newBalance.value)
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
        // Devnet only support's "latest" as block id in this method
        if (provider is JsonRpcProvider) {
            return
        }

        val request = provider.getClassHashAt(contractAddress, BlockTag.PENDING)
        val response = request.send()

        assertNotNull(response)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get class hash at latest block`(provider: Provider) {
        val request = provider.getClassHashAt(contractAddress, BlockTag.LATEST)
        val response = request.send()

        assertNotNull(response)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get class hash at block hash`(provider: Provider) {
        // Devnet only support's "latest" as block id in this method
        if (provider is JsonRpcProvider) {
            return
        }
        val latestBlock = devnetClient.getLatestBlock()

        val request = provider.getClassHashAt(contractAddress, latestBlock.hash)
        val response = request.send()

        assertNotNull(response)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get class hash at block number`(provider: Provider) {
        // Devnet only support's "latest" as block id in this method
        if (provider is JsonRpcProvider) {
            return
        }
        val latestBlock = devnetClient.getLatestBlock()

        val request = provider.getClassHashAt(contractAddress, latestBlock.number)
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

    // FIXME this test will fail until devnet spec is updated as there is no way to differentiate between declare
    //  and deploy tx receipts currently
//    @Test
//    fun `get deploy transaction receipt rpc`() {
//        val request = rpcProvider().getTransactionReceipt(deployTransactionHash)
//        val response = request.send()
//
//        assertNotNull(response)
//        assertTrue(response is DeployTransactionReceipt)
//    }

    @Test
    fun `get declare transaction receipt rpc`() {
        val request = rpcProvider().getTransactionReceipt(declareTransactionHash)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is DeclareTransactionReceipt)
    }

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

        val txrRequest = provider.getTransactionReceipt(response.transactionHash)
        val txr = txrRequest.send()
        assertTrue(isAccepted(txr))
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

        val txrRequest = provider.getTransactionReceipt(response.transactionHash)
        val txr = txrRequest.send()
        assertTrue(isAccepted(txr))
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

        val txrRequest = provider.getTransactionReceipt(response.transactionHash)
        val txr = txrRequest.send()
        assertTrue(isAccepted(txr))
    }

    @Test
    fun `rpc provider throws RpcRequestFailedException`() {
        val request = rpcProvider().getClassAt(BlockTag.LATEST, Felt(0))

        val exception = assertThrows(RpcRequestFailedException::class.java) {
            request.send()
        }
        assertEquals(20, exception.code)
        assertEquals("Contract not found", exception.message)
    }

    @Test
    fun `gateway provider throws GatewayRequestFailedException`() {
        val request = gatewayProvider().getClass(Felt(0))

        val exception = assertThrows(GatewayRequestFailedException::class.java) {
            request.send()
        }
        assertEquals("Class with hash 0x0 is not declared", exception.message)
    }

    @Test
    fun `make contract definition with invalid json`() {
        assertThrows(ContractDefinition.InvalidContractException::class.java) {
            ContractDefinition("{}")
        }
    }
}
