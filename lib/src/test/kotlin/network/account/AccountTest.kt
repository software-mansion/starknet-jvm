package network.account

import com.swmansion.starknet.account.StandardAccount
import com.swmansion.starknet.crypto.StarknetCurve
import com.swmansion.starknet.data.ContractAddressCalculator
import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.data.types.transactions.*
import com.swmansion.starknet.deployercontract.StandardDeployer
import com.swmansion.starknet.extensions.toFelt
import com.swmansion.starknet.extensions.toUint256
import com.swmansion.starknet.provider.rpc.JsonRpcProvider
import com.swmansion.starknet.signer.StarkCurveSigner
import network.utils.NetworkConfig
import network.utils.NetworkConfig.Network
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import starknet.utils.ScarbClient
import java.math.BigInteger
import java.nio.file.Path
import kotlin.io.path.readText

@Execution(ExecutionMode.SAME_THREAD)
class AccountTest {
    companion object {
        @JvmStatic
        private val config = NetworkConfig.config
        private val network = config.network
        private val rpcUrl = config.rpcUrl
        private val accountAddress = config.accountAddress
        private val signer = StarkCurveSigner(config.privateKey)
        private val constNonceAccountAddress = config.constNonceAccountAddress ?: config.accountAddress
        private val constNonceSigner = StarkCurveSigner(config.constNoncePrivateKey ?: config.privateKey)
        private val provider = JsonRpcProvider(rpcUrl)
        private val cairoVersion = config.cairoVersion.toFelt

        val chainId = when (network) {
            Network.GOERLI_INTEGRATION -> StarknetChainId.GOERLI
            Network.GOERLI_TESTNET -> StarknetChainId.GOERLI
            Network.SEPOLIA_INTEGRATION -> StarknetChainId.INTEGRATION_SEPOLIA
            Network.SEPOLIA_TESTNET -> StarknetChainId.SEPOLIA
        }

        val standardAccount = StandardAccount(
            accountAddress,
            signer,
            provider,
            chainId,
            cairoVersion,
        )

        // Note to future developers:
        // Some tests may fail due to getNonce receiving higher nonce than expected by other methods
        // Only use this account for tests that don't change the state of the network (non-gas tests)
        val constNonceAccount = StandardAccount(
            constNonceAccountAddress,
            constNonceSigner,
            provider,
            chainId,
            cairoVersion,
        )

        private val predeclaredAccount = when (network) {
            Network.GOERLI_INTEGRATION -> DeclaredAccount(Felt.fromHex("0x5a9941d0cc16b8619a3325055472da709a66113afcc6a8ab86055da7d29c5f8"), Felt.ZERO)
            Network.GOERLI_TESTNET -> DeclaredAccount(Felt.fromHex("0x5a9941d0cc16b8619a3325055472da709a66113afcc6a8ab86055da7d29c5f8"), Felt.ZERO)
            Network.SEPOLIA_INTEGRATION -> DeclaredAccount(Felt.fromHex("0x2338634f11772ea342365abd5be9d9dc8a6f44f159ad782fdebd3db5d969738"), Felt.ONE)
            Network.SEPOLIA_TESTNET -> DeclaredAccount(Felt.fromHex("0x4c6d6cf894f8bc96bb9c525e6853e5483177841f7388f74a46cfda6f028c755"), Felt.ONE)
        }
        private val predeployedMapContractAddress = when (network) {
            Network.GOERLI_INTEGRATION -> Felt.fromHex("0x05cd21d6b3952a869fda11fa9a5bd2657bd68080d3da255655ded47a81c8bd53")
            Network.GOERLI_TESTNET -> Felt.fromHex("0x2BAe9749940E7b89613C1a21D9C832242447caA065D5A2b8AB08c0c469b3462")
            Network.SEPOLIA_TESTNET -> Felt.fromHex("0x06b248bde9ce00d69099304a527640bc9515a08f0b49e5168e2096656f207e1d")
            Network.SEPOLIA_INTEGRATION -> Felt.fromHex("0x061bbcfc1e11d8de0efcb502f9e1163b4033c74c7977cbb2b8c545164236a88c")
        }
        private val ethContractAddress = Felt.fromHex("0x049d36570d4e46f48e99674bd3fcc84644ddd6b96f7c741b1562b82f9e004dc7")
        private val strkContractAddress = Felt.fromHex("0x04718f5a0fc34cc1af16a1cdee98ffb20c31f5cd61d6ab07201858f4287c938d")
        private val udcAddress = Felt.fromHex("0x41a78e741e5af2fec34b695679bc6891742439f7afb8484ecd7766661ad02bf")

        data class DeclaredAccount(
            val classHash: Felt,
            val cairoVersion: Felt,
        )
    }

