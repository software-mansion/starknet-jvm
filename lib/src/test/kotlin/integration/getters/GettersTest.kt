package integration.getters

import com.swmansion.starknet.account.Account
import com.swmansion.starknet.account.StandardAccount
import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.data.types.transactions.*
import com.swmansion.starknet.provider.Provider
import com.swmansion.starknet.provider.gateway.GatewayProvider
import com.swmansion.starknet.provider.rpc.JsonRpcProvider
import com.swmansion.starknet.signer.Signer
import com.swmansion.starknet.signer.StarkCurveSigner
import integration.utils.ConfigUtils
import kotlinx.serialization.json.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Path
import kotlin.io.path.readText

@Execution(ExecutionMode.SAME_THREAD)
class GettersTest {
    companion object {
        @JvmStatic
        private val config = ConfigUtils.config
        private val rpcUrl = config.rpcUrl
        private val gatewayUrl = config.gatewayUrl
        private val feederGatewayUrl = config.feederGatewayUrl
        private val accountAddress = config.integrationAccountAddress
        private val privateKey = config.privateKey

        private lateinit var signer: Signer

        private lateinit var gatewayProvider: GatewayProvider
        private lateinit var rpcProvider: JsonRpcProvider

        @JvmStatic
        @BeforeAll
        fun before() {
            val testName = GettersTest::class.simpleName!!

            assumeFalse(ConfigUtils.isTestSetSkipped(testName), "$testName is skipped in the current environment.")

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
        data class AccountAndProvider(val account: Account, val provider: Provider)

        @JvmStatic
        private fun getProviders(): List<Provider> = listOf(
            gatewayProvider,
            rpcProvider,
        )

        @JvmStatic
        fun getAccounts(): List<AccountAndProvider> {
            return listOf(
                AccountAndProvider(
                    StandardAccount(
                        accountAddress,
                        signer,
                        gatewayProvider,
                    ),
                    gatewayProvider,
                ),
                AccountAndProvider(
                    StandardAccount(
                        accountAddress,
                        signer,
                        rpcProvider,
                    ),
                    rpcProvider,
                ),
            )
        }

        @JvmStatic
        @AfterAll
        fun after() {}
    }

    @Test
    fun `estimate message fee`() {
        val provider = rpcProvider

        val gasConsumed = Felt(19931)
        val gasPrice = Felt(1022979559)
        val overallFee = Felt(20389005590429)

        val message = RpcMessageL1ToL2(
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
        //        println(receipt.revertReason)

        if (provider is GatewayProvider) {
            receipt = receipt as GatewayTransactionReceipt

            assertEquals(TransactionStatus.REVERTED, receipt.status)
        } else if (provider is JsonRpcProvider) {
            receipt = receipt as RpcTransactionReceipt
        }
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get invoke transaction with events`(provider: Provider) {
        val transactionHash = Felt.fromHex("0x34223514e92989608e3b36f2a2a53011fa0699a275d7936a18921a11963c792")
        val tx = provider.getTransaction(transactionHash).send()
        assertNotNull(tx)

        val receiptRequest = provider.getTransactionReceipt(transactionHash)
        var receipt = receiptRequest.send()

        assertTrue(receipt.isAccepted)
        assertEquals(4, receipt.events.size)
        val firstEvent = receipt.events[0]
        assertEquals(
            listOf(Felt.fromHex("0x07c930a86c2ed72bea4767b688367e06fd2b2a58915bdd3cfa16fb61b485e8c5")),
            firstEvent.data,
        )
        assertEquals(
            Felt.fromHex("0x05ee5dbac8c39fe9ef8d7761cc84086949d7dc42dd6233cb6310208272ee87ea"),
            firstEvent.address,
        )

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
    fun `get declare v1 transaction and receipt`(provider: Provider) {
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

        if (provider is GatewayProvider) {
            receipt = receipt as GatewayTransactionReceipt

            assertEquals(TransactionStatus.ACCEPTED_ON_L1, receipt.status)
        } else if (provider is JsonRpcProvider) {
            receipt = receipt as RpcTransactionReceipt
        }

        assertEquals(Felt.fromHex("0x35f91a1984d"), receipt.actualFee)
    }

    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `estimate declare v1 transaction fee`(accountAndProvider: AccountAndProvider) {
        val (account, provider) = accountAndProvider
        // TODO: find a better account that has a non-changing nonce
        assumeFalse(provider is GatewayProvider)

        val contractCode = Path.of("src/test/resources/compiled_v0/providerTest.json").readText()
        val contractDefinition = Cairo0ContractDefinition(contractCode)
        val nonce = account.getNonce().send()

        // Note to future developers experiencing failures in this test. Compiled contract format sometimes
        // changes, this causes changes in the class hash.
        // If this test starts randomly falling, try recalculating class hash.
        val classHash = Felt.fromHex("0x320aba87b66c023b2db943b9d32bc0f8e3d72625b475e1dc77e4d2f21721d43")
        val declareTransactionPayload = account.signDeclare(
            contractDefinition,
            classHash,
            ExecutionParams(
                nonce = nonce,
                maxFee = Felt(1000000000000000),
            ),
        )

        val signedTransaction = TransactionFactory.makeDeclareV1Transaction(
            classHash = classHash,
            senderAddress = declareTransactionPayload.senderAddress,
            contractDefinition = declareTransactionPayload.contractDefinition,
            chainId = provider.chainId,
            nonce = nonce,
            maxFee = declareTransactionPayload.maxFee,
            signature = declareTransactionPayload.signature,
            version = declareTransactionPayload.version,
        )

        val feeEstimateRequest = provider.getEstimateFee(listOf(signedTransaction.toPayload()), BlockTag.LATEST)

        assertNotNull(feeEstimateRequest)
        val feeEstimate = feeEstimateRequest.send().first()
        assertNotNull(feeEstimate)
        assertNotEquals(Felt(0), feeEstimate.gasConsumed)
        assertNotEquals(Felt(0), feeEstimate.gasPrice)
        assertNotEquals(Felt(0), feeEstimate.overallFee)
    }
}
