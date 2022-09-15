package starknet.provider

import com.swmansion.starknet.data.selectorFromName
import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.data.types.transactions.*
import com.swmansion.starknet.provider.Provider
import com.swmansion.starknet.provider.exceptions.GatewayRequestFailedException
import com.swmansion.starknet.provider.exceptions.RpcRequestFailedException
import com.swmansion.starknet.provider.gateway.GatewayProvider
import com.swmansion.starknet.provider.rpc.JsonRpcProvider
import com.swmansion.starknet.service.http.HttpResponse
import com.swmansion.starknet.service.http.HttpService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import starknet.utils.DevnetClient
import java.nio.file.Files
import java.nio.file.Path

class ProviderTest {
    companion object {
        @JvmStatic
        private val devnetClient = DevnetClient()
        private lateinit var contractAddress: Felt
        private lateinit var classHash: Felt
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
                    listOf(Felt.ZERO),
                )
                val (classHash, declareHash) = devnetClient.declareContract(Path.of("src/test/resources/compiled/providerTest.json"))

                this.contractAddress = deployAddress
                this.classHash = classHash
                this.deployTransactionHash = deployHash
                this.invokeTransactionHash = invokeHash
                this.declareTransactionHash = declareHash
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
    fun `get class`(provider: Provider) {
        val request = provider.getClass(classHash)
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

    @Test
    fun `get events`() {
        val events = """
        {
            "id": 0,
            "jsonrpc": "2.0",
            "result": {
                "events": [
                    {
                        "address": "0x01",
                        "keys": ["0x0a", "0x0b"],
                        "data": ["0x0c", "0x0d"],
                        "block_hash": "0x0aaaa",
                        "block_number": 1234,
                        "transaction_hash": "0x01234"
                    }
                ],
                "page_number": 1,
                "is_last_page": false
            }
        }
        """.trimIndent()

        val httpService = mock<HttpService> {
            on { send(any()) } doReturn HttpResponse(true, 200, events)
        }
        val provider = JsonRpcProvider(devnetClient.rpcUrl, StarknetChainId.TESTNET, httpService)

        val request = provider.getEvents(
            GetEventsPayload(
                BlockId.Number(1),
                BlockId.Number(2),
                Felt(111),
                listOf(Felt.fromHex("0x0a"), Felt.fromHex("0x0b")),
                100,
                1,
            ),
        )

        val response = request.send()

        assertNotNull(response)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get current block number`(provider: Provider) {
        val currentBlock = provider.getBlockNumber()
        val response = currentBlock.send()

        assertNotNull(response)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get current block number and hash`(provider: Provider) {
        val currentBlock = provider.getBlockHashAndNumber()
        val response = currentBlock.send()

        assertNotNull(response)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get block transaction count with block tag`(provider: Provider) {
        val blockTransactionCount = provider.getBlockTransactionCount(BlockTag.LATEST)
        val response = blockTransactionCount.send()

        assertNotNull(response)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get block transaction count with block hash`(provider: Provider) {
        val blockTransactionCount = provider.getBlockTransactionCount(Felt.fromHex("0x0"))
        val response = blockTransactionCount.send()

        assertNotNull(response)
        assertEquals(0, response)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get block transaction count with block id`(provider: Provider) {
        val blockTransactionCount = provider.getBlockTransactionCount(0)
        val response = blockTransactionCount.send()

        assertNotNull(response)
        assertEquals(0, response)
    }

    @Test
    fun `get sync information node not syncing`() {
        val provider = rpcProvider()
        val request = provider.getSyncing()
        val response = request.send()

        assertNotNull(response)
        assertFalse(response.status)
    }

    @Test
    fun `get sync information node synced`() {
        val mocked_response = """
        {
            "id": 0,
            "jsonrpc": "2.0",
            "result": {
                "starting_block_hash": "0x0",
                "starting_block_num": 0,
                "current_block_hash": "0x1",
                "current_block_num": 1,
                "highest_block_hash": "0x10",
                "highest_block_num": 10
            }
        }
        """.trimIndent()
        val httpService = mock<HttpService> {
            on { send(any()) } doReturn HttpResponse(true, 200, mocked_response)
        }
        val provider = JsonRpcProvider(devnetClient.rpcUrl, StarknetChainId.TESTNET, httpService)
        val request = provider.getSyncing()
        val response = request.send()

        assertNotNull(response)
        assertTrue(response.status)
        assertEquals(Felt.ZERO, response.startingBlockHash)
        assertEquals(0, response.startingBlockNumber)
        assertEquals(Felt.fromHex("0x1"), response.currentBlockHash)
        assertEquals(1, response.currentBlockNumber)
        assertEquals(Felt.fromHex("0x10"), response.highestBlockHash)
        assertEquals(10, response.highestBlockNumber)
    }
}