    @Test
    fun `estimate fee for invoke transaction`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val account = constNonceAccount

        val call = Call(
            contractAddress = predeployedMapContractAddress,
            entrypoint = "put",
            calldata = listOf(Felt.fromHex("0x1D2C3B7A8"), Felt.fromHex("0x451")),
        )
        val estimateFeeRequest = account.estimateFeeV1(
            listOf(call),
            skipValidate = false,
        )
        val estimateFeeResponse = estimateFeeRequest.send().first().overallFee
        assertTrue(estimateFeeResponse.value > Felt.ONE.value)
    }

    @Test
    fun `estimate fee for declare v2 transaction`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val account = constNonceAccount

        ScarbClient.buildSaltedContract(
            placeholderContractPath = Path.of("src/test/resources/contracts_v1/src/placeholder_hello_starknet.cairo"),
            saltedContractPath = Path.of("src/test/resources/contracts_v1/src/salted_hello_starknet.cairo"),
        )
        val contractCode = Path.of("src/test/resources/contracts_v1/target/release/ContractsV1_SaltedHelloStarknet.sierra.json").readText()
        val casmCode = Path.of("src/test/resources/contracts_v1/target/release/ContractsV1_SaltedHelloStarknet.casm.json").readText()

        val contractDefinition = Cairo1ContractDefinition(contractCode)
        val casmContractDefinition = CasmContractDefinition(casmCode)

        val nonce = account.getNonce().send()
        val declareTransactionPayload = account.signDeclareV2(
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
            chainId = chainId,
            nonce = nonce,
            maxFee = declareTransactionPayload.maxFee,
            signature = declareTransactionPayload.signature,
            version = declareTransactionPayload.version,
        )

        val feeEstimateRequest = provider.getEstimateFee(listOf(signedTransaction.toPayload()), BlockTag.LATEST, emptySet())

        val feeEstimate = feeEstimateRequest.send().first()
        assertNotEquals(Felt(0), feeEstimate.gasConsumed)
        assertNotEquals(Felt(0), feeEstimate.gasPrice)
        assertNotEquals(Felt(0), feeEstimate.overallFee)
    }

    @Test
    fun `estimate fee for declare v3 transaction`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val account = constNonceAccount

        ScarbClient.buildSaltedContract(
            placeholderContractPath = Path.of("src/test/resources/contracts_v2/src/placeholder_counter_contract.cairo"),
            saltedContractPath = Path.of("src/test/resources/contracts_v2/src/salted_counter_contract.cairo"),
        )
        val contractCode = Path.of("src/test/resources/contracts_v2/target/release/ContractsV2_SaltedCounterContract.sierra.json").readText()
        val casmCode = Path.of("src/test/resources/contracts_v2/target/release/ContractsV2_SaltedCounterContract.casm.json").readText()

        val contractDefinition = Cairo1ContractDefinition(contractCode)
        val casmContractDefinition = CasmContractDefinition(casmCode)

        val nonce = account.getNonce().send()
        val params = DeclareParamsV3(
            nonce = nonce,
            l1ResourceBounds = ResourceBounds.ZERO,
        )
        val declareTransactionPayload = account.signDeclareV3(
            sierraContractDefinition = contractDefinition,
            casmContractDefinition = casmContractDefinition,
            params = params,
            forFeeEstimate = true,
        )

        val feeEstimateRequest = provider.getEstimateFee(listOf(declareTransactionPayload), BlockTag.PENDING)

        val feeEstimate = feeEstimateRequest.send().first()
        assertNotEquals(Felt(0), feeEstimate.gasConsumed)
        assertNotEquals(Felt(0), feeEstimate.gasPrice)
        assertNotEquals(Felt(0), feeEstimate.overallFee)
    }

    @Test
    fun `sign and send declare v2 transaction`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = true))
        // Note to future developers experiencing experiencing failures in this test.
        // This test sometimes fails due to getNonce receiving higher (pending) nonce than addDeclareTransaction expects

        val account = standardAccount

        ScarbClient.buildSaltedContract(
            placeholderContractPath = Path.of("src/test/resources/contracts_v1/src/placeholder_hello_starknet.cairo"),
            saltedContractPath = Path.of("src/test/resources/contracts_v1/src/salted_hello_starknet.cairo"),
        )
        val contractCode = Path.of("src/test/resources/contracts_v1/target/release/ContractsV1_SaltedHelloStarknet.sierra.json").readText()
        val casmCode = Path.of("src/test/resources/contracts_v1/target/release/ContractsV1_SaltedHelloStarknet.casm.json").readText()

        val contractDefinition = Cairo1ContractDefinition(contractCode)
        val contractCasmDefinition = CasmContractDefinition(casmCode)
        val nonce = account.getNonce().send()

        val declareTransactionPayload = account.signDeclareV2(
            contractDefinition,
            contractCasmDefinition,
            ExecutionParams(nonce, Felt(1000000000000000L)),
        )
        val request = provider.declareContract(declareTransactionPayload)
        val result = request.send()

        Thread.sleep(60000)
        val receipt = provider.getTransactionReceipt(result.transactionHash).send()

        assertTrue(receipt.isAccepted)
    }

    @Test
    fun `sign and send declare v2 transaction (cairo compiler v2)`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = true))
        // Note to future developers experiencing experiencing failures in this test.
        // This test sometimes fails due to getNonce receiving higher (pending) nonce than addDeclareTransaction expects

        val account = standardAccount

        ScarbClient.buildSaltedContract(
            placeholderContractPath = Path.of("src/test/resources/contracts_v2/src/placeholder_counter_contract.cairo"),
            saltedContractPath = Path.of("src/test/resources/contracts_v2/src/salted_counter_contract.cairo"),
        )
        val contractCode = Path.of("src/test/resources/contracts_v2/target/release/ContractsV2_SaltedCounterContract.sierra.json").readText()
        val casmCode = Path.of("src/test/resources/contracts_v2/target/release/ContractsV2_SaltedCounterContract.casm.json").readText()

        val contractDefinition = Cairo1ContractDefinition(contractCode)
        val contractCasmDefinition = CasmContractDefinition(casmCode)
        val nonce = account.getNonce().send()

        val declareTransactionPayload = account.signDeclareV2(
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

    @Test
    fun `sign and send declare v3 transaction`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = true))
        // Note to future developers experiencing failures in this test.
        // This test sometimes fails due to getNonce receiving higher (pending) nonce than addDeclareTransaction expects

        // TODO: (#384) Test v3 transactions on Sepolia
        assumeTrue(network == Network.GOERLI_INTEGRATION)

        val account = standardAccount

        ScarbClient.buildSaltedContract(
            placeholderContractPath = Path.of("src/test/resources/contracts_v2/src/placeholder_counter_contract.cairo"),
            saltedContractPath = Path.of("src/test/resources/contracts_v2/src/salted_counter_contract.cairo"),
        )
        val contractCode = Path.of("src/test/resources/contracts_v2/target/release/ContractsV2_SaltedCounterContract.sierra.json").readText()
        val casmCode = Path.of("src/test/resources/contracts_v2/target/release/ContractsV2_SaltedCounterContract.casm.json").readText()

        val contractDefinition = Cairo1ContractDefinition(contractCode)
        val contractCasmDefinition = CasmContractDefinition(casmCode)
        val nonce = account.getNonce().send()

        val l1ResourceBounds = ResourceBounds(
            maxAmount = Uint64(5000),
            maxPricePerUnit = Uint128(200000000),
        )
        val params = DeclareParamsV3(
            nonce = nonce,
            l1ResourceBounds = l1ResourceBounds,
        )
        val declareTransactionPayload = account.signDeclareV3(
            contractDefinition,
            contractCasmDefinition,
            params,
        )
        val request = provider.declareContract(declareTransactionPayload)
        val result = request.send()

        Thread.sleep(60000)
        val receipt = provider.getTransactionReceipt(result.transactionHash).send()

        assertTrue(receipt.isAccepted)
    }

    @Test
    fun `sign and send invoke v1 transaction`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = true))
        // Note to future developers experiencing experiencing failures in this test.
        // This test sometimes fails due to getNonce receiving higher (pending) nonce than addInvokeTransaction expects

        val account = standardAccount

        val call = Call(
            contractAddress = predeployedMapContractAddress,
            entrypoint = "put",
            calldata = listOf(
                Felt.fromHex("0x1D2C3B7A8"),
                Felt.fromHex("0x451"),
            ),
        )

        val invokeRequest = account.executeV1(call, Felt(1000000000000000L))
        val invokeResponse = invokeRequest.send()

        Thread.sleep(20000)

        val receipt = provider.getTransactionReceipt(invokeResponse.transactionHash).send()
        assertTrue(receipt.isAccepted)
    }

    @Test
    fun `sign and send invoke v3 transaction`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = true))
        // Note to future developers experiencing experiencing failures in this test.
        // This test sometimes fails due to getNonce receiving higher (pending) nonce than addInvokeTransaction expects

        // TODO: (#384) Test v3 transactions on Sepolia
        assumeTrue(network == Network.GOERLI_INTEGRATION)

        val account = standardAccount

        val call = Call(
            contractAddress = predeployedMapContractAddress,
            entrypoint = "put",
            calldata = listOf(Felt.fromHex("0x1D2C3B7A8"), Felt.fromHex("0x451")),
        )

        val invokeRequest = account.executeV3(call)
        val invokeResponse = invokeRequest.send()

        Thread.sleep(10000)

        val receipt = provider.getTransactionReceipt(invokeResponse.transactionHash).send()
        assertTrue(receipt.isAccepted)
    }

    @Test
    fun `call contract`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        // Note to future developers:
        // This test might fail if someone deliberately changes the key's corresponding value in contract.
        val call = Call(
            contractAddress = predeployedMapContractAddress,
            entrypoint = "get",
            calldata = listOf(Felt.fromHex("0x1D2C3B7A8")),
        )

        val getRequest = provider.callContract(call)
        val getResponse = getRequest.send()
        val value = getResponse.first()
        assertNotEquals(Felt.ZERO, value)
    }

    @Test
    fun `estimate fee for deploy account`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val privateKey = Felt(System.currentTimeMillis())
        val publicKey = StarknetCurve.getPublicKey(privateKey)

        val (classHash, cairoVersion) = predeclaredAccount

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
            chainId,
            cairoVersion,
        )

        val payloadForFeeEstimation = account.signDeployAccountV1(
            classHash = classHash,
            salt = salt,
            calldata = calldata,
            maxFee = Felt.ZERO,
            nonce = Felt.ZERO,
            forFeeEstimate = true,
        )
        assertEquals(TransactionVersion.V1_QUERY, payloadForFeeEstimation.version)

        val feePayload = provider.getEstimateFee(listOf(payloadForFeeEstimation)).send()
        assertTrue(feePayload.first().overallFee.value > Felt.ONE.value)
    }

    @Test
    fun `get ETH balance`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val account = constNonceAccount
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

    @Test
    fun `transfer ETH`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = true))

        val account = standardAccount

        val recipientAccountAddress = constNonceAccountAddress

        val amount = Uint256(Felt.ONE)
        val call = Call(
            contractAddress = ethContractAddress,
            entrypoint = "transfer",
            calldata = listOf(recipientAccountAddress) + amount.toCalldata(),
        )

        val request = account.executeV1(call)
        val response = request.send()
        Thread.sleep(15000)

        val transferReceipt = provider.getTransactionReceipt(response.transactionHash).send()
        assertTrue(transferReceipt.isAccepted)
    }

    @Test
    fun `sign and send deploy account v1 transaction`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = true))

        val account = standardAccount

        val privateKey = Felt(System.currentTimeMillis())
        val publicKey = StarknetCurve.getPublicKey(privateKey)

        val (classHash, cairoVersion) = predeclaredAccount
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
            chainId,
            cairoVersion,
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

        val transferRequest = account.executeV1(call)
        val transferResponse = transferRequest.send()
        Thread.sleep(15000)

        val transferReceipt = provider.getTransactionReceipt(transferResponse.transactionHash).send()
        assertTrue(transferReceipt.isAccepted)

        val payload = deployedAccount.signDeployAccountV1(
            classHash = classHash,
            salt = salt,
            calldata = calldata,
            // 10*fee from estimate deploy account fee
            maxFee = Felt(deployMaxFee.value),
        )

        val response = provider.deployAccount(payload).send()

        // Make sure the address matches the calculated one
        // TODO: (#344) re-enable this assertion once the address is non-nullable again
        // assertEquals(deployedAccountAddress, response.address)

        // Make sure tx matches what we sent
        Thread.sleep(15000)

        val tx = provider.getTransaction(response.transactionHash).send() as DeployAccountTransactionV1
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

    @Test
    fun `sign and send deploy account v3 transaction`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = true))

        // TODO: (#384) Test v3 transactions on Sepolia
        assumeTrue(network == Network.GOERLI_INTEGRATION)

        val account = standardAccount

        val privateKey = Felt(System.currentTimeMillis())
        val publicKey = StarknetCurve.getPublicKey(privateKey)

        val (classHash, cairoVersion) = predeclaredAccount
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
            chainId,
            cairoVersion,
        )
        val payloadForFeeEstimate = deployedAccount.signDeployAccountV3(
            classHash = classHash,
            salt = salt,
            calldata = calldata,
            params = DeployAccountParamsV3(
                nonce = Felt.ZERO,
                l1ResourceBounds = ResourceBounds.ZERO,
            ),
            forFeeEstimate = true, // BUG: (#344) this should be true, but Pathfinder and Devnet claim that using query version produce invalid signature
        )
        val feeEstimateRequest = provider.getEstimateFee(listOf(payloadForFeeEstimate))

        val feeEstimate = feeEstimateRequest.send().first()
        val resourceBounds = feeEstimate.toResourceBounds()

        val call = Call(
            contractAddress = strkContractAddress,
            entrypoint = "transfer",
            calldata = listOf(deployedAccountAddress) +
                resourceBounds.l1Gas.toMaxFee().toUint256.toCalldata(),
        )

        val transferRequest = account.executeV3(call)
        val transferResponse = transferRequest.send()
        Thread.sleep(15000)

        val transferReceipt = provider.getTransactionReceipt(transferResponse.transactionHash).send()
        assertTrue(transferReceipt.isAccepted)

        val params = DeployAccountParamsV3(
            nonce = Felt.ZERO,
            l1ResourceBounds = resourceBounds.l1Gas,
        )
        val payload = deployedAccount.signDeployAccountV3(
            classHash = classHash,
            salt = salt,
            calldata = calldata,
            params = params,
            forFeeEstimate = false,
        )

        val response = provider.deployAccount(payload).send()

        // Make sure the address matches the calculated one
        // TODO: (#344) re-enable this assertion once the address is non-nullable again
        // assertEquals(deployedAccountAddress, response.address)

        // Make sure tx matches what we sent
        Thread.sleep(15000)

        val tx = provider.getTransaction(response.transactionHash).send() as DeployAccountTransactionV3
        assertEquals(payload.classHash, tx.classHash)
        assertEquals(payload.salt, tx.contractAddressSalt)
        assertEquals(payload.constructorCalldata, tx.constructorCalldata)
        assertEquals(payload.version, tx.version)
        assertEquals(payload.nonce, tx.nonce)
        assertEquals(payload.signature, tx.signature)

        val receipt = provider.getTransactionReceipt(response.transactionHash).send()
        assertTrue(receipt.isAccepted)
    }

    @Test
    fun `simulate multiple invoke transactions`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val account = constNonceAccount

        val nonce = account.getNonce(BlockTag.LATEST).send()
        val call = Call(
            contractAddress = predeployedMapContractAddress,
            entrypoint = "put",
            calldata = listOf(
                Felt(101),
                Felt(2137),
            ),
        )
        val params = ExecutionParams(
            nonce = nonce,
            maxFee = Felt(1_000_000_000_000_000),
        )
        val invokeTx = account.signV1(call, params)

        val call2 = Call(
            contractAddress = predeployedMapContractAddress,
            entrypoint = "put",
            calldata = listOf(
                Felt(1),
                Felt(2),
                Felt(3),
            ),
        )
        val params2 = ExecutionParams(
            nonce = nonce.value.add(BigInteger.ONE).toFelt,
            maxFee = Felt(1_000_000_000_000_000),
        )
        val invokeTx2 = account.signV1(call2, params2)

        // Use SKIP_FEE_CHARGE flag to avoid failure due to insufficient funds
        val simulationFlags = setOf(SimulationFlag.SKIP_FEE_CHARGE)

        val simulationResult = provider.simulateTransactions(
            transactions = listOf(invokeTx, invokeTx2),
            blockTag = BlockTag.LATEST,
            simulationFlags = simulationFlags,
        ).send()
        assertEquals(2, simulationResult.size)
        assertTrue(simulationResult[0].transactionTrace is InvokeTransactionTraceBase)
        assertTrue(simulationResult[0].transactionTrace is InvokeTransactionTrace)
        assertTrue(simulationResult[1].transactionTrace is InvokeTransactionTraceBase)
        assertTrue(simulationResult[1].transactionTrace is RevertedInvokeTransactionTrace)
        assertNotNull((simulationResult[1].transactionTrace as RevertedInvokeTransactionTrace).executeInvocation.revertReason)

        val invokeTxWithoutSignature = InvokeTransactionV1Payload(invokeTx.senderAddress, invokeTx.calldata, emptyList(), invokeTx.maxFee, invokeTx.version, invokeTx.nonce)
        val invokeTxWihtoutSignature2 = InvokeTransactionV1Payload(invokeTx2.senderAddress, invokeTx2.calldata, emptyList(), invokeTx2.maxFee, invokeTx2.version, invokeTx2.nonce)
        val simulationFlags2 = setOf(SimulationFlag.SKIP_FEE_CHARGE, SimulationFlag.SKIP_VALIDATE)
        val simulationResult2 = provider.simulateTransactions(
            transactions = listOf(invokeTxWithoutSignature, invokeTxWihtoutSignature2),
            blockTag = BlockTag.LATEST,
            simulationFlags = simulationFlags2,
        ).send()

        assertEquals(2, simulationResult2.size)
        assertTrue(simulationResult[0].transactionTrace is InvokeTransactionTraceBase)
        assertTrue(simulationResult[0].transactionTrace is InvokeTransactionTrace)
        assertTrue(simulationResult[1].transactionTrace is InvokeTransactionTraceBase)
        assertTrue(simulationResult[1].transactionTrace is RevertedInvokeTransactionTrace)
        assertNotNull((simulationResult[1].transactionTrace as RevertedInvokeTransactionTrace).executeInvocation.revertReason)
    }

    @Test
    fun `simulate deploy account transaction`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val privateKey = Felt(System.currentTimeMillis())
        val publicKey = StarknetCurve.getPublicKey(privateKey)

        val (classHash, cairoVersion) = predeclaredAccount
        val salt = Felt(System.currentTimeMillis())
        val calldata = listOf(publicKey)
        val deployedAccountAddress = ContractAddressCalculator.calculateAddressFromHash(classHash, calldata, salt)

        val deployedAccount = StandardAccount(deployedAccountAddress, privateKey, provider, chainId, cairoVersion)
        val deployAccountTx = deployedAccount.signDeployAccountV1(
            classHash = classHash,
            salt = salt,
            calldata = calldata,
            maxFee = Felt(1_000_000_000_000_000),
        )

        // Use SKIP_FEE_CHARGE flag to avoid having to transfer funds to the account
        val simulationFlags = setOf(SimulationFlag.SKIP_FEE_CHARGE)

        val simulationResult = provider.simulateTransactions(
            transactions = listOf(deployAccountTx),
            blockTag = BlockTag.LATEST,
            simulationFlags = simulationFlags,
        ).send()
        assertEquals(1, simulationResult.size)
        assertTrue(simulationResult[0].transactionTrace is DeployAccountTransactionTrace)

        val deployAccountTxWithoutSignature = DeployAccountTransactionV1Payload(deployAccountTx.classHash, deployAccountTx.salt, deployAccountTx.constructorCalldata, deployAccountTx.version, deployAccountTx.nonce, deployAccountTx.maxFee, emptyList())

        val simulationFlags2 = setOf(SimulationFlag.SKIP_FEE_CHARGE, SimulationFlag.SKIP_VALIDATE)
        val simulationResult2 = provider.simulateTransactions(
            transactions = listOf(deployAccountTxWithoutSignature),
            blockTag = BlockTag.LATEST,
            simulationFlags = simulationFlags2,
        ).send()

        assertEquals(1, simulationResult2.size)
        assertTrue(simulationResult[0].transactionTrace is DeployAccountTransactionTrace)
    }

    @Test
    fun `simulate declare v2 transaction`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val account = constNonceAccount

        ScarbClient.buildSaltedContract(
            placeholderContractPath = Path.of("src/test/resources/contracts_v1/src/placeholder_hello_starknet.cairo"),
            saltedContractPath = Path.of("src/test/resources/contracts_v1/src/salted_hello_starknet.cairo"),
        )
        val contractCode = Path.of("src/test/resources/contracts_v1/target/release/ContractsV1_SaltedHelloStarknet.sierra.json").readText()
        val casmCode = Path.of("src/test/resources/contracts_v1/target/release/ContractsV1_SaltedHelloStarknet.casm.json").readText()

        val contractDefinition = Cairo1ContractDefinition(contractCode)
        val casmContractDefinition = CasmContractDefinition(casmCode)

        val nonce = account.getNonce(BlockTag.LATEST).send()
        val declareTransactionPayload = account.signDeclareV2(
            contractDefinition,
            casmContractDefinition,
            ExecutionParams(
                nonce = nonce,
                maxFee = Felt(1000000000000000L),
            ),
        )

        // Use SKIP_FEE_CHARGE flag to avoid failure due to insufficient funds
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

    @Test
    fun `test udc deploy with parameters`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = true))

        assumeFalse(network == Network.GOERLI_INTEGRATION)
        val classHash = when (network) {
            Network.GOERLI_TESTNET -> Felt.fromHex("0x40971cb2233ff5680dc329121e03ae4af48082cf02d1082bcd07179610af39e")
            Network.SEPOLIA_TESTNET -> Felt.fromHex("0x040971cb2233ff5680dc329121e03ae4af48082cf02d1082bcd07179610af39e")
            Network.SEPOLIA_INTEGRATION -> Felt.fromHex("0x040971cb2233ff5680dc329121e03ae4af48082cf02d1082bcd07179610af39e")
            else -> throw NotImplementedError("Test is not yet supported for this network $network")
        }

        val account = standardAccount
        val deployer = StandardDeployer(udcAddress, provider, account)

        val deployment = deployer.deployContractV1(
            classHash = classHash,
            constructorCalldata = emptyList(),
            maxFee = Felt(1000000000000000L),
            unique = true,
            salt = Felt(System.currentTimeMillis()),
        ).send()
        Thread.sleep(120000)

        val address = deployer.findContractAddress(deployment).send()
        assertNotEquals(Felt.ZERO, address)
    }

    @Test
    fun `test udc deploy with constructor`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = true))

        assumeFalse(network == Network.GOERLI_INTEGRATION)
        val classHash = when (network) {
            Network.GOERLI_TESTNET -> Felt.fromHex("0x31de86764e5a6694939a87321dad5769d427790147a4ee96497ba21102c8af9")
            Network.SEPOLIA_TESTNET -> Felt.fromHex("0x8448a68b5ea1affc45e3fd4b8b480ea36a51dc34e337a16d2567d32d0c6f8a")
            Network.SEPOLIA_INTEGRATION -> Felt.fromHex("0x31de86764e5a6694939a87321dad5769d427790147a4ee96497ba21102c8af9")
            else -> throw NotImplementedError("Test is not yet supported for this network $network")
        }

        val account = standardAccount
        val deployer = StandardDeployer(udcAddress, provider, account)

        val initialBalance = Felt(1000)
        val deployment = deployer.deployContractV1(
            classHash = classHash,
            constructorCalldata = listOf(initialBalance),
            maxFee = Felt(1000000000000000L),
        ).send()
        Thread.sleep(120000)

        val address = deployer.findContractAddress(deployment).send()
        assertNotEquals(Felt.ZERO, address)
    }
}
