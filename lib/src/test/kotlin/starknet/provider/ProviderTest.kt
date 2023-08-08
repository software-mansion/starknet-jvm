package starknet.provider

import com.swmansion.starknet.data.selectorFromName
import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.data.types.transactions.*
import com.swmansion.starknet.provider.Provider
import com.swmansion.starknet.provider.exceptions.GatewayRequestFailedException
import com.swmansion.starknet.provider.exceptions.RequestFailedException
import com.swmansion.starknet.provider.exceptions.RpcRequestFailedException
import com.swmansion.starknet.provider.gateway.GatewayProvider
import com.swmansion.starknet.provider.rpc.JsonRpcProvider
import com.swmansion.starknet.service.http.HttpResponse
import com.swmansion.starknet.service.http.HttpService
import com.swmansion.starknet.service.http.OkHttpService
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.*
import starknet.utils.DevnetClient
import java.math.BigInteger
import java.nio.file.Path
import java.nio.file.Paths

class ProviderTest {
    companion object {
        @JvmStatic
        private val devnetClient = DevnetClient(accountDirectory = Paths.get("src/test/resources/provider_test_account"))
        private lateinit var contractAddress: Felt
        private lateinit var classHash: Felt
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
        private fun mockUpdatedReceiptRpcProvider(sourceProvider: Provider? = null): JsonRpcProvider {
            val mockHttpService = spy(OkHttpService())

            doAnswer { invocation ->
                val originalHttpResponse = invocation.callRealMethod() as HttpResponse
                val originalJson = Json.parseToJsonElement(originalHttpResponse.body) as JsonObject
                val status = originalJson["result"]!!.jsonObject["status"]?.jsonPrimitive?.content
                    ?: TransactionFinalityStatus.ACCEPTED_ON_L1.toString()

                val acceptedStatuses = listOf(
                    TransactionStatus.ACCEPTED_ON_L1.toString(),
                    TransactionStatus.ACCEPTED_ON_L2.toString(),
                )
                val executionStatus = when (status) {
                    in acceptedStatuses -> TransactionExecutionStatus.SUCCEEDED.toString()
                    else -> TransactionExecutionStatus.REVERTED.toString()
                }

                val modifiedJson = JsonObject(
                    originalJson["result"]!!.jsonObject.toMutableMap().apply {
                        remove("status")
                        put("finality_status", JsonPrimitive(status))
                        put("execution_status", JsonPrimitive(executionStatus))
                    },
                )
                val mergedJson = JsonObject(
                    originalJson.toMutableMap().apply {
                        this["result"] = modifiedJson
                    },
                )
                return@doAnswer HttpResponse(
                    isSuccessful = originalHttpResponse.isSuccessful,
                    code = originalHttpResponse.code,
                    body = mergedJson.toString(),
                )
            }.`when`(mockHttpService).send(any())

            return when (sourceProvider) {
                is JsonRpcProvider -> JsonRpcProvider(
                    sourceProvider.url,
                    sourceProvider.chainId,
                    mockHttpService,
                )
                else -> JsonRpcProvider(
                    devnetClient.rpcUrl,
                    StarknetChainId.TESTNET,
                    mockHttpService,
                )
            }
        }

        @JvmStatic
        private fun isAccepted(receipt: TransactionReceipt): Boolean {
            if (receipt !is ProcessedTransactionReceipt) {
                return false
            }
            return receipt.executionStatus == TransactionExecutionStatus.SUCCEEDED
        }

        @JvmStatic
        @BeforeAll
        fun before() {
            try {
                devnetClient.start()

                val (contractAddress, _) = devnetClient.deployContract(Path.of("src/test/resources/compiled_v0/providerTest.json"))
                val (_, invokeHash) = devnetClient.invokeTransaction(
                    "increase_balance",
                    contractAddress,
                    Path.of("src/test/resources/compiled_v0/providerTestAbi.json"),
                    listOf(Felt.ZERO),
                )
                val (classHash, declareHash) = devnetClient.declareContract(Path.of("src/test/resources/compiled_v0/providerTest.json"))
                val (_, deployAccountHash) = devnetClient.deployAccount()
                this.contractAddress = contractAddress
                this.classHash = classHash
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

    @Test
    fun `make default gateway testnet client`() {
        val provider = GatewayProvider.makeTestnetProvider()
        assertEquals("https://alpha4.starknet.io/feeder_gateway", provider.feederGatewayUrl)
        assertEquals("https://alpha4.starknet.io/gateway", provider.gatewayUrl)
        assertEquals(StarknetChainId.TESTNET, provider.chainId)
    }

    @Test
    fun `make specific testnet gateway client`() {
        val testnet1Provider = GatewayProvider.makeTestnetProvider(StarknetChainId.TESTNET)
        val testnet2Provider = GatewayProvider.makeTestnetProvider(StarknetChainId.TESTNET2)

        assertEquals("https://alpha4.starknet.io/feeder_gateway", testnet1Provider.feederGatewayUrl)
        assertEquals("https://alpha4.starknet.io/gateway", testnet1Provider.gatewayUrl)
        assertEquals(StarknetChainId.TESTNET, testnet1Provider.chainId)

        assertEquals("https://alpha4-2.starknet.io/feeder_gateway", testnet2Provider.feederGatewayUrl)
        assertEquals("https://alpha4-2.starknet.io/gateway", testnet2Provider.gatewayUrl)
        assertEquals(StarknetChainId.TESTNET2, testnet2Provider.chainId)

        assertThrows(IllegalArgumentException::class.java) {
            GatewayProvider.makeTestnetProvider(StarknetChainId.MAINNET)
        }
    }

    @Test
    fun `make specific testnet gateway client with custom httpservice`() {
        // starknet --gateway_url http://127.0.0.1:5050/ --feeder_gateway_url http://127.0.0.1:5050/ get_block
        // Modified block number to something outrageously big to verify private httpService
        val blockNumber = 123456789
        val httpService = mock<HttpService> {
            on { send(any()) } doReturn HttpResponse(
                true,
                200,
                """
                    {
                        "block_hash": "0x0",
                        "block_number": 123456789,
                        "gas_price": "0x174876e800",
                        "parent_block_hash": "0x0",
                        "sequencer_address": "0x3711666a3506c99c9d78c4d4013409a87a962b7a0880a1c24af9fe193dafc01",
                        "starknet_version": "0.10.3",
                        "state_root": "0000000000000000000000000000000000000000000000000000000000000000",
                        "status": "ACCEPTED_ON_L2",
                        "timestamp": 1675079650,
                        "transaction_receipts": [],
                        "transactions": []
                    }
                """.trimIndent(),
            )
        }

        val testnet1Provider = GatewayProvider.makeTestnetProvider(StarknetChainId.TESTNET, httpService)
        val testnet2Provider = GatewayProvider.makeTestnetProvider(StarknetChainId.TESTNET2, httpService)

        assertEquals("https://alpha4.starknet.io/feeder_gateway", testnet1Provider.feederGatewayUrl)
        assertEquals("https://alpha4.starknet.io/gateway", testnet1Provider.gatewayUrl)
        assertEquals(StarknetChainId.TESTNET, testnet1Provider.chainId)

        val t1Block = testnet1Provider.getBlockNumber()
        val t1Response = t1Block.send()
        assertEquals(blockNumber, t1Response)

        assertEquals("https://alpha4-2.starknet.io/feeder_gateway", testnet2Provider.feederGatewayUrl)
        assertEquals("https://alpha4-2.starknet.io/gateway", testnet2Provider.gatewayUrl)
        assertEquals(StarknetChainId.TESTNET2, testnet2Provider.chainId)

        val t2Block = testnet2Provider.getBlockNumber()
        val t2Response = t2Block.send()
        assertEquals(blockNumber, t2Response)

        assertThrows(IllegalArgumentException::class.java) {
            GatewayProvider.makeTestnetProvider(StarknetChainId.MAINNET)
        }
    }

    @Test
    fun `make gateway mainnet client`() {
        val defaultProvider = GatewayProvider.makeMainnetProvider()
        assertEquals("https://alpha-mainnet.starknet.io/feeder_gateway", defaultProvider.feederGatewayUrl)
        assertEquals("https://alpha-mainnet.starknet.io/gateway", defaultProvider.gatewayUrl)
        assertEquals(StarknetChainId.MAINNET, defaultProvider.chainId)

        val httpService = mock<HttpService>()
        val withHttpProvider = GatewayProvider.makeMainnetProvider(httpService)
        assertEquals("https://alpha-mainnet.starknet.io/feeder_gateway", withHttpProvider.feederGatewayUrl)
        assertEquals("https://alpha-mainnet.starknet.io/gateway", withHttpProvider.gatewayUrl)
        assertEquals(StarknetChainId.MAINNET, withHttpProvider.chainId)
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

    @Test
    fun `get class definition at latest block`() {
        // FIXME: Devnet only support's calls with block_id of the latest or pending. Other block_id are not supported.
        // After it's fixed add tests with 1) block hash 2) block number
        val provider = rpcProvider()
        val request = provider.getClass(classHash, BlockTag.LATEST)
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
        // Mocked response of GOL2 test contract deploy
        // Fetched from testnet using
        // starknet get_transaction_receipt --hash 0x01a9d9e311ff31e27b20a7919bec6861dd6b603d72b7e8df9900cd7603200d0b
        val httpService = mock<HttpService> {
            on { send(any()) } doReturn HttpResponse(
                true,
                200,
                """
                    {
                        "actual_fee": "0x0",
                        "block_hash": "0x4e782152c52c8637e03df60048deb4f6adf122ef37cf53eeb72322a4b9c9c52",
                        "block_number": 264715,
                        "events": [],
                        "execution_resources": {
                            "builtin_instance_counter": {
                                "bitwise_builtin": 0,
                                "ecdsa_builtin": 0,
                                "output_builtin": 0,
                                "pedersen_builtin": 0,
                                "range_check_builtin": 0
                            },
                            "n_memory_holes": 0,
                            "n_steps": 41
                        },
                        "l2_to_l1_messages": [],
                        "status": "ACCEPTED_ON_L1",
                        "transaction_hash": "0x1a9d9e311ff31e27b20a7919bec6861dd6b603d72b7e8df9900cd7603200d0b",
                        "transaction_index": 8
                    }
                """.trimIndent(),
            )
        }
        val provider = GatewayProvider("", "", StarknetChainId.TESTNET, httpService)

        val request = provider.getTransactionReceipt(Felt.ZERO)
        val response = request.send()

        assertTrue(response is GatewayTransactionReceipt)
    }

    @Test
    fun `get deploy transaction receipt rpc`() {
        val httpService = mock<HttpService> {
            on { send(any()) } doReturn HttpResponse(
                true,
                200,
                """
                    {
                        "id": 0,
                        "jsonrpc": "2.0",
                        "result":
                        {
                            "actual_fee": "0x0",
                            "block_hash": "0x4e782152c52c8637e03df60048deb4f6adf122ef37cf53eeb72322a4b9c9c52",
                            "contract_address": "0x20f8c63faff27a0c5fe8a25dc1635c40c971bf67b8c35c6089a998649dfdfcb",
                            "transaction_hash": "0x1a9d9e311ff31e27b20a7919bec6861dd6b603d72b7e8df9900cd7603200d0b",
                            "status": "ACCEPTED_ON_L1",
                            "block_number": 264715,
                            "type": "DEPLOY",
                            "events":
                            [],
                            "messages_sent":
                            []
                        }
                    }
                """.trimIndent(),
            )
        }
        val provider = JsonRpcProvider(devnetClient.rpcUrl, StarknetChainId.TESTNET, httpService)

        val request = provider.getTransactionReceipt(Felt.ZERO)
        val response = request.send()

        assertTrue(response is DeployRpcTransactionReceipt)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get declare transaction receipt`(provider: Provider) {
        val request = provider.getTransactionReceipt(declareTransactionHash)
        val response = request.send()

        assertNotNull(response)

        if (provider is GatewayProvider) {
            assertTrue(response is GatewayTransactionReceipt)
        } else if (provider is JsonRpcProvider) {
            assertTrue(response is RpcTransactionReceipt)
        }
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get invoke transaction receipt`(provider: Provider) {
        val request = provider.getTransactionReceipt(invokeTransactionHash)
        val response = request.send()

        assertNotNull(response)

        if (provider is GatewayProvider) {
            assertTrue(response is GatewayTransactionReceipt)
        } else if (provider is JsonRpcProvider) {
            assertTrue(response is RpcTransactionReceipt)
        }
    }

    @Test
    fun `get l1 handler transaction receipt gateway`() {
        // Fetched from testnet using
        // starknet get_transaction_receipt --hash 0x4b2ff971b669e31c704fde5c1ad6ee08ba2000986a25ad5106ab94546f36f7
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

    @Test
    fun `get l1 handler transaction receipt rpc`() {
        val httpService = mock<HttpService> {
            on { send(any()) } doReturn HttpResponse(
                true,
                200,
                """
                    {
                        "id": 0,
                        "jsonrpc": "2.0",
                        "result": {
                            "transaction_hash": "0x4b2ff971b669e31c704fde5c1ad6ee08ba2000986a25ad5106ab94546f36f7",
                            "actual_fee": "0x0",
                            "status": "ACCEPTED_ON_L2",
                            "block_hash": "0x16c6bc59271e7b727ac0b139bbf99336fec1c0bfb6d41540d36fe1b3e2994c9",
                            "block_number": 392723,
                            "type": "L1_HANDLER",
                            "messages_sent": [],
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
                            ]
                        }
                    }
                """.trimIndent(),
            )
        }
        val provider = JsonRpcProvider(devnetClient.rpcUrl, StarknetChainId.TESTNET, httpService)

        val request = provider.getTransactionReceipt(Felt.fromHex("0x4b2ff971b669e31c704fde5c1ad6ee08ba2000986a25ad5106ab94546f36f7"))
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is RpcTransactionReceipt)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get transaction receipt throws on incorrect hash`(provider: Provider) {
        val request = provider.getTransactionReceipt(Felt.ZERO)
        assertThrows(RequestFailedException::class.java) {
            request.send()
        }
    }

    @Test
    fun `get deploy transaction gateway`() {
        // Mocked response of GOL2 test contract deploy
        // Fetched from testnet using
        // starknet get_transaction --hash 0x01a9d9e311ff31e27b20a7919bec6861dd6b603d72b7e8df9900cd7603200d0b
        val httpService = mock<HttpService> {
            on { send(any()) } doReturn HttpResponse(
                true,
                200,
                """
                    {
                        "block_hash": "0x4e782152c52c8637e03df60048deb4f6adf122ef37cf53eeb72322a4b9c9c52",
                        "block_number": 264715,
                        "status": "ACCEPTED_ON_L1",
                        "transaction": {
                            "class_hash": "0xc6529e402abdc91cc51f61ae71df2337bc6c535fd96eb79235b1a32bbc6fdc",
                            "constructor_calldata": [
                                "0x12d3ad59161fd2a72d5bc8501bb2f2ca1acd34706d2dfa31a90aadb4b41e050"
                            ],
                            "contract_address": "0x20f8c63faff27a0c5fe8a25dc1635c40c971bf67b8c35c6089a998649dfdfcb",
                            "contract_address_salt": "0x204c6f0c41cb17c5cb5ef00e46952a6679123185c6f9443dae6e31ae042d119",
                            "transaction_hash": "0x1a9d9e311ff31e27b20a7919bec6861dd6b603d72b7e8df9900cd7603200d0b",
                            "type": "DEPLOY",
                            "version": "0x0"
                        },
                        "transaction_index": 8
                    }
                """.trimIndent(),
            )
        }
        val provider = GatewayProvider("", "", StarknetChainId.TESTNET, httpService)

        val request = provider.getTransaction(Felt.ZERO)
        val response = request.send()

        assertTrue(response is DeployTransaction)
    }

    @Test
    fun `get deploy transaction rpc`() {
        val httpService = mock<HttpService> {
            on { send(any()) } doReturn HttpResponse(
                true,
                200,
                """
                    {
                        "id": 0,
                        "jsonrpc": "2.0",
                        "result":
                        {
                            "class_hash": "0xc6529e402abdc91cc51f61ae71df2337bc6c535fd96eb79235b1a32bbc6fdc",
                            "constructor_calldata":
                            [
                                "0x12d3ad59161fd2a72d5bc8501bb2f2ca1acd34706d2dfa31a90aadb4b41e050"
                            ],
                            "contract_address_salt": "0x204c6f0c41cb17c5cb5ef00e46952a6679123185c6f9443dae6e31ae042d119",
                            "transaction_hash": "0x1a9d9e311ff31e27b20a7919bec6861dd6b603d72b7e8df9900cd7603200d0b",
                            "type": "DEPLOY",
                            "version": "0x0"
                        }
                    }
                """.trimIndent(),
            )
        }
        val provider = JsonRpcProvider(devnetClient.rpcUrl, StarknetChainId.TESTNET, httpService)

        val request = provider.getTransaction(Felt.ZERO)
        val response = request.send()

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
    fun `get deploy account transaction`(provider: Provider) {
        val tx = provider.getTransaction(deployAccountTransactionHash).send()

        assertTrue(tx is DeployAccountTransaction)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get deploy account transaction receipt`(provider: Provider) {
        val receipt = provider.getTransactionReceipt(deployAccountTransactionHash).send()

        if (provider is GatewayProvider) {
            assertTrue(receipt is GatewayTransactionReceipt)
        } else if (provider is JsonRpcProvider) {
            assertTrue(receipt is DeployRpcTransactionReceipt)
        }
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get declare transaction`(provider: Provider) {
        val request = provider.getTransaction(declareTransactionHash)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is DeclareTransaction)
    }

    @Test
    fun `get l1 handler transaction gateway`() {
        // Fetched from testnet using
        // starknet get_transaction --hash 0x7e1ed66dbccf915857c6367fc641c24292c063e54a5dd55947c2d958d94e1a9
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

    @Test
    fun `get l1 handler transaction rpc`() {
        val httpService = mock<HttpService> {
            on { send(any()) } doReturn HttpResponse(
                true,
                200,
                """
                    {
                        "id": 0,
                        "jsonrpc": "2.0",
                        "result": {
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
        val provider = JsonRpcProvider(devnetClient.rpcUrl, StarknetChainId.TESTNET, httpService)

        val request = provider.getTransaction(Felt.ZERO)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is L1HandlerTransaction)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get transaction throws on incorrect hash`(provider: Provider) {
        val request = provider.getTransaction(Felt.ZERO)
        assertThrows(RequestFailedException::class.java) {
            request.send()
        }
    }

    @Test
    fun `get transaction throws serialization exception on invalid transaction format`() {
        val httpService = mock<HttpService> {
            on { send(any()) } doReturn HttpResponse(
                true,
                200,
                "{\"not\": \"a\", \"transaction\": false}",
            )
        }
        val provider = GatewayProvider("", "", StarknetChainId.TESTNET, httpService)
        val request = provider.getTransaction(Felt.ZERO)
        assertThrows(SerializationException::class.java) {
            request.send()
        }
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
        assertEquals("Class with hash 0x0 is not declared.", exception.message)
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
        assertThrows(Cairo0ContractDefinition.InvalidContractException::class.java) {
            Cairo0ContractDefinition("{}")
        }
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get event`(provider: Provider) {
        if (provider !is JsonRpcProvider) {
            return
        }
        val key = listOf(Felt(BigInteger("1693986747384444883019945263944467198055030340532126334167406248528974657031")))

        val request = provider.getEvents(
            GetEventsPayload(
                fromBlockId = BlockId.Hash(Felt.ZERO),
                toBlockId = BlockId.Tag(BlockTag.LATEST),
                address = contractAddress,
                keys = listOf(key),
                chunkSize = 10,
            ),
        )
        val response = request.send()

        assertNotNull(response)

        val event = response.events[0]
        assertEquals(contractAddress, event.address)
        assertEquals(key[0], event.keys[0])
        assertEquals(Felt.ZERO, event.data[0])
        assertEquals(invokeTransactionHash, event.transactionHash)
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

    @Test
    fun `get block transaction count with block tag for testnet2`() {
        val provider = GatewayProvider(devnetClient.feederGatewayUrl, devnetClient.gatewayUrl, StarknetChainId.TESTNET2)
        val blockTransactionCount = provider.getBlockTransactionCount(BlockTag.LATEST)
        val response = blockTransactionCount.send()

        assertNotNull(response)
        assertEquals(1, response)
        assertTrue(provider.chainId == StarknetChainId.TESTNET2)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get block transaction count with block hash`(provider: Provider) {
        val blockTransactionCount = provider.getBlockTransactionCount(Felt.fromHex("0x0"))
        val response = blockTransactionCount.send()

        assertNotNull(response)
        assertEquals(17, response)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get block transaction count with block id`(provider: Provider) {
        val blockTransactionCount = provider.getBlockTransactionCount(0)
        val response = blockTransactionCount.send()

        assertNotNull(response)
        assertEquals(17, response)
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
                "starting_block_num": "0x0",
                "current_block_hash": "0x1",
                "current_block_num": "0x1",
                "highest_block_hash": "0x9",
                "highest_block_num": "0xA"
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
        assertEquals(Felt.fromHex("0x9"), response.highestBlockHash)
        assertEquals(10, response.highestBlockNumber)
    }

    @Test
    fun `received gateway receipt`() {
        val hash = Felt.fromHex("0x334da4f63cc6309ba2429a70f103872ab0ae82cf8d9a73b845184a4713cada5")
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
        val provider = GatewayProvider.makeTestnetProvider(httpService)
        val receipt = provider.getTransactionReceipt(hash).send() as GatewayTransactionReceipt

        assertEquals(hash, receipt.hash)
        assertEquals(TransactionStatus.PENDING, receipt.status)
        assertEquals(emptyList<GatewayMessageL2ToL1>(), receipt.messagesL2ToL1)
        assertEquals(emptyList<Event>(), receipt.events)
        assertNull(receipt.messageL1ToL2)
        assertNull(receipt.actualFee)
        assertNull(receipt.failureReason)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get nonce with block tag`(provider: Provider) {
        val request = provider.getNonce(contractAddress, BlockTag.LATEST)
        val response = request.send()

        assertNotNull(response)
    }

    @Test
    fun `get nonce with block number`() {
        val provider = rpcProvider()
        val request = provider.getNonce(contractAddress, latestBlock.number)
        val response = request.send()

        assertNotNull(response)
    }

    @Test
    fun `get nonce with block hash`() {
        val provider = rpcProvider()
        val request = provider.getNonce(contractAddress, latestBlock.hash)
        val response = request.send()

        assertNotNull(response)
    }

    @Test
    fun `get pending block with transactions`() {
        // FIXME: We should also test for 'pending' tag, but atm they are not supported in devnet
        val mockedResponse = """
            {
                "id":0,
                "jsonrpc":"2.0",
                "result":{
                    "parent_hash": "0x123",
                    "timestamp": 7312,
                    "sequencer_address": "0x1234",
                    "transactions": [
                        {
                            "transaction_hash": "0x01",
                            "class_hash": "0x98",
                            "version": "0x0",
                            "type": "DEPLOY",
                            "max_fee": "0x1",
                            "signature": [],
                            "nonce": "0x1",
                            "contract_address_salt": "0x0",
                            "constructor_calldata": []
                        },
                        {
                            "transaction_hash": "0x02",
                            "class_hash": "0x99",
                            "version": "0x1",
                            "max_fee": "0x1",
                            "type": "DECLARE",
                            "sender_address": "0x15",
                            "signature": [],
                            "nonce": "0x1"
                        } 
                    ]
                }
            }
        """.trimIndent()
        val httpService = mock<HttpService> {
            on { send(any()) } doReturn HttpResponse(true, 200, mockedResponse)
        }
        val provider = JsonRpcProvider(devnetClient.rpcUrl, StarknetChainId.TESTNET, httpService)
        val request = provider.getBlockWithTxs(BlockTag.PENDING)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is PendingBlockWithTransactionsResponse)
    }

    @Test
    fun `get block with transactions with block tag`() {
        val provider = rpcProvider()
        val request = provider.getBlockWithTxs(BlockTag.LATEST)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is BlockWithTransactionsResponse)
    }

    @Test
    fun `get block with transactions with block hash`() {
        val provider = rpcProvider()
        val request = provider.getBlockWithTxs(latestBlock.hash)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is BlockWithTransactionsResponse)
    }

    @Test
    fun `get block with transactions with block number`() {
        val provider = rpcProvider()
        val request = provider.getBlockWithTxs(latestBlock.number)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is BlockWithTransactionsResponse)
    }

    @Test
    fun `get state of block with latest tag`() {
        val provider = rpcProvider()
        val request = provider.getStateUpdate(BlockTag.LATEST)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is StateUpdateResponse)
    }

    @Test
    fun `get state of block with pending tag`() {
        val mockedResponse = """
            {
                "id":0,
                "jsonrpc":"2.0",
                "result":{
                    "old_root":"0x0",
                    "state_diff":{
                        "declared_classes":[],
                        "deployed_contracts":[],
                        "deprecated_declared_classes":[],
                        "nonces":[],
                        "replaced_classes":[],
                        "storage_diffs":[]
                    }
                }
            }
        """.trimIndent()
        val httpService = mock<HttpService> {
            on { send(any()) } doReturn HttpResponse(true, 200, mockedResponse)
        }
        val provider = JsonRpcProvider(devnetClient.rpcUrl, StarknetChainId.TESTNET, httpService)
        val request = provider.getStateUpdate(BlockTag.PENDING)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is PendingStateUpdateResponse)
    }

    @Test
    fun `get state of block with hash`() {
        val provider = rpcProvider()
        val request = provider.getStateUpdate(latestBlock.hash)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is StateUpdateResponse)
    }

    @Test
    fun `get state of block with number`() {
        val provider = rpcProvider()
        val request = provider.getStateUpdate(latestBlock.number)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is StateUpdateResponse)
    }

    @Test
    fun `get transactions by block tag and index`() {
        val provider = rpcProvider()
        val request = provider.getTransactionByBlockIdAndIndex(BlockTag.LATEST, 0)
        val response = request.send()

        assertNotNull(response)
    }

    @Test
    fun `get transactions by block hash and index`() {
        val provider = rpcProvider()
        val request = provider.getTransactionByBlockIdAndIndex(latestBlock.hash, 0)
        val response = request.send()

        assertNotNull(response)
    }

    @Test
    fun `get transactions by block number and index`() {
        val provider = rpcProvider()
        val request = provider.getTransactionByBlockIdAndIndex(latestBlock.number, 0)
        val response = request.send()

        assertNotNull(response)
    }

    @Test
    fun `get pending transactions`() {
        val mockedResponse = """
        {
            "id": 0,
            "jsonrpc": "2.0",
            "result": [
                {
                    "transaction_hash": "0x01",
                    "class_hash": "0x98",
                    "version": "0x0",
                    "type": "DEPLOY",
                    "max_fee": "0x1",
                    "signature": [],
                    "nonce": "0x1",
                    "contract_address_salt": "0x0",
                    "constructor_calldata": []
                },
                {
                    "transaction_hash": "0x02",
                    "class_hash": "0x99",
                    "version": "0x1",
                    "max_fee": "0x1",
                    "type": "DECLARE",
                    "sender_address": "0x15",
                    "signature": [],
                    "nonce": "0x1"
                }
            ]
        }
        """.trimIndent()
        val httpService = mock<HttpService> {
            on { send(any()) } doReturn HttpResponse(true, 200, mockedResponse)
        }
        val provider = JsonRpcProvider(devnetClient.rpcUrl, StarknetChainId.TESTNET, httpService)
        val request = provider.getPendingTransactions()
        val response = request.send()

        assertNotNull(response)
    }
}
