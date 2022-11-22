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
        private lateinit var deployAccountTransactionHash: Felt

        @JvmStatic
        private fun getProviders(): List<Provider> = listOf(gatewayProvider(), rpcProvider())

        private data class BlockHashAndNumber(val hash: Felt, val number: Int)

        @JvmStatic
        private val latestBlock: BlockHashAndNumber
            get() {
                val latestBlock = devnetClient.latestBlock()
                return BlockHashAndNumber(latestBlock.blockHash, latestBlock.blockNumber)
            }

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

                val contract = Path.of("src/test/resources/compiled/providerTest.json").toFile().readText()
                val (deployHash, contractAddress) = gatewayProvider().deployContract(
                    DeployTransactionPayload(
                        contractDefinition = ContractDefinition(contract),
                        constructorCalldata = listOf(),
                        salt = Felt.ONE,
                        version = Felt.ZERO,
                    ),
                ).send()
                val (_, invokeHash) = devnetClient.invokeTransaction(
                    "increase_balance",
                    contractAddress,
                    Path.of("src/test/resources/compiled/providerTestAbi.json"),
                    listOf(Felt.ZERO),
                )
                val (classHash, declareHash) = devnetClient.declareContract(Path.of("src/test/resources/compiled/providerTest.json"))
                val (_, deployAccountHash) = devnetClient.deployAccount()
                this.contractAddress = contractAddress
                this.classHash = classHash
                this.deployTransactionHash = deployHash
                this.invokeTransactionHash = invokeHash
                this.declareTransactionHash = declareHash
                this.deployAccountTransactionHash = deployAccountHash
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

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `call contract with block number`(provider: Provider) {
        // Devnet is not supporting RPC calls with id different from "latest"
        if (provider is JsonRpcProvider) return

        val call = Call(
            contractAddress,
            "get_balance",
            emptyList(),
        )
        val expected = devnetClient.getStorageAt(contractAddress, selectorFromName("balance"))

        val request = provider.callContract(call, latestBlock.number)
        val response = request.send()
        val balance = response.first()

        assertEquals(expected, balance)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `call contract with block hash`(provider: Provider) {
        // Devnet is not supporting RPC calls with id different from "latest"
        if (provider is JsonRpcProvider) return

        val call = Call(
            contractAddress,
            "get_balance",
            emptyList(),
        )
        val expected = devnetClient.getStorageAt(contractAddress, selectorFromName("balance"))

        val request = provider.callContract(call, latestBlock.hash)
        val response = request.send()
        val balance = response.first()

        assertEquals(expected, balance)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `call contract with block tag`(provider: Provider) {
        val call = Call(
            contractAddress,
            "get_balance",
            emptyList(),
        )
        val expected = devnetClient.getStorageAt(contractAddress, selectorFromName("balance"))

        val request = provider.callContract(call, BlockTag.LATEST)
        val response = request.send()
        val balance = response.first()

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
    fun `get class at`(provider: Provider) {
        if (provider !is JsonRpcProvider) {
            return
        }

        val request = provider.getClassAt(contractAddress)
        val response = request.send()

        assertNotNull(response)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get class at block hash`(provider: Provider) {
        // FIXME: Devnet only support's "latest" as block id in this method, no endpoint for it in gateway.
        return

        if (provider !is JsonRpcProvider) {
            return
        }

        val latestBlock = devnetClient.getLatestBlock()

        val request = provider.getClassAt(contractAddress, latestBlock.hash)
        val response = request.send()

        assertNotNull(response)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get class at block number`(provider: Provider) {
        // FIXME: Devnet only support's "latest" as block id in this method, no endpoint for it in gateway.
        return

        if (provider !is JsonRpcProvider) {
            return
        }

        val latestBlock = devnetClient.getLatestBlock()

        val request = provider.getClassAt(contractAddress, latestBlock.number)
        val response = request.send()

        assertNotNull(response)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get class at latest block`(provider: Provider) {
        if (provider !is JsonRpcProvider) {
            return
        }

        val request = provider.getClassAt(contractAddress, BlockTag.LATEST)
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

        val request = provider.getClassAt(contractAddress, BlockTag.PENDING)
        val response = request.send()

        assertNotNull(response)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get class hash`(provider: Provider) {
        val request = provider.getClassHashAt(contractAddress)
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

    @Test
    fun `get l1 handler transaction receipt gateway`() {
        val httpService = mock<HttpService> {
            on { send(any()) } doReturn HttpResponse(
                true,
                200,
                """
                {
                    "status": "ACCEPTED_ON_L2",
                    "block_hash": "0x16c6bc59271e7b727ac0b139bbf99336fec1c0bfb6d41540d36fe1b3e2994c9",
                    "block_number": 392723,
                    "transaction_index": 13,
                    "transaction_hash": "0x4b2ff971b669e31c704fde5c1ad6ee08ba2000986a25ad5106ab94546f36f7",
                    "l1_to_l2_consumed_message": {
                        "from_address": "0xc3511006C04EF1d78af4C8E0e74Ec18A6E64Ff9e",
                        "to_address": "0x73314940630fd6dcda0d772d4c972c4e0a9946bef9dabf4ef84eda8ef542b82",
                        "selector": "0x2d757788a8d8d6f21d1cd40bce38a8222d70654214e96ff95d8086e684fbee5",
                        "payload": [
                            "0x4a5a3df7914b59feff1b52164c314d3df0666c053997dfd7c5ff79676984fe6",
                            "0x58d15e176280000",
                            "0x0"
                        ],
                        "nonce": "0x402af"
                    },
                    "l2_to_l1_messages": [],
                    "events": [
                        {
                            "from_address": "0x49d36570d4e46f48e99674bd3fcc84644ddd6b96f7c741b1562b82f9e004dc7",
                            "keys": [
                                "0x99cd8bde557814842a3121e8ddfd433a539b8c9f14bf31ebf108d12e6196e9"
                            ],
                            "data": [
                                "0x0",
                                "0x4a5a3df7914b59feff1b52164c314d3df0666c053997dfd7c5ff79676984fe6",
                                "0x58d15e176280000",
                                "0x0"
                            ]
                        },
                        {
                            "from_address": "0x73314940630fd6dcda0d772d4c972c4e0a9946bef9dabf4ef84eda8ef542b82",
                            "keys": [
                                "0x221e5a5008f7a28564f0eaa32cdeb0848d10657c449aed3e15d12150a7c2db3"
                            ],
                            "data": [
                                "0x4a5a3df7914b59feff1b52164c314d3df0666c053997dfd7c5ff79676984fe6",
                                "0x58d15e176280000",
                                "0x0"
                            ]
                        }
                    ],
                    "execution_resources": {
                        "n_steps": 673,
                        "builtin_instance_counter": {
                            "pedersen_builtin": 2,
                            "range_check_builtin": 12
                        },
                        "n_memory_holes": 22
                    },
                    "actual_fee": "0x0"
                }
                """.trimIndent(),
            )
        }
        val provider = GatewayProvider("", "", StarknetChainId.TESTNET, httpService)

        val request = provider.getTransactionReceipt(Felt.ZERO)
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
        // FIXME: devnet fails when tx doesn't have entry_point_selector, remove condition after bumping devnet
        if (provider is JsonRpcProvider) {
            return
        }
        val request = provider.getTransaction(invokeTransactionHash)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is InvokeTransaction)
    }

    @Test
    fun `get deploy account transaction`() {
        val provider = gatewayProvider()
        val tx = provider.getTransaction(deployAccountTransactionHash).send()

        assertTrue(tx is DeployAccountTransaction)
    }

    @Test
    fun `get deploy account transaction receipt`() {
        val provider = gatewayProvider()
        val receipt = provider.getTransactionReceipt(deployAccountTransactionHash).send()

        assertTrue(receipt is GatewayTransactionReceipt)
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

    // FIXME(Extend this to rpc provider this once devnet supports rpc 0.2.1 spec
    @Test
    fun `get l1 handler transaction`() {
        val httpService = mock<HttpService> {
            on { send(any()) } doReturn HttpResponse(
                true,
                200,
                """
                {
                    "status": "ACCEPTED_ON_L1",
                    "block_hash": "0x38ce7678420eaff5cd62597643ca515d0887579a8be69563067fe79a624592b",
                    "block_number": 370459,
                    "transaction_index": 9,
                    "transaction": {
                        "version": "0x0",
                        "contract_address": "0x278f24c3e74cbf7a375ec099df306289beb0605a346277d200b791a7f811a19",
                        "entry_point_selector": "0x2d757788a8d8d6f21d1cd40bce38a8222d70654214e96ff95d8086e684fbee5",
                        "nonce": "0x34c20",
                        "calldata": [
                            "0xd8beaa22894cd33f24075459cfba287a10a104e4",
                            "0x3f9c67ef1d31e24b386184b4ede63a869c4659de093ef437ee235cae4daf2be",
                            "0x3635c9adc5dea00000",
                            "0x0",
                            "0x7cb4539b69a2371f75d21160026b76a7a7c1cacb"
                        ],
                        "transaction_hash": "0x7e1ed66dbccf915857c6367fc641c24292c063e54a5dd55947c2d958d94e1a9",
                        "type": "L1_HANDLER"
                    }
                }
                """.trimIndent(),
            )
        }
        val provider = GatewayProvider("", "", StarknetChainId.TESTNET, httpService)

        val request = provider.getTransaction(Felt.ZERO)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is L1HandlerTransaction)
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
        val request = rpcProvider().getClassAt(Felt(0), BlockTag.LATEST)

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
    fun `gateway provider throws GatewayRequestFailedException on mocked testnet response`() {
        val httpService = mock<HttpService> {
            on { send(any()) } doReturn HttpResponse(
                false,
                500,
                "{\"code\": \"StarknetErrorCode.UNINITIALIZED_CONTRACT\", \"message\": \"Requested contract address 0x1 is not deployed.\"}",
            )
        }
        val provider = GatewayProvider("", "", StarknetChainId.TESTNET, httpService)
        val request = provider.getClass(Felt.ZERO)

        val exception = assertThrows(GatewayRequestFailedException::class.java) {
            request.send()
        }
        assertEquals("Requested contract address 0x1 is not deployed.", exception.message)
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
        val mockedResponse = """
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
            on { send(any()) } doReturn HttpResponse(true, 200, mockedResponse)
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

    @Test
    fun `received gateway receipt`() {
        // There is no way for us to recreate this behaviour as devnet processes txs right away
        val httpService = mock<HttpService> {
            on { send(any()) } doReturn HttpResponse(
                true,
                200,
                """
                {
                    "status": "RECEIVED", 
                    "transaction_hash": "0x334da4f63cc6309ba2429a70f103872ab0ae82cf8d9a73b845184a4713cada5", 
                    "l2_to_l1_messages": [], 
                    "events": []
                }
                """.trimIndent(),
            )
        }
        val provider = GatewayProvider.makeTestnetClient(httpService)
        val receipt = provider.getTransactionReceipt(
            Felt.fromHex("0x334da4f63cc6309ba2429a70f103872ab0ae82cf8d9a73b845184a4713cada5"),
        ).send() as GatewayTransactionReceipt

        assertEquals(Felt.fromHex("0x334da4f63cc6309ba2429a70f103872ab0ae82cf8d9a73b845184a4713cada5"), receipt.hash)
        assertEquals(TransactionStatus.PENDING, receipt.status)
        assertEquals(emptyList<MessageToL1>(), receipt.messagesToL1)
        assertEquals(emptyList<Event>(), receipt.events)
        assertNull(receipt.messageToL2)
        assertNull(receipt.actualFee)
        assertNull(receipt.failureReason)
    }
}
