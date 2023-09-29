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
import com.swmansion.starknet.signer.StarkCurveSigner
import integration.utils.IntegrationConfig
import kotlinx.serialization.json.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import starknet.utils.ScarbClient
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
        private val signer = StarkCurveSigner(config.privateKey)
        private val constNonceAccountAddress = config.constNonceAccountAddress ?: config.accountAddress
        private val constNonceSigner = StarkCurveSigner(config.constNoncePrivateKey ?: config.privateKey)

        private val accountContractClassHash = Felt.fromHex("0x05a9941d0cc16b8619a3325055472da709a66113afcc6a8ab86055da7d29c5f8")
        private val predeployedMapContractAddress = Felt.fromHex("0x05cd21d6b3952a869fda11fa9a5bd2657bd68080d3da255655ded47a81c8bd53")
        private val ethContractAddress = Felt.fromHex("0x049d36570d4e46f48e99674bd3fcc84644ddd6b96f7c741b1562b82f9e004dc7")

        private val gatewayProvider = GatewayProvider(
            feederGatewayUrl,
            gatewayUrl,
            StarknetChainId.TESTNET,
        )
        private val rpcProvider = JsonRpcProvider(
            rpcUrl,
            StarknetChainId.TESTNET,
        )

        @JvmStatic
        @BeforeAll
        fun before() {}

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
        fun getConstNonceAccounts(): List<AccountAndProvider> {
            return listOf(
                AccountAndProvider(
                    StandardAccount(
                        constNonceAccountAddress,
                        constNonceSigner,
                        rpcProvider,
                    ),
                    rpcProvider,
                ),
                AccountAndProvider(
                    // Note to future developers:
                    // Some tests may fail due to getNonce receiving higher nonce than expected by other methods
                    // Apparently, getNonce knows about pending blocks while other methods don't
                    // Until it remains this way, an account with a constant nonce is used for these tests
                    // Only use this account for tests that don't change the state of the network (non-gas tests)
                    StandardAccount(
                        constNonceAccountAddress,
                        constNonceSigner,
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
    @MethodSource("getConstNonceAccounts")
    fun `estimate fee for invoke transaction`(accountAndProvider: AccountAndProvider) {
        assumeTrue(IntegrationConfig.isTestEnabled(requiresGas = false))

        val (account, provider) = accountAndProvider

        val call = Call(
            contractAddress = predeployedMapContractAddress,
            entrypoint = "put",
            calldata = listOf(Felt.fromHex("0x1D2C3B7A8"), Felt.fromHex("0x451")),
        )
        val estimateFeeRequest = account.estimateFee(listOf(call))
        val estimateFeeResponse = estimateFeeRequest.send().first().overallFee
        assertTrue(estimateFeeResponse.value > Felt.ONE.value)
    }

    @ParameterizedTest
    @MethodSource("getConstNonceAccounts")
    fun `estimate fee for declare v1 transaction`(accountAndProvider: AccountAndProvider) {
        assumeTrue(IntegrationConfig.isTestEnabled(requiresGas = false))

        val (account, provider) = accountAndProvider
        val contractCode = Path.of("src/test/resources/contracts_v0/target/release/providerTest.json").readText()
        val contractDefinition = Cairo0ContractDefinition(contractCode)
        val nonce = account.getNonce().send()

        // Note to future developers experiencing failures in this test.
        // 1. Compiled contract format sometimes changes, this causes changes in the class hash.
        // If this test starts randomly falling, try recalculating class hash.
        // 2. If it fails on CI, make sure to delete the compiled contracts before running this test.
        // Chances are, the contract was compiled with a different compiler version.

        val classHash = Felt.fromHex("0x3b32bb615844ea7a9a56a8966af1a5ba1457b1f5c9162927ca1968975b0d2a9")
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

        val feeEstimate = feeEstimateRequest.send().first()
        assertNotEquals(Felt(0), feeEstimate.gasConsumed)
        assertNotEquals(Felt(0), feeEstimate.gasPrice)
        assertNotEquals(Felt(0), feeEstimate.overallFee)
    }

    @ParameterizedTest
    @MethodSource("getConstNonceAccounts")
    fun `estimate fee for declare v2 transaction`(accountAndProvider: AccountAndProvider) {
        assumeTrue(IntegrationConfig.isTestEnabled(requiresGas = false))

        val (account, provider) = accountAndProvider
        assumeFalse(provider is GatewayProvider)

        ScarbClient.createSaltedContract(
            placeholderContractPath = Path.of("src/test/resources/contracts_v1/src/placeholder_hello_starknet.cairo"),
            saltedContractPath = Path.of("src/test/resources/contracts_v1/src/salted_hello_starknet.cairo"),
        )
        ScarbClient.buildContracts(Path.of("src/test/resources/contracts_v1"))
        val contractCode = Path.of("src/test/resources/contracts_v1/target/release/ContractsV1_SaltedHelloStarknet.sierra.json").readText()
        val casmCode = Path.of("src/test/resources/contracts_v1/target/release/ContractsV1_SaltedHelloStarknet.casm.json").readText()

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

        val feeEstimate = feeEstimateRequest.send().first()
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

        val contractCode = Path.of("src/test/resources/contracts_v0/target/release/providerTest.json").readText()
        val contractDefinition = Cairo0ContractDefinition(contractCode)
        val nonce = account.getNonce().send()

        // Note to future developers experiencing failures in this test.
        // 1. Compiled contract format sometimes changes, this causes changes in the class hash.
        // If this test starts randomly falling, try recalculating class hash.
        // 2. If it fails on CI, make sure to delete the compiled contracts before running this test.
        // Chances are, the contract was compiled with a different compiler version.
        // 3. This test sometimes fails due to getNonce receiving higher (pending) nonce than addDeclareTransaction expects

        val classHash = Felt.fromHex("0x3b32bb615844ea7a9a56a8966af1a5ba1457b1f5c9162927ca1968975b0d2a9")
        val declareTransactionPayload = account.signDeclare(
            contractDefinition,
            classHash,
            ExecutionParams(nonce, Felt(1000000000000000L)),
        )

        val request = provider.declareContract(declareTransactionPayload)
        val result = request.send()

        Thread.sleep(30000)

        val receipt = provider.getTransactionReceipt(result.transactionHash).send()

        assertTrue(receipt.isAccepted)
    }

    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `sign and send declare v2 transaction`(accountAndProvider: AccountAndProvider) {
        assumeTrue(IntegrationConfig.isTestEnabled(requiresGas = true))
        // Note to future developers experiencing experiencing failures in this test.
        // This test sometimes fails due to getNonce receiving higher (pending) nonce than addDeclareTransaction expects

        val (account, provider) = accountAndProvider

        ScarbClient.createSaltedContract(
            placeholderContractPath = Path.of("src/test/resources/contracts_v1/src/placeholder_hello_starknet.cairo"),
            saltedContractPath = Path.of("src/test/resources/contracts_v1/src/salted_hello_starknet.cairo"),
        )
        ScarbClient.buildContracts(Path.of("src/test/resources/contracts_v1"))
        val contractCode = Path.of("src/test/resources/contracts_v1/target/release/ContractsV1_SaltedHelloStarknet.sierra.json").readText()
        val casmCode = Path.of("src/test/resources/contracts_v1/target/release/ContractsV1_SaltedHelloStarknet.casm.json").readText()

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

        Thread.sleep(30000)
        val receipt = provider.getTransactionReceipt(result.transactionHash).send()

        assertTrue(receipt.isAccepted)
    }

    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `sign and send declare v2 transaction (cairo compiler v2)`(accountAndProvider: AccountAndProvider) {
        assumeTrue(IntegrationConfig.isTestEnabled(requiresGas = true))
        // Note to future developers experiencing experiencing failures in this test.
        // This test sometimes fails due to getNonce receiving higher (pending) nonce than addDeclareTransaction expects

        val (account, provider) = accountAndProvider

        ScarbClient.createSaltedContract(
            placeholderContractPath = Path.of("src/test/resources/contracts_v2/src/placeholder_counter_contract.cairo"),
            saltedContractPath = Path.of("src/test/resources/contracts_v2/src/salted_counter_contract.cairo"),
        )
        ScarbClient.buildContracts(Path.of("src/test/resources/contracts_v2"))
        val contractCode = Path.of("src/test/resources/contracts_v2/target/release/ContractsV2_SaltedCounterContract.sierra.json").readText()
        val casmCode = Path.of("src/test/resources/contracts_v2/target/release/ContractsV2_SaltedCounterContract.casm.json").readText()

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

        Thread.sleep(30000)
        val receipt = provider.getTransactionReceipt(result.transactionHash).send()

        assertTrue(receipt.isAccepted)
    }

    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `sign and send invoke transaction`(accountAndProvider: AccountAndProvider) {
        assumeTrue(IntegrationConfig.isTestEnabled(requiresGas = true))
        // Note to future developers experiencing experiencing failures in this test.
        // This test sometimes fails due to getNonce receiving higher (pending) nonce than addInvokeTransaction expects

        val (account, provider) = accountAndProvider

        val call = Call(
            contractAddress = predeployedMapContractAddress,
            entrypoint = "put",
            calldata = listOf(Felt.fromHex("0x1D2C3B7A8"), Felt.fromHex("0x451")),
        )

        val invokeRequest = account.execute(call)
        val invokeResponse = invokeRequest.send()

        Thread.sleep(30000)

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
    fun `estimate fee for deploy account`(provider: Provider) {
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

    @ParameterizedTest
    @MethodSource("getConstNonceAccounts")
    fun `get ETH balance`(accountAndProvider: AccountAndProvider) {
        assumeTrue(IntegrationConfig.isTestEnabled(requiresGas = false))

        val (account, provider) = accountAndProvider
        val call = Call(
            contractAddress = ethContractAddress,
            entrypoint = "balanceOf",
            calldata = listOf(account.address),
        )

        val request = provider.callContract(call)
        val response = request.send()
        val balance = Uint256(
            low = response[0],
            high = response[1],
        )

        assertTrue(balance.value > Felt.ZERO.value)
    }

    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `transfer ETH`(accountAndProvider: AccountAndProvider) {
        assumeTrue(IntegrationConfig.isTestEnabled(requiresGas = true))

        val (account, provider) = accountAndProvider
        val recipientAccountAddress = constNonceAccountAddress

        val amount = Uint256(Felt.ONE)
        val call = Call(
            contractAddress = ethContractAddress,
            entrypoint = "transfer",
            calldata = listOf(recipientAccountAddress, amount.low, amount.high),
        )

        val request = account.execute(call)
        val response = request.send()
        Thread.sleep(15000)

        val transferReceipt = provider.getTransactionReceipt(response.transactionHash).send()
        assertTrue(transferReceipt.isAccepted)
    }

    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `deploy account`(accountAndProvider: AccountAndProvider) {
        assumeTrue(IntegrationConfig.isTestEnabled(requiresGas = true))

        val (account, provider) = accountAndProvider

        val privateKey = Felt(System.currentTimeMillis())
        val publicKey = StarknetCurve.getPublicKey(privateKey)

        val classHash = accountContractClassHash
        val salt = Felt(System.currentTimeMillis())
        val calldata = listOf(publicKey)
        val deployedAccountAddress = ContractAddressCalculator.calculateAddressFromHash(
            classHash = classHash,
            calldata = calldata,
            salt = salt,
        )

        val deployedAccount = StandardAccount(
            deployedAccountAddress,
            privateKey,
            provider,
        )

        val deployMaxFee = Uint256(5523000060522)
        val call = Call(
            contractAddress = ethContractAddress,
            entrypoint = "transfer",
            calldata = listOf(
                deployedAccountAddress,
                deployMaxFee.low,
                deployMaxFee.high,
            ),
        )

        val transferRequest = account.execute(call)
        val transferResponse = transferRequest.send()
        Thread.sleep(15000)

        val transferReceipt = provider.getTransactionReceipt(transferResponse.transactionHash).send()
        assertTrue(transferReceipt.isAccepted)

        val payload = deployedAccount.signDeployAccount(
            classHash = classHash,
            salt = salt,
            calldata = calldata,
            // 10*fee from estimate deploy account fee
            maxFee = Felt(deployMaxFee.value),
        )

        val response = provider.deployAccount(payload).send()

        // Make sure the address matches the calculated one
        assertEquals(deployedAccountAddress, response.address)

        // Make sure tx matches what we sent
        Thread.sleep(15000)

        val tx = provider.getTransaction(response.transactionHash).send() as DeployAccountTransaction
        assertEquals(payload.classHash, tx.classHash)
        assertEquals(payload.salt, tx.contractAddressSalt)
        assertEquals(payload.constructorCalldata, tx.constructorCalldata)
        assertEquals(payload.version, tx.version)
        assertEquals(payload.nonce, tx.nonce)
        assertEquals(payload.maxFee, tx.maxFee)
        assertEquals(payload.signature, tx.signature)

        val receipt = provider.getTransactionReceipt(response.transactionHash).send()
        assertTrue(receipt.isAccepted)
    }

    @ParameterizedTest
    @MethodSource("getConstNonceAccounts")
    fun `simulate invoke and deploy account transactions`(accountAndProvider: AccountAndProvider) {
        assumeTrue(IntegrationConfig.isTestEnabled(requiresGas = false))
        val (account, sourceProvider) = accountAndProvider
        assumeTrue(sourceProvider is JsonRpcProvider)
        val provider = sourceProvider as JsonRpcProvider

        val nonce = account.getNonce().send()
        val call = Call(
            contractAddress = predeployedMapContractAddress,
            entrypoint = "put",
            calldata = listOf(
                Felt(101),
                Felt(2137),
            ),
        )
        val params = ExecutionParams(nonce, Felt(1000000000))
        val invokeTx = account.sign(call, params)

        val privateKey = Felt(System.currentTimeMillis())
        val publicKey = StarknetCurve.getPublicKey(privateKey)

        val classHash = accountContractClassHash
        val salt = Felt(System.currentTimeMillis())
        val calldata = listOf(publicKey)
        val deployedAccountAddress = ContractAddressCalculator.calculateAddressFromHash(classHash, calldata, salt)

        val deployedAccount = StandardAccount(deployedAccountAddress, privateKey, provider)
        val deployAccountTx = deployedAccount.signDeployAccount(
            classHash = classHash,
            salt = salt,
            calldata = calldata,
            maxFee = Felt.fromHex("0x11fcc58c7f7000"),
        )

        // Pathfinder currently always requires SKIP_FEE_CHARGE flag
        val simulationFlags = setOf(SimulationFlag.SKIP_FEE_CHARGE)
        val simulationResult = provider.simulateTransactions(
            transactions = listOf(invokeTx, deployAccountTx),
            blockTag = BlockTag.LATEST,
            simulationFlags = simulationFlags,
        ).send()
        assertEquals(2, simulationResult.size)
        assertTrue(simulationResult[0].transactionTrace is InvokeTransactionTrace)
        assertTrue(simulationResult[0].transactionTrace is CommonInvokeTransactionTrace)
        assertTrue(simulationResult[1].transactionTrace is DeployAccountTransactionTrace)

        val invokeTxWithoutSignature = InvokeTransactionPayload(invokeTx.senderAddress, invokeTx.calldata, emptyList(), invokeTx.maxFee, invokeTx.version, invokeTx.nonce)
        val deployAccountTxWithoutSignature = DeployAccountTransactionPayload(deployAccountTx.classHash, deployAccountTx.salt, deployAccountTx.constructorCalldata, deployAccountTx.version, deployAccountTx.nonce, deployAccountTx.maxFee, emptyList())

        val simulationFlags2 = setOf(SimulationFlag.SKIP_FEE_CHARGE, SimulationFlag.SKIP_VALIDATE)
        val simulationResult2 = provider.simulateTransactions(
            transactions = listOf(invokeTxWithoutSignature, deployAccountTxWithoutSignature),
            blockTag = BlockTag.LATEST,
            simulationFlags = simulationFlags2,
        ).send()

        assertEquals(2, simulationResult2.size)
        assertTrue(simulationResult[0].transactionTrace is InvokeTransactionTrace)
        assertTrue(simulationResult[0].transactionTrace is CommonInvokeTransactionTrace)
        assertTrue(simulationResult[1].transactionTrace is DeployAccountTransactionTrace)
    }

    @ParameterizedTest
    @MethodSource("getConstNonceAccounts")
    fun `simulate declare v1 transaction`(accountAndProvider: AccountAndProvider) {
        assumeTrue(IntegrationConfig.isTestEnabled(requiresGas = false))
        val (account, sourceProvider) = accountAndProvider
        assumeTrue(sourceProvider is JsonRpcProvider)
        val provider = sourceProvider as JsonRpcProvider

        val contractCode = Path.of("src/test/resources/contracts_v0/target/release/providerTest.json").readText()
        val contractDefinition = Cairo0ContractDefinition(contractCode)
        val nonce = account.getNonce().send()

        // Note to future developers experiencing failures in this test.
        // 1. Compiled contract format sometimes changes, this causes changes in the class hash.
        // If this test starts randomly falling, try recalculating class hash.
        // 2. If it fails on CI, make sure to delete the compiled contracts before running this test.
        // Chances are, the contract was compiled with a different compiler version.

        val classHash = Felt.fromHex("0x3b32bb615844ea7a9a56a8966af1a5ba1457b1f5c9162927ca1968975b0d2a9")
        val declareTransactionPayload = account.signDeclare(
            contractDefinition,
            classHash,
            ExecutionParams(
                nonce = nonce,
                maxFee = Felt(1000000000000000L),
            ),
        )

        // Pathfinder currently always requires SKIP_FEE_CHARGE flag
        val simulationFlags = setOf(SimulationFlag.SKIP_FEE_CHARGE)
        val simulationResult = provider.simulateTransactions(
            transactions = listOf(declareTransactionPayload),
            blockTag = BlockTag.LATEST,
            simulationFlags = simulationFlags,
        ).send()
        assertEquals(1, simulationResult.size)
        val trace = simulationResult.first().transactionTrace
        assertTrue(trace is DeclareTransactionTrace)
    }

    @ParameterizedTest
    @MethodSource("getConstNonceAccounts")
    fun `simulate declare v2 transaction`(accountAndProvider: AccountAndProvider) {
        assumeTrue(IntegrationConfig.isTestEnabled(requiresGas = false))
        val (account, sourceProvider) = accountAndProvider
        assumeTrue(sourceProvider is JsonRpcProvider)
        val provider = sourceProvider as JsonRpcProvider

        ScarbClient.createSaltedContract(
            placeholderContractPath = Path.of("src/test/resources/contracts_v1/src/placeholder_hello_starknet.cairo"),
            saltedContractPath = Path.of("src/test/resources/contracts_v1/src/salted_hello_starknet.cairo"),
        )
        ScarbClient.buildContracts(Path.of("src/test/resources/contracts_v1"))
        val contractCode = Path.of("src/test/resources/contracts_v1/target/release/ContractsV1_SaltedHelloStarknet.sierra.json").readText()
        val casmCode = Path.of("src/test/resources/contracts_v1/target/release/ContractsV1_SaltedHelloStarknet.casm.json").readText()

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

        // Pathfinder currently always requires SKIP_FEE_CHARGE flag
        val simulationFlags = setOf(SimulationFlag.SKIP_FEE_CHARGE)
        val simulationResult = provider.simulateTransactions(
            transactions = listOf(declareTransactionPayload),
            blockTag = BlockTag.LATEST,
            simulationFlags = simulationFlags,
        ).send()
        assertEquals(1, simulationResult.size)
        val trace = simulationResult.first().transactionTrace
        assertTrue(trace is DeclareTransactionTrace)
    }
}
