package com.swmansion.starknet.account

import com.swmansion.starknet.data.TypedData
import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.data.types.transactions.*
import com.swmansion.starknet.data.types.transactions.DeployAccountTransactionV1Payload
import com.swmansion.starknet.extensions.compose
import com.swmansion.starknet.extensions.toFelt
import com.swmansion.starknet.provider.Provider
import com.swmansion.starknet.provider.Request
import com.swmansion.starknet.provider.exceptions.RequestFailedException
import com.swmansion.starknet.signer.Signer
import com.swmansion.starknet.signer.StarkCurveSigner
import java.math.BigInteger
import java.util.concurrent.CompletableFuture

/**
 * Standard account used in Starknet.
 *
 * @param provider a provider used to interact with Starknet
 * @param address the address of the account contract
 * @param signer a signer instance used to sign transactions
 */
class StandardAccount(
    override val address: Felt,
    private val signer: Signer,
    private val provider: Provider,
    private val cairoVersion: Felt = Felt.ZERO,
) : Account {
    private fun estimateVersion(version: Felt): Felt {
        return BigInteger.valueOf(2).pow(128)
            .add(version.value)
            .toFelt
    }
    private val defaultFeeEstimateSimulationFlags: Set<SimulationFlagForEstimateFee> by lazy {
        setOf(SimulationFlagForEstimateFee.SKIP_VALIDATE)
    }

    /**
     * @param provider a provider used to interact with Starknet
     * @param address the address of the account contract
     * @param privateKey a private key used to create a signer
     */
    constructor(address: Felt, privateKey: Felt, provider: Provider, cairoVersion: Felt = Felt.ZERO) : this(
        address,
        StarkCurveSigner(privateKey),
        provider,
        cairoVersion,
    )

    override fun sign(calls: List<Call>, params: ExecutionParams, forFeeEstimate: Boolean): InvokeTransactionV1Payload {
        val calldata = AccountCalldataTransformer.callsToExecuteCalldata(calls, cairoVersion)
        val signVersion = when (forFeeEstimate) {
            true -> estimateVersion(Felt.ONE)
            false -> Felt.ONE
        }
        val tx = TransactionFactory.makeInvokeV1Transaction(
            senderAddress = address,
            calldata = calldata,
            chainId = provider.chainId,
            nonce = params.nonce,
            maxFee = params.maxFee,
            version = signVersion,
        )

        val signedTransaction = tx.copy(signature = signer.signTransaction(tx))

        return signedTransaction.toPayload()
    }

    override fun sign(calls: List<Call>, params: ExecutionParamsV3, forFeeEstimate: Boolean): InvokeTransactionV3Payload {
        val calldata = AccountCalldataTransformer.callsToExecuteCalldata(calls, cairoVersion)
        val signVersion = when (forFeeEstimate) {
            true -> estimateVersion(Felt(3))
            false -> Felt(3)
        }
        val tx = TransactionFactory.makeInvokeV3Transaction(
            senderAddress = address,
            calldata = calldata,
            chainId = provider.chainId,
            nonce = params.nonce,
            version = signVersion,
            resourceBounds = params.resourceBounds,
            tip = params.tip,
            paymasterData = params.paymasterData,
            accountDeploymentData = params.accountDeploymentData,
            nonceDataAvailabilityMode = params.nonceDataAvailabilityMode,
            feeDataAvailabilityMode = params.feeDataAvailabilityMode,
        )

        val signedTransaction = tx.copy(signature = signer.signTransaction(tx))

        return signedTransaction.toPayload()
    }

    override fun signDeployAccount(
        classHash: Felt,
        calldata: Calldata,
        salt: Felt,
        maxFee: Felt,
        nonce: Felt,
        forFeeEstimate: Boolean,
    ): DeployAccountTransactionV1Payload {
        val signVersion = when (forFeeEstimate) {
            true -> estimateVersion(Felt.ONE)
            false -> Felt.ONE
        }
        val tx = TransactionFactory.makeDeployAccountV1Transaction(
            classHash = classHash,
            contractAddress = address,
            salt = salt,
            calldata = calldata,
            chainId = provider.chainId,
            maxFee = maxFee,
            version = signVersion,
            nonce = nonce,
        )
        val signedTransaction = tx.copy(signature = signer.signTransaction(tx))

        return signedTransaction.toPayload()
    }

    override fun signDeployAccount(
        classHash: Felt,
        calldata: Calldata,
        salt: Felt,
        params: DeployAccountParamsV3,
        forFeeEstimate: Boolean,
    ): DeployAccountTransactionV3Payload {
        val signVersion = when (forFeeEstimate) {
            true -> estimateVersion(Felt(3))
            false -> Felt(3)
        }
        val tx = TransactionFactory.makeDeployAccountV3Transaction(
            classHash = classHash,
            senderAddress = address,
            salt = salt,
            calldata = calldata,
            chainId = provider.chainId,
            version = signVersion,
            nonce = params.nonce,
            resourceBounds = params.resourceBounds,
            tip = params.tip,
            paymasterData = params.paymasterData,
            nonceDataAvailabilityMode = params.nonceDataAvailabilityMode,
            feeDataAvailabilityMode = params.feeDataAvailabilityMode,
        )
        val signedTransaction = tx.copy(signature = signer.signTransaction(tx))

        return signedTransaction.toPayload()
    }

    override fun signDeclare(
        contractDefinition: Cairo0ContractDefinition,
        classHash: Felt,
        params: ExecutionParams,
        forFeeEstimate: Boolean,
    ): DeclareTransactionV1Payload {
        val signVersion = when (forFeeEstimate) {
            true -> estimateVersion(Felt.ONE)
            false -> Felt.ONE
        }
        val tx = TransactionFactory.makeDeclareV1Transaction(
            contractDefinition = contractDefinition,
            classHash = classHash,
            senderAddress = address,
            chainId = provider.chainId,
            nonce = params.nonce,
            maxFee = params.maxFee,
            version = signVersion,
        )
        val signedTransaction = tx.copy(signature = signer.signTransaction(tx))

        return signedTransaction.toPayload()
    }

    override fun signDeclare(
        sierraContractDefinition: Cairo1ContractDefinition,
        casmContractDefinition: CasmContractDefinition,
        params: ExecutionParams,
        forFeeEstimate: Boolean,
    ): DeclareTransactionV2Payload {
        val signVersion = when (forFeeEstimate) {
            true -> estimateVersion(Felt(2))
            false -> Felt(2)
        }
        val tx = TransactionFactory.makeDeclareV2Transaction(
            contractDefinition = sierraContractDefinition,
            senderAddress = address,
            chainId = provider.chainId,
            nonce = params.nonce,
            maxFee = params.maxFee,
            version = signVersion,
            casmContractDefinition = casmContractDefinition,
        )
        val signedTransaction = tx.copy(signature = signer.signTransaction(tx))

        return signedTransaction.toPayload()
    }

    override fun signDeclare(
        sierraContractDefinition: Cairo1ContractDefinition,
        casmContractDefinition: CasmContractDefinition,
        params: DeclareParamsV3,
        forFeeEstimate: Boolean,
    ): DeclareTransactionV3Payload {
        val signVersion = when (forFeeEstimate) {
            true -> estimateVersion(Felt(3))
            false -> Felt(3)
        }
        val tx = TransactionFactory.makeDeclareV3Transaction(
            contractDefinition = sierraContractDefinition,
            senderAddress = address,
            chainId = provider.chainId,
            nonce = params.nonce,
            version = signVersion,
            resourceBounds = params.resourceBounds,
            tip = params.tip,
            paymasterData = params.paymasterData,
            accountDeploymentData = params.accountDeploymentData,
            casmContractDefinition = casmContractDefinition,
            nonceDataAvailabilityMode = params.nonceDataAvailabilityMode,
            feeDataAvailabilityMode = params.feeDataAvailabilityMode,
        )
        val signedTransaction = tx.copy(signature = signer.signTransaction(tx))

        return signedTransaction.toPayload()
    }

    override fun signTypedData(typedData: TypedData): Signature {
        return signer.signTypedData(typedData, address)
    }

    override fun verifyTypedDataSignature(typedData: TypedData, signature: Signature): Request<Boolean> {
        val messageHash = typedData.getMessageHash(address)
        val calldata = listOf(messageHash, Felt(signature.size)) + signature
        val call = Call(address, "isValidSignature", calldata)
        val request = provider.callContract(call)

        return object : Request<Boolean> {
            override fun send(): Boolean {
                return try {
                    val result = request.send()
                    result[0] > Felt.ZERO
                } catch (e: RequestFailedException) {
                    return handleValidationError(e)
                }
            }

            override fun sendAsync(): CompletableFuture<Boolean> {
                return request.sendAsync().handle { result, exception ->
                    if (exception is RequestFailedException) {
                        return@handle handleValidationError(exception)
                    }
                    return@handle result[0] > Felt.ZERO
                }
            }
        }
    }

    /**
     * Check if the error message contains part like `Signature ..., is invalid`
     *
     * Account contract `isValidSignature` signature raises an error instead of
     * returning `0` on invalid signature. We have to check the call error to verify
     * if it was caused by invalid signature or some other problem.
     */
    private fun handleValidationError(e: RequestFailedException): Boolean {
        val regex = """Signature\s.+,\sis\sinvalid""".toRegex()
        if (e.message?.let { regex.containsMatchIn(it) } == true) {
            return false
        }
        if (e.data?.let { regex.containsMatchIn(it) } == true) {
            return false
        }
        throw e
    }

    override fun execute(calls: List<Call>, maxFee: Felt): Request<InvokeFunctionResponse> {
        return getNonce().compose { nonce ->
            val signParams = ExecutionParams(nonce = nonce, maxFee = maxFee)
            val payload = sign(calls, signParams)

            return@compose provider.invokeFunction(payload)
        }
    }

    override fun executeV3(calls: List<Call>, l1ResourceBounds: ResourceBounds): Request<InvokeFunctionResponse> {
        return getNonce().compose { nonce ->
            val signParams = ExecutionParamsV3(
                nonce = nonce,
                l1ResourceBounds = l1ResourceBounds,
            )
            val payload = sign(calls, signParams, false)

            return@compose provider.invokeFunction(payload)
        }
    }

    override fun execute(calls: List<Call>): Request<InvokeFunctionResponse> {
        return estimateFee(calls).compose { estimateFee ->
            val maxFee = estimateFee.first().toMaxFee()
            execute(calls, maxFee)
        }
    }

    override fun executeV3(calls: List<Call>): Request<InvokeFunctionResponse> {
        return estimateFee(calls).compose { estimateFee ->
            val resourceBounds = estimateFee.first().toResourceBounds()
            executeV3(calls, resourceBounds.l1Gas)
        }
    }

    override fun execute(call: Call, maxFee: Felt): Request<InvokeFunctionResponse> {
        return execute(listOf(call), maxFee)
    }

    override fun executeV3(call: Call, l1ResourceBounds: ResourceBounds): Request<InvokeFunctionResponse> {
        return executeV3(listOf(call), l1ResourceBounds)
    }

    override fun execute(call: Call): Request<InvokeFunctionResponse> {
        return execute(listOf(call))
    }

    override fun executeV3(call: Call): Request<InvokeFunctionResponse> {
        return executeV3(listOf(call))
    }

    override fun getNonce(): Request<Felt> = getNonce(BlockTag.PENDING)

    override fun getNonce(blockTag: BlockTag) = provider.getNonce(address, blockTag)

    override fun getNonce(blockHash: Felt) = provider.getNonce(address, blockHash)

    override fun getNonce(blockNumber: Int) = provider.getNonce(address, blockNumber)

    override fun estimateFee(call: Call): Request<List<EstimateFeeResponse>> {
        return estimateFee(listOf(call))
    }

    override fun estimateFee(call: Call, simulationFlags: Set<SimulationFlagForEstimateFee>): Request<List<EstimateFeeResponse>> {
        return estimateFee(listOf(call), simulationFlags)
    }

    override fun estimateFee(call: Call, blockTag: BlockTag): Request<List<EstimateFeeResponse>> {
        return estimateFee(listOf(call), blockTag)
    }

    override fun estimateFee(
        call: Call,
        blockTag: BlockTag,
        simulationFlags: Set<SimulationFlagForEstimateFee>,
    ): Request<List<EstimateFeeResponse>> {
        return estimateFee(listOf(call), blockTag, simulationFlags)
    }

    override fun estimateFee(calls: List<Call>): Request<List<EstimateFeeResponse>> {
        return estimateFee(calls, BlockTag.PENDING)
    }

    override fun estimateFeeV3(calls: List<Call>): Request<List<EstimateFeeResponse>> {
        return estimateFeeV3(calls, BlockTag.PENDING, defaultFeeEstimateSimulationFlags)
    }

    override fun estimateFee(
        calls: List<Call>,
        simulationFlags: Set<SimulationFlagForEstimateFee>,
    ): Request<List<EstimateFeeResponse>> {
        return estimateFee(calls, BlockTag.PENDING, simulationFlags)
    }

    override fun estimateFeeV3(
        calls: List<Call>,
        simulationFlags: Set<SimulationFlagForEstimateFee>,
    ): Request<List<EstimateFeeResponse>> {
        return estimateFeeV3(calls, BlockTag.PENDING, simulationFlags)
    }

    override fun estimateFeeV3(calls: List<Call>, blockTag: BlockTag): Request<List<EstimateFeeResponse>> {
        return estimateFeeV3(calls, blockTag)
    }

    override fun estimateFee(calls: List<Call>, blockTag: BlockTag): Request<List<EstimateFeeResponse>> {
        return estimateFee(calls, blockTag, defaultFeeEstimateSimulationFlags)
    }

    override fun estimateFee(
        calls: List<Call>,
        blockTag: BlockTag,
        simulationFlags: Set<SimulationFlagForEstimateFee>,
    ): Request<List<EstimateFeeResponse>> {
        return getNonce(blockTag).compose { nonce ->
            val payload = buildEstimateFeePayload(calls, nonce)
            return@compose provider.getEstimateFee(payload, blockTag, simulationFlags)
        }
    }

    override fun estimateFeeV3(
        call: Call,
        simulationFlags: Set<SimulationFlagForEstimateFee>,
    ): Request<List<EstimateFeeResponse>> {
        return estimateFeeV3(listOf(call), simulationFlags)
    }

    override fun estimateFeeV3(call: Call, blockTag: BlockTag): Request<List<EstimateFeeResponse>> {
        return estimateFeeV3(listOf(call), blockTag)
    }

    override fun estimateFeeV3(
        call: Call,
        blockTag: BlockTag,
        simulationFlags: Set<SimulationFlagForEstimateFee>,
    ): Request<List<EstimateFeeResponse>> {
        return estimateFeeV3(listOf(call), blockTag, simulationFlags)
    }

    override fun estimateFeeV3(
        calls: List<Call>,
        blockTag: BlockTag,
        simulationFlags: Set<SimulationFlagForEstimateFee>,
    ): Request<List<EstimateFeeResponse>> {
        return getNonce(blockTag).compose { nonce ->
            val payload = buildEstimateFeeV3Payload(calls, nonce)
            return@compose provider.getEstimateFee(payload, blockTag, simulationFlags)
        }
    }

    private fun buildEstimateFeePayload(calls: List<Call>, nonce: Felt): List<TransactionPayload> {
        val executionParams = ExecutionParams(nonce = nonce, maxFee = Felt.ZERO)
        val payload = sign(calls, executionParams, true)

        val signedTransaction = TransactionFactory.makeInvokeV1Transaction(
            senderAddress = payload.senderAddress,
            calldata = payload.calldata,
            chainId = provider.chainId,
            nonce = nonce,
            maxFee = payload.maxFee,
            signature = payload.signature,
            version = payload.version,
        )
        return listOf(signedTransaction.toPayload())
    }

    private fun buildEstimateFeeV3Payload(calls: List<Call>, nonce: Felt): List<TransactionPayload> {
        val executionParams = ExecutionParamsV3(
            nonce = nonce,
            l1ResourceBounds = ResourceBounds.ZERO,
        )
        val payload = sign(calls, executionParams, true)

        val signedTransaction = TransactionFactory.makeInvokeV3Transaction(
            senderAddress = payload.senderAddress,
            calldata = payload.calldata,
            chainId = provider.chainId,
            nonce = nonce,
            signature = payload.signature,
            version = payload.version,
            resourceBounds = payload.resourceBounds,
            tip = payload.tip,
            paymasterData = payload.paymasterData,
            accountDeploymentData = payload.accountDeploymentData,
            nonceDataAvailabilityMode = payload.nonceDataAvailabilityMode,
            feeDataAvailabilityMode = payload.feeDataAvailabilityMode,
        )
        return listOf(signedTransaction.toPayload())
    }
}
