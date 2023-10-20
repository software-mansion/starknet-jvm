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
            gatewayProvider = GatewayProvider(
                feederGatewayUrl,
                gatewayUrl,
                StarknetChainId.TESTNET,
            )
            rpcProvider = JsonRpcProvider(
                rpcUrl,
                StarknetChainId.TESTNET,
            )
        }

        @JvmStatic
        private fun getProviders(): List<Provider> = listOf(
            gatewayProvider,
            rpcProvider,
        )

        @JvmStatic
        @AfterAll
        fun after() {}
    }

    @Test
    fun `estimate message fee`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val provider = rpcProvider

        val gasConsumed = Felt(19931)
        val gasPrice = Felt(1022979559)
        val overallFee = Felt(20389005590429)

        val message = MessageL1ToL2(
            fromAddress = Felt.fromHex("0xbe1259ff905cadbbaa62514388b71bdefb8aacc1"),
            toAddress = Felt.fromHex("0x073314940630fd6dcda0d772d4c972c4e0a9946bef9dabf4ef84eda8ef542b82"),
            selector = Felt.fromHex("0x02d757788a8d8d6f21d1cd40bce38a8222d70654214e96ff95d8086e684fbee5"),
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

        val transactionHash = Felt.fromHex("0x029da9f8997ce580718fa02ed0bd628976418b30a0c5c542510aaef21a4445e4")
        val tx = provider.getTransaction(transactionHash).send()

        assertNotNull(tx)
        assertNotNull(tx.hash)
        assertEquals(transactionHash, tx.hash)
        assertEquals(Felt(10000000000000000L), tx.maxFee)

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
                receipt = receipt as DeployRpcTransactionReceipt
            }
        }
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get reverted invoke transaction`(provider: Provider) {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val transactionHash = Felt.fromHex("0x5e2e61a59e3f254f2c65109344be985dff979abd01b9c15b659a95f466689bf")
        val tx = provider.getTransaction(transactionHash).send()
        assertNotNull(tx)
        assertNotNull(tx.hash)
        assertEquals(transactionHash, tx.hash)

        val receiptRequest = provider.getTransactionReceipt(transactionHash)
        var receipt = receiptRequest.send()

        assertFalse(receipt.isAccepted)
        assertEquals(TransactionExecutionStatus.REVERTED, receipt.executionStatus)
        assertEquals(TransactionFinalityStatus.ACCEPTED_ON_L1, receipt.finalityStatus)
        assertNotNull(receipt.revertReason)

        when (provider) {
            is GatewayProvider -> {
                receipt = receipt as GatewayTransactionReceipt
                assertEquals(TransactionStatus.REVERTED, receipt.status)
            }
            is JsonRpcProvider -> {
                receipt = receipt as RpcTransactionReceipt
            }
        }
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get invoke transaction with events`(provider: Provider) {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val transactionHash = when (network) {
            Network.INTEGRATION -> Felt.fromHex("0x26396c032286bcefb54616581eea5c7e373f0a21c322c44912cfa0944a52926")
            Network.TESTNET -> Felt.fromHex("0x47ca5f1e16ba2cf997ebc33e60dfa3e5323fb3eebf63b0b9319fb4f6174ade8")
        }
        val tx = provider.getTransaction(transactionHash).send()
        assertNotNull(tx)

        val receiptRequest = provider.getTransactionReceipt(transactionHash)
        var receipt = receiptRequest.send()

        assertTrue(receipt.isAccepted)
        assertEquals(2, receipt.events.size)

        when (provider) {
            is GatewayProvider -> {
                receipt = receipt as GatewayTransactionReceipt
                assertEquals(TransactionStatus.ACCEPTED_ON_L1, receipt.status)
            }
            is JsonRpcProvider -> {
                receipt = receipt as RpcTransactionReceipt
            }
        }
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get declare v0 transaction and receipt`(provider: Provider) {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val transactionHash = Felt.fromHex("0x6d346ba207eb124355960c19c737698ad37a3c920a588b741e0130ff5bd4d6d")
        val tx = provider.getTransaction(transactionHash).send() as DeclareTransactionV0
        assertNotNull(tx)
        assertNotNull(tx.hash)
        assertNotNull(tx.classHash)
        assertEquals(transactionHash, tx.hash)
        assertEquals(Felt.fromHex("0x071e6ef53e53e6f5ca792fc4a5799a33e6f4118e4fd1d948dca3a371506f0cc7"), tx.classHash)
        assertEquals(Felt.ZERO, tx.version)
        assertEquals(Felt.ONE, tx.senderAddress)

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
                receipt = receipt as RpcTransactionReceipt
            }
        }
        assertEquals(Felt.ZERO, receipt.actualFee)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get declare v1 transaction and receipt`(provider: Provider) {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val transactionHash = Felt.fromHex("0x0417ec8ece9d2d2e68307069fdcde3c1fd8b0713b8a2687b56c19455c6ea85c1")
        val tx = provider.getTransaction(transactionHash).send() as DeclareTransactionV1
        assertNotNull(tx)
        assertNotNull(tx.hash)
        assertNotNull(tx.classHash)
        assertEquals(transactionHash, tx.hash)
        assertEquals(
            Felt.fromHex("0x043403d83a5efac5193d8942b135fbc27e684966a01a482ac8481b7561a0b737"),
            tx.classHash,
        )

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
                receipt = receipt as RpcTransactionReceipt
            }
        }
        assertEquals(Felt.fromHex("0x240b93577c4"), receipt.actualFee)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get declare v2 transaction`(provider: Provider) {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val transactionHash = Felt.fromHex("0x70fac6862a52000d2d63a1c845c26c9202c9030921b4607818a0820a46eab26")
        val tx = provider.getTransaction(transactionHash).send() as DeclareTransactionV2
        assertNotNull(tx)
        assertNotNull(tx.hash)
        assertNotNull(tx.classHash)
        assertEquals(transactionHash, tx.hash)
        assertEquals(
            Felt.fromHex("0x053bbb3899e1bfb338e9d2687204136c1e5bb89d581bdfdd650689df65b3a836"),
            tx.classHash,
        )
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
                receipt = receipt as RpcTransactionReceipt
            }
            else -> throw IllegalStateException("Unknown provider type")
        }

        assertEquals(Felt.fromHex("0x35f91a1984d"), receipt.actualFee)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get transaction receipt with l1 to l2 message`(provider: Provider) {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val transactionHash = Felt.fromHex("0x27d9e669bb43d9f95bed591b296aeab0067b24c84818fb650a65eb120a9aebd")

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
                receipt = receipt as RpcTransactionReceipt
            }
        }
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get transaction receipt with l2 to l1 messages`(provider: Provider) {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))
        val transactionHash = Felt.fromHex("0x157438780a13f8cdfa5c291d666361c112ac0082751fac480e520a7bd78af6d")

        val receiptRequest = provider.getTransactionReceipt(transactionHash)
        var receipt = receiptRequest.send()

        assertTrue(receipt.isAccepted)

        assertEquals(2, receipt.messagesSent.size)
        assertNotNull(receipt.messagesSent[0].fromAddress)
        assertNotNull(receipt.messagesSent[0].toAddress)
        assertNotNull(receipt.messagesSent[0].payload)

        when (provider) {
            is GatewayProvider -> {
                receipt = receipt as GatewayTransactionReceipt
                assertNull(receipt.messageL1ToL2)
            }
            is JsonRpcProvider -> {
                receipt = receipt as RpcTransactionReceipt
            }
            else -> throw IllegalStateException("Unknown provider type")
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
        val blockHash = Felt.fromHex("0x164923d2819eb5dd207275b51348ea2ac6b46965290ffcdf89350c998f28048")
        val request = provider.getBlockWithTxs(blockHash)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is BlockWithTransactionsResponse)
        assertEquals(4, response.transactions.size)
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
        assertEquals(4, response.transactions.size)
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
        val blockHash = Felt.fromHex("0x164923d2819eb5dd207275b51348ea2ac6b46965290ffcdf89350c998f28048")
        val request = provider.getBlockWithTxHashes(blockHash)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is BlockWithTransactionHashesResponse)
        assertEquals(4, response.transactionHashes.size)
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
        assertEquals(4, response.transactionHashes.size)
    }
}
