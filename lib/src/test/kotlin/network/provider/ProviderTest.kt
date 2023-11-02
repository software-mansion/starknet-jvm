package network.provider

import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.data.types.transactions.*
import com.swmansion.starknet.provider.Provider
import com.swmansion.starknet.provider.gateway.GatewayProvider
import com.swmansion.starknet.provider.rpc.JsonRpcProvider
import com.swmansion.starknet.signer.Signer
import com.swmansion.starknet.signer.StarkCurveSigner
import kotlinx.serialization.json.*
import network.utils.NetworkConfig
import network.utils.NetworkConfig.Network
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.*
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@Execution(ExecutionMode.SAME_THREAD)
class ProviderTest {
    companion object {
        @JvmStatic
        private val config = NetworkConfig.config
        private val network = config.network
        private val rpcUrl = config.rpcUrl
        private val gatewayUrl = config.gatewayUrl
        private val feederGatewayUrl = config.feederGatewayUrl
        private val accountAddress = config.accountAddress
        private val privateKey = config.privateKey

        private lateinit var signer: Signer

        private lateinit var gatewayProvider: GatewayProvider
        private lateinit var rpcProvider: JsonRpcProvider

        @JvmStatic
        @BeforeAll
        fun before() {
            signer = StarkCurveSigner(
                privateKey = privateKey,
            )
            rpcProvider = JsonRpcProvider(
                rpcUrl,
                StarknetChainId.TESTNET,
            )
        }

        @JvmStatic
        private fun getProviders(): List<Provider> = listOf(
            rpcProvider,
        )

        @JvmStatic
        @AfterAll
        fun after() {}
    }

    @Test
    fun `get spec version`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val provider = rpcProvider

        val request = provider.getSpecVersion()
        val specVersion = request.send()

