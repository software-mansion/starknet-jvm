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
import starknet.utils.LegacyDevnetClient
import java.nio.file.Path
import java.nio.file.Paths

class ProviderTest {
    companion object {
        @JvmStatic
        private val legacyDevnetClient = LegacyDevnetClient(
            port = 5052,
            accountDirectory = Paths.get("src/test/resources/accounts_legacy/provider_test"),
        )
        private val devnetClient = DevnetClient(
            port = 5062,
            accountDirectory = Paths.get("src/test/resources/accounts/provider_test"),
            contractsDirectory = Paths.get("src/test/resources/contracts"),
        )
        private val rpcProvider = JsonRpcProvider(
            devnetClient.rpcUrl,
            StarknetChainId.TESTNET,
        )
        private val legacyGatewayProvider = GatewayProvider(
            legacyDevnetClient.feederGatewayUrl,
            legacyDevnetClient.gatewayUrl,
            StarknetChainId.TESTNET,
        )
        private lateinit var legacyDevnetAddressBook: AddressBook
        private lateinit var devnetAddressBook: AddressBook

        @JvmStatic
        @BeforeAll
        fun before() {
            try {
                devnetClient.start()
                legacyDevnetClient.start()

                // Prepare devnet address book
                val declareResult = devnetClient.declareContract("Balance")
                val balanceClassHash = declareResult.classHash
                val declareTransactionHash = declareResult.transactionHash
                val balanceContractAddress = devnetClient.deployContract(
                    classHash = balanceClassHash,
                    constructorCalldata = listOf(Felt(451)),
                ).contractAddress
                val deployAccountTransactionHash = devnetClient.createDeployAccount("provider_test").transactionHash
                val invokeTransactionHash = devnetClient.invokeContract(
                    contractAddress = balanceContractAddress,
                    function = "increase_balance",
                    calldata = listOf(Felt(10)),
                ).transactionHash
                devnetAddressBook = AddressBook(
                    balanceContractAddress = balanceContractAddress,
                    balanceClassHash = balanceClassHash,
                    invokeTransactionHash = invokeTransactionHash,
                    declareTransactionHash = declareTransactionHash,
                    deployAccountTransactionHash = deployAccountTransactionHash,
                )

                // Prepare legacy devnet address book
                val legacyBalanceContractAddress = legacyDevnetClient.deployContract(Path.of("src/test/resources/contracts_v0/target/release/providerTest.json")).address
                val legacyInvokeTransactionHash = legacyDevnetClient.invokeTransaction(
                    "increase_balance",
                    legacyBalanceContractAddress,
                    Path.of("src/test/resources/contracts_v0/target/release/providerTestAbi.json"),
                    listOf(Felt(10)),
                ).transactionHash
                val (legacyBalanceClassHash, legacyDeclareTransactionHash) = legacyDevnetClient.declareContract(Path.of("src/test/resources/contracts_v0/target/release/providerTest.json"))
                val legacyDeployAccountTransactionHash = legacyDevnetClient.deployAccount().transactionHash
                legacyDevnetAddressBook = AddressBook(
                    balanceContractAddress = legacyBalanceContractAddress,
                    balanceClassHash = legacyBalanceClassHash,
                    invokeTransactionHash = legacyInvokeTransactionHash,
                    declareTransactionHash = legacyDeclareTransactionHash,
                    deployAccountTransactionHash = legacyDeployAccountTransactionHash,
                )
            } catch (ex: Exception) {
                devnetClient.close()
                legacyDevnetClient.close()
                throw ex
            }
        }

        @JvmStatic
        @AfterAll
        fun after() {
            devnetClient.close()
            legacyDevnetClient.close()
        }

        data class AddressBook(
            val balanceContractAddress: Felt,
            val balanceClassHash: Felt,
            val invokeTransactionHash: Felt,
            val declareTransactionHash: Felt,
            val deployAccountTransactionHash: Felt,
        )

        data class ProviderParameters(
            val provider: Provider,
            val addressBook: AddressBook,
        )

        @JvmStatic
        fun getProviders(): List<ProviderParameters> {
            return listOf(
                ProviderParameters(
                    legacyGatewayProvider,
                    legacyDevnetAddressBook,
                ),
                ProviderParameters(
                    rpcProvider,
                    devnetAddressBook,
                ),
            )
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
    fun `make testnet gateway client with custom httpservice`() {
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

        val testnetProvider = GatewayProvider.makeTestnetProvider(httpService)

        assertEquals("https://alpha4.starknet.io/feeder_gateway", testnetProvider.feederGatewayUrl)
        assertEquals("https://alpha4.starknet.io/gateway", testnetProvider.gatewayUrl)
        assertEquals(StarknetChainId.TESTNET, testnetProvider.chainId)

        val block = testnetProvider.getBlockNumber()
        val response = block.send()
        assertEquals(blockNumber, response)
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
    fun `call contract with block number`(providerParameters: ProviderParameters) {
        val provider = providerParameters.provider
        val balanceContractAddress = providerParameters.addressBook.balanceContractAddress

        val currentNumber = provider.getBlockNumber().send()

        val call = Call(
            contractAddress = balanceContractAddress,
            entrypoint = "get_balance",
            calldata = emptyList(),
        )
        val request = provider.callContract(
            call = call,
            blockNumber = currentNumber,
        )
        val response = request.send()
        val balance = response.first()

        val expectedBalance = provider.getStorageAt(balanceContractAddress, selectorFromName("balance"), BlockTag.LATEST).send()

        assertEquals(expectedBalance, balance)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `call contract with block hash`(providerParameters: ProviderParameters) {
        val provider = providerParameters.provider
        val balanceContractAddress = providerParameters.addressBook.balanceContractAddress

        val call = Call(
            contractAddress = balanceContractAddress,
            entrypoint = "get_balance",
            calldata = emptyList(),
        )
        val blockHash = provider.getBlockHashAndNumber().send().blockHash

        val request = provider.callContract(
            call = call,
            blockHash = blockHash,
        )
        val response = request.send()
        val balance = response.first()

        val expectedBalance = provider.getStorageAt(balanceContractAddress, selectorFromName("balance"), BlockTag.LATEST).send()

        assertEquals(expectedBalance, balance)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `call contract with block tag`(providerParameters: ProviderParameters) {
        val provider = providerParameters.provider
        val balanceContractAddress = providerParameters.addressBook.balanceContractAddress

        val call = Call(
            contractAddress = balanceContractAddress,
            entrypoint = "get_balance",
            calldata = emptyList(),
        )

        val request = provider.callContract(
            call = call,
            blockTag = BlockTag.LATEST,
        )
        val response = request.send()
        val balance = response.first()

        val expectedBalance = provider.getStorageAt(balanceContractAddress, selectorFromName("balance"), BlockTag.LATEST).send()

        assertEquals(expectedBalance, balance)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get storage at`(providerParameters: ProviderParameters) {
        val provider = providerParameters.provider
        val balanceContractAddress = providerParameters.addressBook.balanceContractAddress

        val request = provider.getStorageAt(
            contractAddress = balanceContractAddress,
            key = selectorFromName("balance"),
            blockTag = BlockTag.LATEST,
        )

        val response = request.send()
        assertTrue(response >= Felt(10))
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get class definition at class hash`(providerParameters: ProviderParameters) {
        val balanceClassHash = providerParameters.addressBook.balanceClassHash

        val provider = providerParameters.provider
        val request = provider.getClass(balanceClassHash)
        val response = request.send()

        // Note to future developers:
        // This test assumes that balance contract is written in:
        // Cairo 1 - for JsonRpcProvider, Cairo 0 - for GatewayProvider
        when (provider) {
            is JsonRpcProvider -> assertTrue(response is ContractClass)
            is GatewayProvider -> assertTrue(response is DeprecatedContractClass)
            else -> throw IllegalStateException("Unknown provider type")
        }
    }

    @Test
    fun `get class definition at class hash (latest block)`() {
        val balanceClassHash = devnetAddressBook.balanceClassHash

        // FIXME: Devnet only support's calls with block_id of the latest or pending. Other block_id are not supported.
        // After it's fixed add tests with 1) block hash 2) block number
        val provider = rpcProvider
        val request = provider.getClass(balanceClassHash, BlockTag.LATEST)
        val response = request.send()

        // This test assumes that balance contract is written in Cairo 1
        assertTrue(response is ContractClass)
    }

    @Test
    fun `get class definition at class hash (block number)`() {
        val provider = rpcProvider
        val balanceClassHash = devnetAddressBook.balanceClassHash

        val blockNumber = provider.getBlockNumber().send()

        val request = provider.getClass(balanceClassHash, blockNumber)
        val response = request.send()

        // This test assumes that balance contract is written in Cairo 1
        assertTrue(response is ContractClass)
    }

    @Test
    fun `get class definition at class hash (block hash)`() {
        val provider = rpcProvider
        val balanceClassHash = devnetAddressBook.balanceClassHash

        val blockHash = provider.getBlockHashAndNumber().send().blockHash

        val request = provider.getClass(balanceClassHash, blockHash)
        val response = request.send()

        // This test assumes that balance contract is written in Cairo 1
        assertTrue(response is ContractClass)
    }

    @Test
    fun `get class definition at contract address`() {
        val provider = rpcProvider
        val balanceContractAddress = devnetAddressBook.balanceContractAddress

        val request = provider.getClassAt(balanceContractAddress)
        val response = request.send()

        // This test assumes that balance contract is written in Cairo 1
        assertTrue(response is ContractClass)
    }

    @Test
    fun `get class definition at contract address (block hash)`() {
        val provider = rpcProvider
        val balanceContractAddress = devnetAddressBook.balanceContractAddress

        val blockHash = provider.getBlockHashAndNumber().send().blockHash

        val request = provider.getClassAt(balanceContractAddress, blockHash)
        val response = request.send()

        // This test assumes that balance contract is written in Cairo 1
        assertTrue(response is ContractClass)
    }

    @Test
    fun `get class definition at contract address (block number)`() {
        val provider = rpcProvider
        val balanceContractAddress = devnetAddressBook.balanceContractAddress

        val blockNumber = provider.getBlockNumber().send()

        val request = provider.getClassAt(balanceContractAddress, blockNumber)
        val response = request.send()

        // This test assumes that balance contract is written in Cairo 1
        assertTrue(response is ContractClass)
    }

    @Test
    fun `get class definition at contract address (latest block tag)`() {
        val provider = rpcProvider
        val balanceContractAddress = devnetAddressBook.balanceContractAddress

        val request = provider.getClassAt(balanceContractAddress, BlockTag.LATEST)
        val response = request.send()

        // This test assumes that balance contract is written in Cairo 1
        assertTrue(response is ContractClass)
    }

    @Test
    fun `get class definition at contract address (pending block tag)`() {
        val provider = rpcProvider
        val balanceContractAddress = devnetAddressBook.balanceContractAddress

        val request = provider.getClassAt(balanceContractAddress, BlockTag.PENDING)
        val response = request.send()

        // This test assumes that balance contract is written in Cairo 1
        assertTrue(response is ContractClass)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get class hash`(providerParameters: ProviderParameters) {
        val provider = providerParameters.provider
        val balanceContractAddress = providerParameters.addressBook.balanceContractAddress

        val request = provider.getClassHashAt(balanceContractAddress)
        val response = request.send()

        assertNotEquals(Felt.ZERO, response)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get class hash at pending block`(providerParameters: ProviderParameters) {
        val provider = providerParameters.provider
        val balanceContractAddress = providerParameters.addressBook.balanceContractAddress

        val request = provider.getClassHashAt(balanceContractAddress, BlockTag.PENDING)
        val response = request.send()

        assertNotEquals(Felt.ZERO, response)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get class hash at latest block`(providerParameters: ProviderParameters) {
        val provider = providerParameters.provider
        val balanceContractAddress = providerParameters.addressBook.balanceContractAddress

        val request = provider.getClassHashAt(balanceContractAddress, BlockTag.LATEST)
        val response = request.send()

        assertNotEquals(Felt.ZERO, response)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get class hash at block hash`(providerParameters: ProviderParameters) {
        val provider = providerParameters.provider
        val balanceContractAddress = providerParameters.addressBook.balanceContractAddress

        val blockHash = provider.getBlockHashAndNumber().send().blockHash

        val request = provider.getClassHashAt(balanceContractAddress, blockHash)
        val response = request.send()

        assertNotEquals(Felt.ZERO, response)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get class hash at block number`(providerParameters: ProviderParameters) {
        val provider = providerParameters.provider
        val balanceContractAddress = providerParameters.addressBook.balanceContractAddress

        val blockNumber = provider.getBlockNumber().send()

        val request = provider.getClassHashAt(balanceContractAddress, blockNumber)
        val response = request.send()

        assertNotEquals(Felt.ZERO, response)
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
                        "messages_sent": [],
                        "status": "ACCEPTED_ON_L1",
                        "finality_status": "ACCEPTED_ON_L1",
                        "execution_status": "SUCCEEDED",
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
                            "finality_status": "ACCEPTED_ON_L1",
                            "execution_status": "SUCCEEDED",
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
        val provider = JsonRpcProvider(legacyDevnetClient.rpcUrl, StarknetChainId.TESTNET, httpService)

        val request = provider.getTransactionReceipt(Felt.ZERO)
        val response = request.send()

        assertTrue(response is ProcessedDeployRpcTransactionReceipt)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get declare transaction receipt`(providerParameters: ProviderParameters) {
        val provider = providerParameters.provider
        val declareTransactionHash = providerParameters.addressBook.declareTransactionHash

        val request = provider.getTransactionReceipt(declareTransactionHash)
        val response = request.send()

        when (provider) {
            is GatewayProvider -> assertTrue(response is GatewayTransactionReceipt)
            is JsonRpcProvider -> assertTrue(response is ProcessedRpcTransactionReceipt)
            else -> throw IllegalStateException("Unknown provider type")
        }
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get invoke transaction receipt`(providerParameters: ProviderParameters) {
        val provider = providerParameters.provider
        val invokeTransactionHash = providerParameters.addressBook.invokeTransactionHash

        val request = provider.getTransactionReceipt(invokeTransactionHash)
        val response = request.send()

        when (provider) {
            is GatewayProvider -> assertTrue(response is GatewayTransactionReceipt)
            is JsonRpcProvider -> assertTrue(response is ProcessedRpcTransactionReceipt)
            else -> throw IllegalStateException("Unknown provider type")
        }
    }

    @Test
    fun `get pending invoke transaction receipt rpc`() {
        val mockedResponse =
            """
        {
            "id": 0,
            "jsonrpc": "2.0",
            "result": {
                "type": "INVOKE",
                "transaction_hash": "0x333198614194ae5b5ef921e63898a592de5e9f4d7b6e04745093da88b429f2a",
                "actual_fee": "0x244adfc7e22",
                "messages_sent": [],
                "events": [],
                "execution_status": "SUCCEEDED",
                "finality_status": "ACCEPTED_ON_L2"
            }
        }
            """.trimIndent()

        val httpService = mock<HttpService> {
            on { send(any()) } doReturn HttpResponse(
                isSuccessful = true,
                code = 200,
                body = mockedResponse,
            )
        }

        val provider = JsonRpcProvider(devnetClient.rpcUrl, StarknetChainId.TESTNET, httpService)
        val receipt = provider.getTransactionReceipt(Felt.fromHex("0x333198614194ae5b5ef921e63898a592de5e9f4d7b6e04745093da88b429f2a")).send()

        assertTrue(receipt is PendingRpcTransactionReceipt)
        assertFalse(receipt is ProcessedRpcTransactionReceipt)
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
                    "finality_status": "ACCEPTED_ON_L2",
                    "execution_status": "SUCCEEDED",
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
        val gatewayResponse = response as GatewayTransactionReceipt
        assertNotNull(gatewayResponse.messageL1ToL2)
        assertNotNull(gatewayResponse.messageL1ToL2!!.nonce)
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
                            "finality_status": "ACCEPTED_ON_L2",
                            "execution_status": "SUCCEEDED",
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
        assertTrue(response is ProcessedRpcTransactionReceipt)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get transaction receipt throws on incorrect hash`(providerParameters: ProviderParameters) {
        val provider = providerParameters.provider
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
    fun `get invoke transaction`(providerParameters: ProviderParameters) {
        val provider = providerParameters.provider
        val invokeTransactionHash = providerParameters.addressBook.invokeTransactionHash

        val request = provider.getTransaction(invokeTransactionHash)
        val response = request.send()

        assertTrue(response is InvokeTransaction)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get deploy account transaction`(providerParameters: ProviderParameters) {
        val provider = providerParameters.provider
        val deployAccountTransactionHash = providerParameters.addressBook.deployAccountTransactionHash

        val response = provider.getTransaction(deployAccountTransactionHash).send()

        assertTrue(response is DeployAccountTransaction)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get deploy account transaction receipt`(providerParameters: ProviderParameters) {
        val provider = providerParameters.provider
        val deployAccountTransactionHash = providerParameters.addressBook.deployAccountTransactionHash

        val receipt = provider.getTransactionReceipt(deployAccountTransactionHash).send()

        if (provider is GatewayProvider) {
            assertTrue(receipt is GatewayTransactionReceipt)
        } else if (provider is JsonRpcProvider) {
            assertTrue(receipt is ProcessedDeployRpcTransactionReceipt)
        }
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get declare transaction`(providerParameters: ProviderParameters) {
        val provider = providerParameters.provider
        val declareTransactionHash = providerParameters.addressBook.declareTransactionHash

        val request = provider.getTransaction(declareTransactionHash)
        val response = request.send()

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

        assertTrue(response is L1HandlerTransaction)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get transaction throws on incorrect hash`(providerParameters: ProviderParameters) {
        val provider = providerParameters.provider

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
        val request = rpcProvider.getClassAt(Felt(0), BlockTag.LATEST)

        val exception = assertThrows(RpcRequestFailedException::class.java) {
            request.send()
        }
        assertEquals(20, exception.code)
        assertEquals("Contract not found", exception.message)
    }

    @Test
    fun `gateway provider throws GatewayRequestFailedException`() {
        val request = legacyGatewayProvider.getClass(Felt(0))

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

    @Test
    fun `get event`() {
        val provider = rpcProvider
        val eventsContractAddress = devnetClient.declareDeployContract("Events").contractAddress

        val key = listOf(Felt.fromHex("0x477e157efde59c5531277ede78acb3e03ef69508c6c35fde3495aa0671d227"))
        val invokeTransactionHash = devnetClient.invokeContract(
            contractAddress = eventsContractAddress,
            function = "emit_event",
            calldata = listOf(Felt.ONE), //  0 - static event, 1 - incremental event
        ).transactionHash

        val request = provider.getEvents(
            GetEventsPayload(
                fromBlockId = BlockId.Number(0),
                toBlockId = BlockId.Tag(BlockTag.LATEST),
                address = eventsContractAddress,
                keys = listOf(key),
                chunkSize = 10,
            ),
        )
        val response = request.send()
        assertTrue(response.events.isNotEmpty())

        val event = response.events[0]

        assertEquals(eventsContractAddress, event.address)
        assertEquals(key[0], event.keys[0])
        assertEquals(Felt.ZERO, event.data[0])
        assertEquals(invokeTransactionHash, event.transactionHash)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get current block number`(providerParameters: ProviderParameters) {
        val provider = providerParameters.provider

        val request = provider.getBlockNumber()
        val response = request.send()

        assertNotEquals(0, response)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get current block number and hash`(providerParameters: ProviderParameters) {
        val provider = providerParameters.provider

        val request = provider.getBlockHashAndNumber()
        val response = request.send()

        val blockNumber = response.blockNumber
        val blockHash = response.blockHash

        assertNotEquals(0, blockNumber)
        assertNotEquals(Felt.ZERO, blockHash)

        val expectedHash = when (provider) {
            is JsonRpcProvider -> {
                val getBlockResponse = provider.getBlockWithTxHashes(blockNumber).send() as BlockWithTransactionHashesResponse
                getBlockResponse.blockHash
            }
            is GatewayProvider -> {
                legacyDevnetClient.getBlock(blockNumber).blockHash
            }
            else -> throw IllegalStateException("Unknown provider type")
        }
        assertEquals(expectedHash, blockHash)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get block transaction count with block tag`(providerParameters: ProviderParameters) {
        val provider = providerParameters.provider

        val request = provider.getBlockTransactionCount(BlockTag.LATEST)
        val response = request.send()

        assertNotEquals(0, response)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get block transaction count with block hash`(providerParameters: ProviderParameters) {
        val provider = providerParameters.provider

        val blockHash = provider.getBlockHashAndNumber().send().blockHash

        val request = provider.getBlockTransactionCount(blockHash)
        val response = request.send()

        assertNotEquals(0, response)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get block transaction count with block number`(providerParameters: ProviderParameters) {
        val provider = providerParameters.provider

        val request = provider.getBlockTransactionCount(0)
        val response = request.send()

        assertNotEquals(0, response)
    }

    @Test
    fun `get sync information node not syncing`() {
        val provider = rpcProvider

        val request = provider.getSyncing()
        val response = request.send()

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
                    "finality_status": "RECEIVED",
                    "execution_status": "SUCCEEDED",
                    "transaction_hash": "0x334da4f63cc6309ba2429a70f103872ab0ae82cf8d9a73b845184a4713cada5", 
                    "messages_sent": [], 
                    "events": []
                }
                """.trimIndent(),
            )
        }
        val provider = GatewayProvider.makeTestnetProvider(httpService)
        val receipt = provider.getTransactionReceipt(hash).send() as GatewayTransactionReceipt

        assertEquals(hash, receipt.hash)
        assertEquals(TransactionStatus.PENDING, receipt.status)
        assertEquals(emptyList<MessageL2ToL1>(), receipt.messagesSent)
        assertEquals(emptyList<Event>(), receipt.events)
        assertNull(receipt.messageL1ToL2)
        assertNull(receipt.actualFee)
        assertNull(receipt.failureReason)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get nonce with block tag`(providerParameters: ProviderParameters) {
        val provider = providerParameters.provider
        val balanceContractAddress = providerParameters.addressBook.balanceContractAddress

        val request = provider.getNonce(balanceContractAddress, BlockTag.LATEST)
        val response = request.send()

        assertNotNull(response)
    }

    @Test
    fun `get nonce with block number`() {
        val provider = rpcProvider
        val balanceContractAddress = devnetAddressBook.balanceContractAddress

        val blockNumber = provider.getBlockNumber().send()

        val request = provider.getNonce(balanceContractAddress, blockNumber)
        val response = request.send()

        assertNotNull(response)
    }

    @Test
    fun `get nonce with block hash`() {
        val provider = rpcProvider
        val balanceContractAddress = devnetAddressBook.balanceContractAddress

        val blockHash = provider.getBlockHashAndNumber().send().blockHash

        val request = provider.getNonce(balanceContractAddress, blockHash)
        val response = request.send()

        assertNotNull(response)
    }

    @Test
    fun `get pending block with transactions`() {
        // TODO (#304): We should also test for 'pending' tag, but atm they are not supported in devnet
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

        assertTrue(response is PendingBlockWithTransactionsResponse)
    }

    @Test
    fun `get block with transactions with block tag`() {
        val provider = rpcProvider

        val request = provider.getBlockWithTxs(BlockTag.LATEST)
        val response = request.send()

        assertTrue(response is BlockWithTransactionsResponse)
    }

    @Test
    fun `get block with transactions with block hash`() {
        val provider = rpcProvider

        val blockHash = provider.getBlockHashAndNumber().send().blockHash

        val request = provider.getBlockWithTxs(blockHash)
        val response = request.send()

        assertTrue(response is BlockWithTransactionsResponse)
    }

    @Test
    fun `get block with transactions with block number`() {
        val provider = rpcProvider

        val blockNumber = provider.getBlockNumber().send()

        val request = provider.getBlockWithTxs(blockNumber)
        val response = request.send()

        assertTrue(response is BlockWithTransactionsResponse)
    }

    @Test
    fun `get pending block with transaction hashes`() {
        // TODO (#304): We should also test for 'pending' tag, but atm they are not supported in devnet
        val mockedResponse = """
            {
                "id":0,
                "jsonrpc":"2.0",
                "result":{
                    "parent_hash": "0x123",
                    "timestamp": 7312,
                    "sequencer_address": "0x1234",
                    "transactions": [
                        "0x01",
                        "0x02"
                    ]
                }
            }
        """.trimIndent()
        val httpService = mock<HttpService> {
            on { send(any()) } doReturn HttpResponse(true, 200, mockedResponse)
        }
        val provider = JsonRpcProvider(devnetClient.rpcUrl, StarknetChainId.TESTNET, httpService)

        val request = provider.getBlockWithTxHashes(BlockTag.PENDING)
        val response = request.send()

        assertTrue(response is PendingBlockWithTransactionHashesResponse)
    }

    @Test
    fun `get block with transaction hashes with block tag`() {
        val provider = rpcProvider

        val request = provider.getBlockWithTxHashes(BlockTag.LATEST)
        val response = request.send()

        assertTrue(response is BlockWithTransactionHashesResponse)
    }

    @Test
    fun `get block with transaction hashes with block hash`() {
        val provider = rpcProvider

        val blockHash = provider.getBlockHashAndNumber().send().blockHash

        val request = provider.getBlockWithTxHashes(blockHash)
        val response = request.send()

        assertTrue(response is BlockWithTransactionHashesResponse)
    }

    @Test
    fun `get block with transaction hashes with block number`() {
        val provider = rpcProvider

        val blockNumber = provider.getBlockNumber().send()

        val request = provider.getBlockWithTxHashes(blockNumber)
        val response = request.send()

        assertTrue(response is BlockWithTransactionHashesResponse)
    }

    @Test
    fun `get state of block with latest tag`() {
        val provider = rpcProvider

        val request = provider.getStateUpdate(BlockTag.LATEST)
        val response = request.send()

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

        assertTrue(response is PendingStateUpdateResponse)
    }

    @Test
    fun `get state of block with hash`() {
        val provider = rpcProvider

        val blockHash = provider.getBlockHashAndNumber().send().blockHash

        val request = provider.getStateUpdate(blockHash)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is StateUpdateResponse)
    }

    @Test
    fun `get state of block with number`() {
        val provider = rpcProvider

        val blockNumber = provider.getBlockNumber().send()

        val request = provider.getStateUpdate(blockNumber)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is StateUpdateResponse)
    }

    @Test
    fun `get transactions by block tag and index`() {
        val provider = rpcProvider

        val request = provider.getTransactionByBlockIdAndIndex(BlockTag.LATEST, 0)
        val response = request.send()

        assertNotNull(response)
    }

    @Test
    fun `get transactions by block hash and index`() {
        val provider = rpcProvider

        val blockHash = provider.getBlockHashAndNumber().send().blockHash

        val request = provider.getTransactionByBlockIdAndIndex(blockHash, 0)
        val response = request.send()

        assertNotNull(response)
    }

    @Test
    fun `get transactions by block number and index`() {
        val provider = rpcProvider

        val blockNumber = provider.getBlockNumber().send()

        val request = provider.getTransactionByBlockIdAndIndex(blockNumber, 0)
        val response = request.send()

        assertNotNull(response)
    }
}
