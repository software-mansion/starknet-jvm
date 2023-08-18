package integration.account

import com.swmansion.starknet.account.Account
import com.swmansion.starknet.account.StandardAccount
import com.swmansion.starknet.crypto.StarknetCurve
import com.swmansion.starknet.data.ContractAddressCalculator
import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.data.types.transactions.*
import com.swmansion.starknet.provider.Provider
import com.swmansion.starknet.provider.gateway.GatewayProvider
import com.swmansion.starknet.provider.rpc.JsonRpcProvider
import com.swmansion.starknet.signer.Signer
import com.swmansion.starknet.signer.StarkCurveSigner
import integration.utils.IntegrationConfig
import kotlinx.serialization.json.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigInteger
import java.nio.file.Path
import kotlin.io.path.readText

@Execution(ExecutionMode.SAME_THREAD)
class AccountTest {
    companion object {
        @JvmStatic
        private val config = IntegrationConfig.config
        private val rpcUrl = config.rpcUrl
        private val gatewayUrl = config.gatewayUrl
        private val feederGatewayUrl = config.feederGatewayUrl
        private val accountAddress = config.accountAddress
        private val privateKey = config.privateKey
        private val accountContractClassHash = Felt.fromHex("0x05a9941d0cc16b8619a3325055472da709a66113afcc6a8ab86055da7d29c5f8")
        private val predeployedMapContractAddress = Felt.fromHex("0x05cd21d6b3952a869fda11fa9a5bd2657bd68080d3da255655ded47a81c8bd53")

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
                        rpcProvider,
                    ),
                    rpcProvider,
                ),
                AccountAndProvider(
                    StandardAccount(
                        accountAddress,
                        signer,
                        gatewayProvider,
                    ),
                    gatewayProvider,
                ),
            )
        }

        @JvmStatic
        @AfterAll
        fun after() {}
    }

    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `estimate fee for declare v1 transaction`(accountAndProvider: AccountAndProvider) {
        assumeTrue(IntegrationConfig.isTestEnabled(requiresGas = false))

        val (account, provider) = accountAndProvider
        // Note to future developers
        // This test sometimes fails due to getNonce receiving higher nonce than expected by addDeclareTransaction
        // Apparently, getNonce knows about pending blocks while other methods don't
        // TODO: find a better account that has a non-changing nonce
        assumeFalse(provider is GatewayProvider)

        val contractCode = Path.of("src/test/resources/compiled_v0/providerTest.json").readText()
        val contractDefinition = Cairo0ContractDefinition(contractCode)
        val nonce = account.getNonce().send()

        // Note to future developers experiencing failures in this test.
        // 1. Compiled contract format sometimes changes, this causes changes in the class hash.
        // If this test starts randomly falling, try recalculating class hash.
        // 2. If it fails on CI, make sure to delete the compiled contracts before running this test.
        // Chances are, the contract was compiled with a different compiler version.

        val classHash = Felt.fromHex("0x661efb55f8bcf34ad1596936b631e6b581bfa246b99ff3f9f2d9b8fa4ff5962")
        val declareTransactionPayload = account.signDeclare(
            contractDefinition,
            classHash,
            ExecutionParams(
                nonce = nonce,
                maxFee = Felt(1000000000000000L),
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

    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `estimate fee for declare v2 transaction`(accountAndProvider: AccountAndProvider) {
        assumeTrue(IntegrationConfig.isTestEnabled(requiresGas = false))

        // Note to future developers
        // This test sometimes fails due to getNonce receiving higher nonce than expected by addDeclareTransaction
        // Apparently, getNonce knows about pending blocks while other methods don't

        val (account, provider) = accountAndProvider
        // TODO: find a better account that has a non-changing nonce
        assumeFalse(provider is GatewayProvider)

        val contractCode = Path.of("src/test/resources/compiled_v1/${provider::class.simpleName}_hello_starknet.json").readText()
        val casmCode = Path.of("src/test/resources/compiled_v1/${provider::class.simpleName}_hello_starknet.casm").readText()

        val contractDefinition = Cairo1ContractDefinition(contractCode)
        val casmContractDefinition = CasmContractDefinition(casmCode)

        val nonce = account.getNonce().send()
        val declareTransactionPayload = account.signDeclare(
            contractDefinition,
            casmContractDefinition,
            ExecutionParams(
                nonce = nonce,
                maxFee = Felt(1000000000000000L),
            ),
        )

        val signedTransaction = TransactionFactory.makeDeclareV2Transaction(
            senderAddress = declareTransactionPayload.senderAddress,
            contractDefinition = declareTransactionPayload.contractDefinition,
            casmContractDefinition = casmContractDefinition,
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

    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `sign and send declare v1 transaction`(accountAndProvider: AccountAndProvider) {
        assumeTrue(IntegrationConfig.isTestEnabled(requiresGas = true))

        val (account, provider) = accountAndProvider
        // assumeFalse(provider is JsonRpcProvider)
        // Note to future developers experiencing failures in this test.
        // Sometimes the test fails with "A transaction with the same hash already exists in the mempool"
        // This error can be caused by RPC node not having access to pending transactions and therefore nonce not getting updated.

        val contractCode = Path.of("src/test/resources/compiled_v0/providerTest.json").readText()
        val contractDefinition = Cairo0ContractDefinition(contractCode)
        val nonce = account.getNonce().send()

        // Note to future developers experiencing failures in this test.
        // 1. Compiled contract format sometimes changes, this causes changes in the class hash.
        // If this test starts randomly falling, try recalculating class hash.
        // 2. If it fails on CI, make sure to delete the compiled contracts before running this test.
        // Chances are, the contract was compiled with a different compiler version.

        val classHash = Felt.fromHex("0x661efb55f8bcf34ad1596936b631e6b581bfa246b99ff3f9f2d9b8fa4ff5962")
        val declareTransactionPayload = account.signDeclare(
            contractDefinition,
            classHash,
            ExecutionParams(nonce, Felt(1000000000000000L)),
        )

        val request = provider.declareContract(declareTransactionPayload)
        val result = request.send()

        Thread.sleep(10000)

        val receipt = provider.getTransactionReceipt(result.transactionHash).send()

        assertNotNull(result)
        assertNotNull(receipt)
        assertTrue(receipt.isAccepted)
    }

    @Disabled
    // TODO: Fails with "Compiled versions older than 1.1.0 or newer than 1.3.0 are not supported. Got version 1.0.0."
    // Can be reenabled after compiler version bump (awaiting PR #285 merge)
    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `sign and send declare v2 transaction`(accountAndProvider: AccountAndProvider) {
        assumeTrue(IntegrationConfig.isTestEnabled(requiresGas = true))

        val (account, provider) = accountAndProvider

        val contractCode = Path.of("src/test/resources/compiled_v1/${provider::class.simpleName}_hello_starknet.json").readText()
        val casmCode = Path.of("src/test/resources/compiled_v1/${provider::class.simpleName}_hello_starknet.casm").readText()

        val contractDefinition = Cairo1ContractDefinition(contractCode)
        val contractCasmDefinition = CasmContractDefinition(casmCode)
        val nonce = account.getNonce().send()

        val declareTransactionPayload = account.signDeclare(
            contractDefinition,
            contractCasmDefinition,
            ExecutionParams(nonce, Felt(1000000000000000L)),
        )
        val request = provider.declareContract(declareTransactionPayload)
        val result = request.send()

        Thread.sleep(10000)
        val receipt = provider.getTransactionReceipt(result.transactionHash).send()

        assertNotNull(result)
        assertNotNull(receipt)
        assertTrue(receipt.isAccepted)
    }

    @Disabled
    // Note to future developers
    // This test sometimes fails due to getNonce receiving higher (pending) nonce than addInvokeTransaction expects
    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `sign and send invoke transaction`(accountAndProvider: AccountAndProvider) {
        assumeTrue(IntegrationConfig.isTestEnabled(requiresGas = true))

        val (account, provider) = accountAndProvider

        val call = Call(
            contractAddress = predeployedMapContractAddress,
            entrypoint = "put",
            calldata = listOf(Felt.fromHex("0x1D2C3B7A8"), Felt.fromHex("0x451")),
        )

        val invokeRequest = account.execute(call)
        val invokeResponse = invokeRequest.send()

        Thread.sleep(60000)

        val receipt = provider.getTransactionReceipt(invokeResponse.transactionHash).send()
        assertTrue(receipt.isAccepted)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `call contract`(provider: Provider) {
        assumeTrue(IntegrationConfig.isTestEnabled(requiresGas = false))

        // Note to future developers:
        // This test might fail if someone deliberately changes the key's corresponding value in contract.
        val call = Call(
            contractAddress = predeployedMapContractAddress,
            entrypoint = "get",
            calldata = listOf(Felt.fromHex("0x1D2C3B7A8")),
        )

        val getRequest = provider.callContract(call, BlockTag.LATEST)
        val getResponse = getRequest.send()
        val value = getResponse.first()
        assertNotEquals(Felt.fromHex("0x0"), value)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `estimate deploy account fee`(provider: Provider) {
        assumeTrue(IntegrationConfig.isTestEnabled(requiresGas = false))

        val privateKey = Felt(System.currentTimeMillis())
        val publicKey = StarknetCurve.getPublicKey(privateKey)

        val classHash = accountContractClassHash

        val salt = Felt(System.currentTimeMillis())

        val calldata = listOf(publicKey)
        val address = ContractAddressCalculator.calculateAddressFromHash(
            classHash = classHash,
            calldata = calldata,
            salt = salt,
        )

        val account = StandardAccount(
            address,
            privateKey,
            provider,
        )

        val payloadForFeeEstimation = account.signDeployAccount(
            classHash = classHash,
            salt = salt,
            calldata = calldata,
            maxFee = Felt.ZERO,
            nonce = Felt.ZERO,
            forFeeEstimate = true,
        )
        assertEquals(payloadForFeeEstimation.version, Felt(BigInteger("340282366920938463463374607431768211457")))

        val feePayload = provider.getEstimateFee(listOf(payloadForFeeEstimation)).send()
        assertTrue(feePayload.first().overallFee.value > Felt.ONE.value)
    }
}