        assertNotEquals(0, specVersion.length)
        val validPattern = "\\d+\\.\\d+\\.\\d+".toRegex()
        assertTrue(validPattern.containsMatchIn(specVersion))
    }

    @Test
    fun `get transaction status`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val provider = rpcProvider

        val transactionHash = when (network) {
            Network.INTEGRATION -> Felt.fromHex("0x26396c032286bcefb54616581eea5c7e373f0a21c322c44912cfa0944a52926")
            Network.TESTNET -> Felt.fromHex("0x72776cb6462e7e1268bd93dee8ad2df5ee0abed955e3010182161bdb0daea62")
        }
        val transactionHash2 = when (network) {
            Network.INTEGRATION -> Felt.fromHex("0x5e2e61a59e3f254f2c65109344be985dff979abd01b9c15b659a95f466689bf")
            Network.TESTNET -> Felt.fromHex("0x6bf08a6547a8be3cd3d718a068c2c0e9d3820252935f766c1ba6dd46f62e05")
        }
        val transactionStatus = provider.getTransactionStatus(transactionHash).send()
        assertEquals(TransactionStatus.ACCEPTED_ON_L1, transactionStatus.finalityStatus)
        assertNotNull(transactionStatus.executionStatus)
        assertEquals(TransactionExecutionStatus.SUCCEEDED, transactionStatus.executionStatus)

        val transactionStatus2 = provider.getTransactionStatus(transactionHash2).send()
        assertEquals(TransactionStatus.ACCEPTED_ON_L1, transactionStatus2.finalityStatus)
        assertNotNull(transactionStatus2.executionStatus)
        assertEquals(TransactionExecutionStatus.REVERTED, transactionStatus2.executionStatus)
    }

    @Disabled
    @Test
    fun `estimate message fee`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        // TODO: (#344) Currently, Juno fails to estimate the message fee.
        assumeFalse(network == Network.INTEGRATION)

        val provider = rpcProvider

        val gasConsumed = Felt(19931)
        val gasPrice = Felt(1022979559)
        val overallFee = Felt(20389005590429)

        val fromAddress = when (network) {
            Network.INTEGRATION -> Felt.fromHex("0xbe1259ff905cadbbaa62514388b71bdefb8aacc1")
            Network.TESTNET -> Felt.fromHex("0xf7d519a1660dd9237d47c039696fe4a2b93b6987")
        }
        val toAddress = when (network) {
            Network.INTEGRATION -> Felt.fromHex("0x073314940630fd6dcda0d772d4c972c4e0a9946bef9dabf4ef84eda8ef542b82")
            Network.TESTNET -> Felt.fromHex("0x0677d43766e880bfa6ddcf43e2ff54d54c64105e4a7fce20b7b1d40086a3a674")
        }
        val selector = when (network) {
            Network.INTEGRATION -> Felt.fromHex("0x02d757788a8d8d6f21d1cd40bce38a8222d70654214e96ff95d8086e684fbee5")
            Network.TESTNET -> Felt.fromHex("0x026490f901ea8ad5a245d987479919f1d20fbb0c164367e33ef09a9ea4ba8d04")
        }
        val message = MessageL1ToL2(
            fromAddress = fromAddress,
            toAddress = toAddress,
            selector = selector,
            payload = listOf(
                Felt.fromHex("0x54d01e5fc6eb4e919ceaab6ab6af192e89d1beb4f29d916768c61a4d48e6c95"),
                Felt.fromHex("0x38d7ea4c68000"),
                Felt.fromHex("0x0"),
            ),
        )

        val request = provider.getEstimateMessageFee(
            message = message,
            blockNumber = 306687,
        )
        val response = request.send()

        assertNotNull(response)
        assertNotNull(response.gasConsumed)
        assertNotNull(response.gasPrice)
        assertNotNull(response.overallFee)

        assertEquals(gasPrice, response.gasPrice)
        assertEquals(gasConsumed, response.gasConsumed)
        assertEquals(overallFee, response.overallFee)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get deploy account transaction`(provider: Provider) {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val transactionHash = when (network) {
            Network.INTEGRATION -> Felt.fromHex("0x029da9f8997ce580718fa02ed0bd628976418b30a0c5c542510aaef21a4445e4")
            Network.TESTNET -> Felt.fromHex("0xa8f359bad1181a37e41479b70a5c69a34e824b90accf8fbfba022708b7f08f")
        }
        val tx = provider.getTransaction(transactionHash).send()
        assertEquals(transactionHash, tx.hash)

        val receiptRequest = provider.getTransactionReceipt(transactionHash)
        val receipt = receiptRequest.send()

        assertTrue(receipt.isAccepted)
        assertNull(receipt.revertReason)

        when (provider) {
            is GatewayProvider -> assertTrue(receipt is GatewayTransactionReceipt)
            is JsonRpcProvider -> assertTrue(receipt is ProcessedDeployAccountRpcTransactionReceipt)
        }
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get reverted invoke transaction`(provider: Provider) {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val transactionHash = when (network) {
            Network.INTEGRATION -> Felt.fromHex("0x5e2e61a59e3f254f2c65109344be985dff979abd01b9c15b659a95f466689bf")
            Network.TESTNET -> Felt.fromHex("0x6bf08a6547a8be3cd3d718a068c2c0e9d3820252935f766c1ba6dd46f62e05")
        }
        val tx = provider.getTransaction(transactionHash).send()
        assertEquals(transactionHash, tx.hash)

        val receiptRequest = provider.getTransactionReceipt(transactionHash)
        val receipt = receiptRequest.send()

        assertFalse(receipt.isAccepted)
        assertEquals(TransactionExecutionStatus.REVERTED, receipt.executionStatus)
        assertEquals(TransactionFinalityStatus.ACCEPTED_ON_L1, receipt.finalityStatus)
        assertNotNull(receipt.revertReason)

        when (provider) {
            is GatewayProvider -> {
                assertTrue(receipt is GatewayTransactionReceipt)
                assertEquals(TransactionStatus.REVERTED, (receipt as GatewayTransactionReceipt).status)
            }
            is JsonRpcProvider -> assertTrue(receipt is ProcessedInvokeRpcTransactionReceipt)
        }
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get invoke transaction with events`(provider: Provider) {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val transactionHash = when (network) {
            Network.INTEGRATION -> Felt.fromHex("0x26396c032286bcefb54616581eea5c7e373f0a21c322c44912cfa0944a52926")
            Network.TESTNET -> Felt.fromHex("0x72776cb6462e7e1268bd93dee8ad2df5ee0abed955e3010182161bdb0daea62")
        }
        val tx = provider.getTransaction(transactionHash).send()
        assertTrue(tx is InvokeTransaction)
        assertEquals(transactionHash, tx.hash)
        assertEquals(TransactionType.INVOKE, tx.type)

        val receiptRequest = provider.getTransactionReceipt(transactionHash)
        val receipt = receiptRequest.send()

        assertTrue(receipt.isAccepted)
        assertTrue(receipt.events.size > 2)

        assertTrue(receipt.isAccepted)
        assertNull(receipt.revertReason)

        when (provider) {
            is GatewayProvider -> assertTrue(receipt is GatewayTransactionReceipt)
            is JsonRpcProvider -> assertTrue(receipt is ProcessedInvokeRpcTransactionReceipt)
        }
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get declare v0 transaction`(provider: Provider) {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val transactionHash = Felt.fromHex("0x6d346ba207eb124355960c19c737698ad37a3c920a588b741e0130ff5bd4d6d")
        val tx = provider.getTransaction(transactionHash).send() as DeclareTransactionV0
        assertEquals(transactionHash, tx.hash)
        assertNotEquals(Felt.ZERO, tx.classHash)
        assertEquals(Felt.ZERO, tx.version)

        val receiptRequest = provider.getTransactionReceipt(transactionHash)
        val receipt = receiptRequest.send()

        assertTrue(receipt.isAccepted)
        assertEquals(TransactionFinalityStatus.ACCEPTED_ON_L1, receipt.finalityStatus)
        assertNull(receipt.revertReason)

        when (provider) {
            is GatewayProvider -> assertTrue(receipt is GatewayTransactionReceipt)
            is JsonRpcProvider -> assertTrue(receipt is ProcessedDeclareRpcTransactionReceipt)
        }
        assertEquals(Felt.ZERO, receipt.actualFee)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get declare v1 transaction`(provider: Provider) {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val transactionHash = when (network) {
            Network.INTEGRATION -> Felt.fromHex("0x6d346ba207eb124355960c19c737698ad37a3c920a588b741e0130ff5bd4d6d")
            Network.TESTNET -> Felt.fromHex("0x6801a86a4a6873f62aaa478151ba03171691edde897c434ec8cf9db3bb77573")
        }
        val tx = provider.getTransaction(transactionHash).send() as DeclareTransactionV1
        assertNotEquals(Felt.ZERO, tx.classHash)
        assertEquals(transactionHash, tx.hash)

        val receiptRequest = provider.getTransactionReceipt(transactionHash)
        var receipt = receiptRequest.send()

        assertTrue(receipt.isAccepted)
        assertEquals(TransactionExecutionStatus.SUCCEEDED, receipt.executionStatus)
        assertEquals(TransactionFinalityStatus.ACCEPTED_ON_L1, receipt.finalityStatus)
        assertNull(receipt.revertReason)

        when (provider) {
            is GatewayProvider -> {
                receipt = receipt as GatewayTransactionReceipt
                assertEquals(TransactionStatus.ACCEPTED_ON_L1, receipt.status)
            }
            is JsonRpcProvider -> {
                receipt = receipt as ProcessedRpcTransactionReceipt
            }
        }
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get declare v2 transaction`(provider: Provider) {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val transactionHash = when (network) {
            Network.INTEGRATION -> Felt.fromHex("0x70fac6862a52000d2d63a1c845c26c9202c9030921b4607818a0820a46eab26")
            Network.TESTNET -> Felt.fromHex("0x747a364442ed4d72cd24d7e26f2c6ab0bc98c0a835f2276cd2bc07266331555")
        }
        val tx = provider.getTransaction(transactionHash).send() as DeclareTransactionV2
        assertNotEquals(Felt.ZERO, tx.classHash)
        assertEquals(transactionHash, tx.hash)

        val receiptRequest = provider.getTransactionReceipt(transactionHash)
        val receipt = receiptRequest.send()

        assertTrue(receipt.isAccepted)
        assertNull(receipt.revertReason)

        when (provider) {
            is GatewayProvider -> assertTrue(receipt is GatewayTransactionReceipt)
            is JsonRpcProvider -> assertTrue(receipt is ProcessedDeclareRpcTransactionReceipt)
        }
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get l1 handler transaction`(provider: Provider) {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val transactionHash = when (network) {
            Network.INTEGRATION -> Felt.fromHex("0x5753d979e05f7c079b04c8fdafe2b6f4951492b6509f66f1d86e7c061882ee3")
            Network.TESTNET -> Felt.fromHex("0x47ca5f1e16ba2cf997ebc33e60dfa3e5323fb3eebf63b0b9319fb4f6174ade8")
        }
        val tx = provider.getTransaction(transactionHash).send()
        assertTrue(tx is L1HandlerTransaction)
        assertEquals(TransactionType.L1_HANDLER, tx.type)

        val receiptRequest = provider.getTransactionReceipt(transactionHash)
        val receipt = receiptRequest.send()

        assertTrue(receipt.isAccepted)
        assertNull(receipt.revertReason)

        when (provider) {
            is GatewayProvider -> assertTrue(receipt is GatewayTransactionReceipt)
            is JsonRpcProvider -> assertTrue(receipt is ProcessedL1HandlerRpcTransactionReceipt)
        }
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get transaction receipt with l1 to l2 message`(provider: Provider) {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val transactionHash = when (network) {
            Network.INTEGRATION -> Felt.fromHex("0x27d9e669bb43d9f95bed591b296aeab0067b24c84818fb650a65eb120a9aebd")
            Network.TESTNET -> Felt.fromHex("0x116a9b469d266db31209d908e98f8b191c5923e808f347ed3fdde640d46f8d0")
        }

        val receiptRequest = provider.getTransactionReceipt(transactionHash)
        var receipt = receiptRequest.send()

        assertTrue(receipt.isAccepted)

        when (provider) {
            is GatewayProvider -> {
                receipt = receipt as GatewayTransactionReceipt
                assertNotNull(receipt.messageL1ToL2)
                assertNotNull(receipt.messageL1ToL2!!.nonce)
            }
            is JsonRpcProvider -> {
                receipt = receipt as ProcessedRpcTransactionReceipt
            }
        }
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get transaction receipt with l2 to l1 messages`(provider: Provider) {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val transactionHash = when (network) {
            Network.INTEGRATION -> Felt.fromHex("0x157438780a13f8cdfa5c291d666361c112ac0082751fac480e520a7bd78af6d")
            Network.TESTNET -> Felt.fromHex("0xd73488307e92d91ddf1a84b5670b37d3b1598e56096ad9be9925597133b681")
        }

        val receiptRequest = provider.getTransactionReceipt(transactionHash)
        val receipt = receiptRequest.send()

        assertTrue(receipt.isAccepted)

        assertEquals(2, receipt.messagesSent.size)
        assertNotNull(receipt.messagesSent[0].fromAddress)
        assertNotNull(receipt.messagesSent[0].toAddress)
        assertNotNull(receipt.messagesSent[0].payload)

        when (provider) {
            is GatewayProvider -> {
                assertTrue(receipt is GatewayTransactionReceipt)
                assertNull((receipt as GatewayTransactionReceipt).messageL1ToL2)
            }
            is JsonRpcProvider -> assertTrue(receipt is ProcessedRpcTransactionReceipt)
        }
    }

    @Test
    fun `get block with transactions with latest block tag`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val provider = rpcProvider
        val request = provider.getBlockWithTxs(BlockTag.LATEST)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is BlockWithTransactionsResponse)
    }

    @Disabled
    @Test
    fun `get block with transactions with pending block tag`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))
        // Note to future developers experiencing failures in this test:
        // 1. This test may fail because there's temporarily no pending block at the moment.
        // If this happens, try running the test again after a while or disable it.
        // 2. The node can be configured such way that accessing pending block is not supported.

        val provider = rpcProvider
        val request = provider.getBlockWithTxs(BlockTag.PENDING)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is PendingBlockWithTransactionsResponse)
    }

    @Test
    fun `get block with transactions with block hash`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val provider = rpcProvider
        val blockHash = when (network) {
            Network.INTEGRATION -> Felt.fromHex("0x164923d2819eb5dd207275b51348ea2ac6b46965290ffcdf89350c998f28048")
            Network.TESTNET -> Felt.fromHex("0x42be1d27e55744ab5d43ee98b8feb9895e96a034d6bb742a8204f530c680f3c")
        }
        val request = provider.getBlockWithTxs(blockHash)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is BlockWithTransactionsResponse)
        assertTrue(response.transactions.size >= 4)
    }

    @Test
    fun `get block with transactions with block number`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val provider = rpcProvider
        val blockNumber = 310252
        val request = provider.getBlockWithTxs(blockNumber)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is BlockWithTransactionsResponse)
        assertTrue(response.transactions.size >= 4)
    }

    @Test
    fun `get block with transaction hashes with latest block tag`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val provider = rpcProvider
        val request = provider.getBlockWithTxHashes(BlockTag.LATEST)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is BlockWithTransactionHashesResponse)
    }

    @Disabled
    @Test
    fun `get block with transaction hashes with pending block tag`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))
        // Note to future developers experiencing failures in this test:
        // 1. This test may fail because there's temporarily no pending block at the moment.
        // If this happens, try running the test again after a while or disable it.
        // 2. The node can be configured such way that accessing pending block is not supported.

        val provider = rpcProvider
        val request = provider.getBlockWithTxHashes(BlockTag.PENDING)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is PendingBlockWithTransactionHashesResponse)
    }

    @Test
    fun `get block with transaction hashes with block hash`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val provider = rpcProvider
        val blockHash = when (network) {
            Network.INTEGRATION -> Felt.fromHex("0x164923d2819eb5dd207275b51348ea2ac6b46965290ffcdf89350c998f28048")
            Network.TESTNET -> Felt.fromHex("0x42be1d27e55744ab5d43ee98b8feb9895e96a034d6bb742a8204f530c680f3c")
        }
        val request = provider.getBlockWithTxHashes(blockHash)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is BlockWithTransactionHashesResponse)
        assertTrue(response.transactionHashes.size >= 4)
    }

    @Test
    fun `get block with transaction hashes with block number`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val provider = rpcProvider
        val blockNumber = 310252
        val request = provider.getBlockWithTxHashes(blockNumber)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is BlockWithTransactionHashesResponse)
        assertTrue(response.transactionHashes.size >= 4)
    }
}
