package starknet.provider

import com.swmansion.starknet.data.selectorFromName
import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.provider.exceptions.RequestFailedException
import com.swmansion.starknet.provider.exceptions.RpcRequestFailedException
import com.swmansion.starknet.provider.rpc.JsonRpcProvider
import com.swmansion.starknet.service.http.HttpResponse
import com.swmansion.starknet.service.http.HttpService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import starknet.utils.DevnetClient
import java.nio.file.Paths

class ProviderTest {
    companion object {
        private val devnetClient = DevnetClient(
            port = 5052,
            accountDirectory = Paths.get("src/test/resources/accounts/provider_test"),
            contractsDirectory = Paths.get("src/test/resources/contracts"),
        )

        private val rpcUrl = devnetClient.rpcUrl
        private val provider = JsonRpcProvider(rpcUrl)

        private lateinit var balanceContractAddress: Felt
        private lateinit var balanceClassHash: Felt
        private lateinit var invokeTransactionHash: Felt
        private lateinit var declareTransactionHash: Felt
        private lateinit var deployAccountTransactionHash: Felt

        @JvmStatic
        @BeforeAll
        fun before() {
            try {
                devnetClient.start()

                // Prepare devnet address book
                val deployAccountResult = devnetClient.createDeployAccount()
                val declareResult = devnetClient.declareContract("Balance")
                balanceClassHash = declareResult.classHash
                declareTransactionHash = declareResult.transactionHash
                balanceContractAddress = devnetClient.deployContract(
                    classHash = balanceClassHash,
                    constructorCalldata = listOf(Felt(451)),
                ).contractAddress
                deployAccountTransactionHash = deployAccountResult.transactionHash
                invokeTransactionHash = devnetClient.invokeContract(
                    contractAddress = balanceContractAddress,
                    function = "increase_balance",
                    calldata = listOf(Felt(10)),
                ).transactionHash
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
    fun getSpecVersion() {
        val request = provider.getSpecVersion()
        val specVersion = request.send().value

        assertNotEquals(0, specVersion.length)
        val validPattern = "\\d+\\.\\d+\\.\\d+".toRegex()
        assertTrue(validPattern.containsMatchIn(specVersion))
    }

    @Test
    fun getTransactionStatus() {
        val transactionHash = invokeTransactionHash
        val transactionStatus = provider.getTransactionStatus(transactionHash).send()

        assertEquals(TransactionStatus.ACCEPTED_ON_L2, transactionStatus.finalityStatus)
        assertNotNull(transactionStatus.executionStatus)
        assertEquals(TransactionExecutionStatus.SUCCEEDED, transactionStatus.executionStatus)
    }

    @Test
    fun getMessagesStatus() {
        val mockedResponse = """
        {
            "id": 0,
            "jsonrpc": "2.0",
            "result": [
                {
                    "transaction_hash": "0x123",
                    "finality_status": "ACCEPTED_ON_L2"
                },
                {
                    "transaction_hash": "0x123",
                    "finality_status": "ACCEPTED_ON_L2",
                    "failure_reason": "Example failure reason"
                }
            ]
        }
        """.trimIndent()

        val httpService = mock<HttpService> {
            on { send(any()) } doReturn HttpResponse(true, 200, mockedResponse)
        }
        val provider = JsonRpcProvider(rpcUrl, httpService)
        val request = provider.getMessagesStatus(NumAsHex(0x123))
        val response = request.send()

        assertEquals(2, response.values.count())

        assertEquals(Felt(0x123), response.values[0].transactionHash)
        assertEquals(TransactionStatus.ACCEPTED_ON_L2, response.values[0].finalityStatus)
        assertNull(response.values[0].failureReason)

        assertEquals(Felt(0x123), response.values[1].transactionHash)
        assertEquals(TransactionStatus.ACCEPTED_ON_L2, response.values[1].finalityStatus)
        assertNotNull(response.values[1].failureReason)
    }

    @Test
    fun callContractWithBlockNumber() {
        val currentNumber = provider.getBlockNumber().send().value

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

    @Test
    fun callContractWithBlockHash() {
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

    @Test
    fun callContractWithBlockTag() {
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

    @Test
    fun getStorageAt() {
        val request = provider.getStorageAt(
            contractAddress = balanceContractAddress,
            key = selectorFromName("balance"),
            blockTag = BlockTag.LATEST,
        )

        val response = request.send()
        assertTrue(response >= Felt(10))
    }

    @Test
    fun getStorageAtWithKeyAsString() {
        val request = provider.getStorageAt(
            contractAddress = balanceContractAddress,
            key = "balance",
            blockTag = BlockTag.LATEST,
        )

        val response = request.send()
        assertTrue(response >= Felt(10))
    }

    @Test
    fun getClassDefinitionAtClassHash() {
        val request = provider.getClass(balanceClassHash)
        val response = request.send()

        // Note to future developers:
        // This test assumes that balance contract is written in Cairo 1
        assertTrue(response is ContractClass)
    }

    @Test
    fun getClassDefinitionAtClassHashWithLatestBlock() {
        // FIXME: Devnet only support's calls with block_id of the latest or pending. Other block_id are not supported.
        // After it's fixed add tests with 1) block hash 2) block number
        val request = provider.getClass(balanceClassHash, BlockTag.LATEST)
        val response = request.send()

        // This test assumes that balance contract is written in Cairo 1
        assertTrue(response is ContractClass)
    }

    @Test
    fun getClassDefinitionAtClassHashWithBlockNumber() {
        val blockNumber = provider.getBlockNumber().send().value

        val request = provider.getClass(balanceClassHash, blockNumber)
        val response = request.send()

        // This test assumes that balance contract is written in Cairo 1
        assertTrue(response is ContractClass)
    }

    @Test
    fun getClassDefinitionAtClassHashWithBlockHash() {
        val blockHash = provider.getBlockHashAndNumber().send().blockHash

        val request = provider.getClass(balanceClassHash, blockHash)
        val response = request.send()

        // This test assumes that balance contract is written in Cairo 1
        assertTrue(response is ContractClass)
    }

    @Test
    fun getClassDefinitionAtContractAddress() {
        val request = provider.getClassAt(balanceContractAddress)
        val response = request.send()

        // This test assumes that balance contract is written in Cairo 1
        assertTrue(response is ContractClass)
    }

    @Test
    fun getClassDefinitionAtContractAddressWithBlockHash() {
        val blockHash = provider.getBlockHashAndNumber().send().blockHash

        val request = provider.getClassAt(balanceContractAddress, blockHash)
        val response = request.send()

        // This test assumes that balance contract is written in Cairo 1
        assertTrue(response is ContractClass)
    }

    @Test
    fun getClassDefinitionAtContractAddressWithBlockNumber() {
        val blockNumber = provider.getBlockNumber().send().value

        val request = provider.getClassAt(balanceContractAddress, blockNumber)
        val response = request.send()

        // This test assumes that balance contract is written in Cairo 1
        assertTrue(response is ContractClass)
    }

    @Test
    fun getClassDefinitionAtContractAddressWithLatestBlock() {
        val request = provider.getClassAt(balanceContractAddress, BlockTag.LATEST)
        val response = request.send()

        // This test assumes that balance contract is written in Cairo 1
        assertTrue(response is ContractClass)
    }

    @Test
    fun `get class definition at contract address (pending block tag)`() {
        val request = provider.getClassAt(balanceContractAddress, BlockTag.PENDING)
        val response = request.send()

        // This test assumes that balance contract is written in Cairo 1
        assertTrue(response is ContractClass)
    }

    @Test
    fun getClassHash() {
        val request = provider.getClassHashAt(balanceContractAddress)
        val response = request.send()

        assertNotEquals(Felt.ZERO, response)
    }

    @Test
    fun getClassHashAtPendingBlock() {
        val request = provider.getClassHashAt(balanceContractAddress, BlockTag.PENDING)
        val response = request.send()

        assertNotEquals(Felt.ZERO, response)
    }

    @Test
    fun `get class hash at latest block`() {
        val request = provider.getClassHashAt(balanceContractAddress, BlockTag.LATEST)
        val response = request.send()

        assertNotEquals(Felt.ZERO, response)
    }

    @Test
    fun `get class hash at block hash`() {
        val blockHash = provider.getBlockHashAndNumber().send().blockHash

        val request = provider.getClassHashAt(balanceContractAddress, blockHash)
        val response = request.send()

        assertNotEquals(Felt.ZERO, response)
    }

    @Test
    fun `get class hash at block number`() {
        val blockNumber = provider.getBlockNumber().send().value

        val request = provider.getClassHashAt(balanceContractAddress, blockNumber)
        val response = request.send()

        assertNotEquals(Felt.ZERO, response)
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
                            "actual_fee": {
                                "amount": "0x244adfc7e22",
                                "unit": "WEI"
                            },
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
                            [],
                            "execution_resources": 
                            {
                                "l1_gas": "123",
                                "l1_data_gas": "456",
                                "l2_gas": "789"
                            }
                        }
                    }
                """.trimIndent(),
            )
        }
        val provider = JsonRpcProvider(rpcUrl, httpService)

        val request = provider.getTransactionReceipt(Felt.ZERO)
        val response = request.send()

        assertTrue(response is DeployTransactionReceipt)
        assertFalse(response.isPending)
        assertTrue(response.hasBlockInfo)
    }

    @Test
    fun `get declare transaction receipt`() {
        val request = provider.getTransactionReceipt(declareTransactionHash)
        val response = request.send()

        assertTrue(response is DeclareTransactionReceipt)
        assertFalse(response.isPending)
        assertTrue(response.hasBlockInfo)
    }

    @Test
    fun getInvokeTransactionReceipt() {
        val request = provider.getTransactionReceipt(invokeTransactionHash)
        val response = request.send()

        assertTrue(response is InvokeTransactionReceipt)
        assertFalse(response.isPending)
        assertTrue(response.hasBlockInfo)
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
                "actual_fee": {
                    "amount": "0x244adfc7e22",
                    "unit": "FRI"
                },
                "messages_sent": [],
                "events": [],
                "execution_status": "SUCCEEDED",
                "finality_status": "ACCEPTED_ON_L2",
                "execution_resources": 
                {
                    "l1_gas": "123",
                    "l1_data_gas": "456",
                    "l2_gas": "789"
                }
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

        val provider = JsonRpcProvider(rpcUrl, httpService)
        val receipt = provider.getTransactionReceipt(Felt.fromHex("0x333198614194ae5b5ef921e63898a592de5e9f4d7b6e04745093da88b429f2a")).send()

        assertTrue(receipt is InvokeTransactionReceipt)
        assertTrue(receipt.isPending)
        assertEquals(PriceUnit.FRI, receipt.actualFee.unit)
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
                            "actual_fee": {
                                "amount": "0x244adfc7e22",
                                "unit": "WEI"
                            },
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
                            ],
                            "execution_resources": 
                            {
                                "l1_gas": "123",
                                "l1_data_gas": "456",
                                "l2_gas": "789"
                            },
                            "message_hash": "0x8000000000000110000000000000000000000000000000000000011111111111"
                        }
                    }
                """.trimIndent(),
            )
        }
        val provider = JsonRpcProvider(rpcUrl, httpService)

        val request = provider.getTransactionReceipt(Felt.fromHex("0x4b2ff971b669e31c704fde5c1ad6ee08ba2000986a25ad5106ab94546f36f7"))
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is L1HandlerTransactionReceipt)
        assertTrue(response.hasBlockInfo)
        assertFalse(response.isPending)
        assertEquals(PriceUnit.WEI, response.actualFee.unit)
    }

    @Test
    fun `get transaction receipt throws on incorrect hash`() {
        val request = provider.getTransactionReceipt(Felt.ZERO)
        assertThrows(RequestFailedException::class.java) {
            request.send()
        }
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
        val provider = JsonRpcProvider(rpcUrl, httpService)

        val request = provider.getTransaction(Felt.ZERO)
        val response = request.send()

        assertTrue(response is DeployTransaction)
    }

    @Test
    fun getInvokeTransaction() {
        val request = provider.getTransaction(invokeTransactionHash)
        val response = request.send()

        assertTrue(response is InvokeTransaction)
    }

    @Test
    fun `get deploy account transaction`() {
        val response = provider.getTransaction(deployAccountTransactionHash).send()

        assertTrue(response is DeployAccountTransaction)
    }

    @Test
    fun `get deploy account transaction receipt`() {
        val receipt = provider.getTransactionReceipt(deployAccountTransactionHash).send()
        assertTrue(receipt is DeployAccountTransactionReceipt)
        assertTrue(receipt.hasBlockInfo)
        assertFalse(receipt.isPending)
    }

    @Test
    fun `get declare transaction`() {
        val request = provider.getTransaction(declareTransactionHash)
        val response = request.send()

        assertTrue(response is DeclareTransaction)
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
        val provider = JsonRpcProvider(rpcUrl, httpService)

        val request = provider.getTransaction(Felt.ZERO)
        val response = request.send()

        assertTrue(response is L1HandlerTransaction)
    }

    @Test
    fun `get transaction throws on incorrect hash`() {
        val request = provider.getTransaction(Felt.ZERO)
        assertThrows(RequestFailedException::class.java) {
            request.send()
        }
    }

    @Test
    fun `rpc provider throws RpcRequestFailedException`() {
        val request = provider.getClassAt(Felt(0), BlockTag.LATEST)

        val exception = assertThrows(RpcRequestFailedException::class.java) {
            request.send()
        }
        assertEquals(20, exception.code)
        assertEquals("Contract not found", exception.message)
    }

    @Test
    fun `make contract definition with invalid json`() {
        assertThrows(Cairo0ContractDefinition.InvalidContractException::class.java) {
            Cairo0ContractDefinition("{}")
        }
    }

    @Test
    fun getEvents() {
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

    @Test
    fun getCurrentBlockNumber() {
        val request = provider.getBlockNumber()
        val response = request.send()

        assertNotEquals(0, response)
    }

    @Test
    fun getCurrentBlockHashAndNumber() {
        val request = provider.getBlockHashAndNumber()
        val response = request.send()

        val blockNumber = response.blockNumber
        val blockHash = response.blockHash

        assertNotEquals(0, blockNumber)
        assertNotEquals(Felt.ZERO, blockHash)

        val getBlockResponse = provider.getBlockWithTxHashes(blockNumber).send() as ProcessedBlockWithTransactionHashes
        val expectedHash = getBlockResponse.blockHash

        assertEquals(expectedHash, blockHash)
    }

    @Test
    fun getBlockTransactionCountWithBlockTag() {
        val request = provider.getBlockTransactionCount(BlockTag.LATEST)
        val response = request.send()

        assertNotEquals(0, response)
    }

    @Test
    fun getBlockTransactionCountWithBlockHash() {
        val blockHash = provider.getBlockHashAndNumber().send().blockHash

        val request = provider.getBlockTransactionCount(blockHash)
        val response = request.send()

        assertNotEquals(0, response)
    }

    @Test
    fun getBlockTransactionCountWithBlockNumber() {
        val request = provider.getBlockTransactionCount(1)
        val response = request.send()

        assertNotEquals(0, response)
    }

    @Test
    fun getSyncInformationNodeNotSyncing() {
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
        val provider = JsonRpcProvider(rpcUrl, httpService)
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
    fun getChainId() {
        val request = provider.getChainId()
        val response = request.send()

        assertEquals(StarknetChainId.SEPOLIA, response)
    }

    @Test
    fun getNonceWithBlockTag() {
        val request = provider.getNonce(balanceContractAddress, BlockTag.LATEST)
        val response = request.send()

        assertNotNull(response)
    }

    @Test
    fun getNonceWithBlockNumber() {
        val blockNumber = provider.getBlockNumber().send().value

        val request = provider.getNonce(balanceContractAddress, blockNumber)
        val response = request.send()

        assertNotNull(response)
    }

    @Test
    fun getNonceWithBlockHash() {
        val blockHash = provider.getBlockHashAndNumber().send().blockHash

        val request = provider.getNonce(balanceContractAddress, blockHash)
        val response = request.send()

        assertNotNull(response)
    }

    @Test
    fun getStorageProof() {
        val mockedResponse = """
        {
            "id": 0,
            "jsonrpc": "2.0",
            "result": {
                "classes_proof": [
                    {"node": {"left": "0x123", "right": "0x123"}, "node_hash": "0x123"},
                    {
                        "node": {"child": "0x123", "length": 2, "path": "0x123"},
                        "node_hash": "0x123"
                    }
                ],
                "contracts_proof": {
                    "contract_leaves_data": [
                        {"class_hash": "0x123", "nonce": "0x0", "storage_root": "0x123"}
                    ],
                    "nodes": [
                        {
                            "node": {"left": "0x123", "right": "0x123"},
                            "node_hash": "0x123"
                        },
                        {
                            "node": {"child": "0x123", "length": 232, "path": "0x123"},
                            "node_hash": "0x123"
                        }
                    ]
                },
                "contracts_storage_proofs": [
                    [
                        {
                            "node": {"left": "0x123", "right": "0x123"},
                            "node_hash": "0x123"
                        },
                        {
                            "node": {"child": "0x123", "length": 123, "path": "0x123"},
                            "node_hash": "0x123"
                        },
                        {
                            "node": {"left": "0x123", "right": "0x123"},
                            "node_hash": "0x123"
                        }
                    ]
                ],
                "global_roots": {
                    "block_hash": "0x123",
                    "classes_tree_root": "0x456",
                    "contracts_tree_root": "0x789"
                }
            }
        }
        """.trimIndent()

        val httpService = mock<HttpService> {
            on { send(any()) } doReturn HttpResponse(true, 200, mockedResponse)
        }
        val provider = JsonRpcProvider(rpcUrl, httpService)

        val request = provider.getStorageProof(
            blockId = BlockId.Number(0),
        )
        val response = request.send()

        assertNotNull(response)

        assertTrue(response.classesProof[0].node is NodeHashToNodeMappingItem.BinaryNode)
        assertTrue(response.classesProof[1].node is NodeHashToNodeMappingItem.EdgeNode)
        assertTrue(response.contractsProof.nodes[0].node is NodeHashToNodeMappingItem.BinaryNode)
        assertTrue(response.contractsStorageProofs[0][0].node is NodeHashToNodeMappingItem.BinaryNode)

        assertEquals(2, response.classesProof.size)
        assertEquals(2, response.contractsProof.nodes.size)
        assertEquals(1, response.contractsStorageProofs.size)
        assertEquals(Felt.fromHex("0x123"), response.globalRoots.blockHash)
        assertEquals(Felt.fromHex("0x456"), response.globalRoots.classesTreeRoot)
        assertEquals(Felt.fromHex("0x789"), response.globalRoots.contractsTreeRoot)
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
                    "l1_gas_price": 
                    {
                        "price_in_wei": "0x2137",
                        "price_in_fri": "0x1234"
                    },
                    "l2_gas_price":
                    {
                        "price_in_wei": "0x123",
                        "price_in_fri": "0x456"
                    },
                    "l1_data_gas_price":
                    {
                        "price_in_wei": "0x789",
                        "price_in_fri": "0x123"
                    },
                    "l1_da_mode": "BLOB",
                    "starknet_version": "0.13.1",
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
        val provider = JsonRpcProvider(rpcUrl, httpService)

        val request = provider.getBlockWithTxs(BlockTag.PENDING)
        val response = request.send()

        assertTrue(response is PendingBlockWithTransactions)
    }

    @Test
    fun getBlockWithTransactionsWithBlockTag() {
        val request = provider.getBlockWithTxs(BlockTag.LATEST)
        val response = request.send()

        assertTrue(response is ProcessedBlockWithTransactions)
        val block = response as ProcessedBlockWithTransactions
        assertEquals(BlockStatus.ACCEPTED_ON_L2, block.status)
    }

    @Test
    fun getBlockWithTransactionsWithBlockHash() {
        val blockHash = provider.getBlockHashAndNumber().send().blockHash

        val request = provider.getBlockWithTxs(blockHash)
        val response = request.send()

        assertTrue(response is ProcessedBlockWithTransactions)
    }

    @Test
    fun getBlockWithTransactionsWithBlockNumber() {
        val blockNumber = provider.getBlockNumber().send().value

        val request = provider.getBlockWithTxs(blockNumber)
        val response = request.send()

        assertTrue(response is ProcessedBlockWithTransactions)
    }

    @Test
    fun `get pending block with transaction receipts`() {
        // TODO (#304): We should also test for 'pending' tag, but atm they are not supported in devnet
        val mockedResponse = """
            {
                "id":0,
                "jsonrpc":"2.0",
                "result":{
                    "parent_hash": "0x123",
                    "timestamp": 7312,
                    "sequencer_address": "0x1234",
                    "l1_gas_price": 
                    {
                        "price_in_wei": "0x2137",
                        "price_in_fri": "0x1234"
                    },
                    "l2_gas_price":
                    {
                        "price_in_wei": "0x123",
                        "price_in_fri": "0x456"
                    },
                    "l1_data_gas_price":
                    {
                        "price_in_wei": "0x789",
                        "price_in_fri": "0x123"
                    },
                    "l1_da_mode": "BLOB",
                    "starknet_version": "0.13.1",
                    "transactions": [
                        {
                            "transaction": 
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
                            "receipt": 
                            {
                                "actual_fee": {
                                    "amount": "0x244adfc7e22",
                                    "unit": "WEI"
                                },
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
                                [],
                                "execution_resources": 
                                {
                                    "l1_gas": "123",
                                    "l1_data_gas": "456",
                                    "l2_gas": "789"
                                }
                            }
                        },
                        {
                            "transaction": 
                            {
                                "transaction_hash": "0x02",
                                "class_hash": "0x99",
                                "version": "0x1",
                                "max_fee": "0x1",
                                "type": "DECLARE",
                                "sender_address": "0x15",
                                "signature": [],
                                "nonce": "0x1"
                            },
                            "receipt": 
                            {
                                "actual_fee": {
                                    "amount": "0x244adfc7e22",
                                    "unit": "WEI"
                                },
                                "block_hash": "0x4e782152c52c8637e03df60048deb4f6adf122ef37cf53eeb72322a4b9c9c52",
                                "transaction_hash": "0x1a9d9e311ff31e27b20a7919bec6861dd6b603d72b7e8df9900cd7603200d0b",
                                "finality_status": "ACCEPTED_ON_L1",
                                "execution_status": "SUCCEEDED",
                                "block_number": 264715,
                                "type": "DECLARE",
                                "events":
                                [],
                                "messages_sent":
                                [],
                                "execution_resources": 
                                {
                                    "l1_gas": "123",
                                    "l1_data_gas": "456",
                                    "l2_gas": "789"
                                }
                            }
                        } 
                    ]
                }
            }
        """.trimIndent()
        val httpService = mock<HttpService> {
            on { send(any()) } doReturn HttpResponse(true, 200, mockedResponse)
        }
        val provider = JsonRpcProvider(rpcUrl, httpService)

        val request = provider.getBlockWithReceipts(BlockTag.PENDING)
        val response = request.send()

        assertTrue(response is PendingBlockWithReceipts)
    }

    @Test
    fun getBlockWithTransactionReceiptsWithBlockTag() {
        val request = provider.getBlockWithReceipts(BlockTag.LATEST)
        val response = request.send()

        assertTrue(response is ProcessedBlockWithReceipts)
        val block = response as ProcessedBlockWithReceipts
        assertEquals(BlockStatus.ACCEPTED_ON_L2, block.status)
    }

    @Test
    fun getBlockWithTransactionReceiptsWithBlockHash() {
        val blockHash = provider.getBlockHashAndNumber().send().blockHash

        val request = provider.getBlockWithReceipts(blockHash)
        val response = request.send()

        assertTrue(response is ProcessedBlockWithReceipts)
    }

    @Test
    fun getBlockWithTransactionReceiptsWithBlockNumber() {
        val blockNumber = provider.getBlockNumber().send().value

        val request = provider.getBlockWithReceipts(blockNumber)
        val response = request.send()

        assertTrue(response is ProcessedBlockWithReceipts)
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
                    "l1_gas_price": 
                    {
                        "price_in_wei": "0x2137",
                        "price_in_fri": "0x1234"
                    },
                    "l2_gas_price":
                    {
                        "price_in_wei": "0x123",
                        "price_in_fri": "0x456"
                    },
                    "l1_data_gas_price":
                    {
                        "price_in_wei": "0x789",
                        "price_in_fri": "0x123"
                    },
                    "l1_da_mode": "BLOB",
                    "starknet_version": "0.13.1",
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
        val provider = JsonRpcProvider(rpcUrl, httpService)

        val request = provider.getBlockWithTxHashes(BlockTag.PENDING)
        val response = request.send()

        assertTrue(response is PendingBlockWithTransactionHashes)
    }

    @Test
    fun getBlockWithTransactionHashesWithBlockTag() {
        val request = provider.getBlockWithTxHashes(BlockTag.LATEST)
        val response = request.send()

        assertTrue(response is ProcessedBlockWithTransactionHashes)
        val block = response as ProcessedBlockWithTransactionHashes
        assertEquals(BlockStatus.ACCEPTED_ON_L2, block.status)
    }

    @Test
    fun getBlockWithTransactionHashesWithBlockHash() {
        val blockHash = provider.getBlockHashAndNumber().send().blockHash

        val request = provider.getBlockWithTxHashes(blockHash)
        val response = request.send()

        assertTrue(response is ProcessedBlockWithTransactionHashes)
    }

    @Test
    fun getBlockWithTransactionHashesWithBlockNumber() {
        val blockNumber = provider.getBlockNumber().send().value

        val request = provider.getBlockWithTxHashes(blockNumber)
        val response = request.send()

        assertTrue(response is ProcessedBlockWithTransactionHashes)
    }

    @Test
    fun getStateOfBlockWithLatestTag() {
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
        val provider = JsonRpcProvider(rpcUrl, httpService)

        val request = provider.getStateUpdate(BlockTag.PENDING)
        val response = request.send()

        assertTrue(response is PendingStateUpdateResponse)
    }

    @Test
    fun getStateOfBlockWithHash() {
        val blockHash = provider.getBlockHashAndNumber().send().blockHash

        val request = provider.getStateUpdate(blockHash)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is StateUpdateResponse)
    }

    @Test
    fun getStateOfBlockWithNumber() {
        val blockNumber = provider.getBlockNumber().send().value

        val request = provider.getStateUpdate(blockNumber)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is StateUpdateResponse)
    }

    @Test
    fun getTransactionByBlockTagAndIndex() {
        val request = provider.getTransactionByBlockIdAndIndex(BlockTag.LATEST, 0)
        val response = request.send()

        assertNotNull(response)
    }

    @Test
    fun getTransactionByBlockHashAndIndex() {
        val blockHash = provider.getBlockHashAndNumber().send().blockHash

        val request = provider.getTransactionByBlockIdAndIndex(blockHash, 0)
        val response = request.send()

        assertNotNull(response)
    }

    @Test
    fun getTransactionByBlockNumberAndIndex() {
        val blockNumber = provider.getBlockNumber().send().value

        val request = provider.getTransactionByBlockIdAndIndex(blockNumber, 0)
        val response = request.send()

        assertNotNull(response)
    }

    @Test
    fun `batch call contract with block hash and block tag`() {
        val call1 = Call(
            contractAddress = balanceContractAddress,
            entrypoint = "get_balance",
            calldata = emptyList(),
        )
        val blockHash = provider.getBlockHashAndNumber().send().blockHash

        val call2 = Call(
            contractAddress = balanceContractAddress,
            entrypoint = "get_balance",
            calldata = emptyList(),
        )

        val callRequests = listOf(
            provider.callContract(
                call = call1,
                blockHash = blockHash,
            ),
            provider.callContract(
                call = call2,
                blockTag = BlockTag.LATEST,
            ),
        )
        val request = provider.batchRequests(callRequests)
        val response = request.send()
        val expectedBalance = provider.getStorageAt(balanceContractAddress, selectorFromName("balance"), BlockTag.LATEST).send()

        assertEquals(response[0].getOrThrow().first(), expectedBalance)
        assertEquals(response[1].getOrThrow().first(), expectedBalance)
    }

    @Test
    fun batchGetTransactions() {
        // docsStart
        val blockNumber = provider.getBlockNumber().send().value
        val request = provider.batchRequests(
            provider.getTransactionByBlockIdAndIndex(blockNumber, 0),
            provider.getTransaction(invokeTransactionHash),
            provider.getTransaction(declareTransactionHash),
            provider.getTransaction(deployAccountTransactionHash),

        )

        val response = request.send()
        // docsEnd
        assertEquals(response[0].getOrThrow().hash, invokeTransactionHash)
        assertEquals(response[1].getOrThrow().hash, invokeTransactionHash)
        assertEquals(response[2].getOrThrow().hash, declareTransactionHash)
        assertEquals(response[3].getOrThrow().hash, deployAccountTransactionHash)
    }

    @Test
    fun batchRequestsAny() {
        // docsStart
        val request = provider.batchRequestsAny(
            provider.getTransaction(invokeTransactionHash),
            provider.getBlockNumber(),
            provider.getTransactionStatus(invokeTransactionHash),
        )

        val response = request.send()

        val transaction = response[0].getOrThrow() as Transaction
        val blockNumber = (response[1].getOrThrow() as IntResponse).value
        val txStatus = response[2].getOrThrow() as GetTransactionStatusResponse
        // docsEnd
        assertEquals(transaction.hash, invokeTransactionHash)

        assertNotEquals(0, blockNumber)

        assertEquals(TransactionStatus.ACCEPTED_ON_L2, txStatus.finalityStatus)
        assertEquals(TransactionExecutionStatus.SUCCEEDED, txStatus.executionStatus)
    }
}
