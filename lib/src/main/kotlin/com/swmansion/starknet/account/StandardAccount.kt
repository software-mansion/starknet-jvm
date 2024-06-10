package com.swmansion.starknet.account

import com.swmansion.starknet.data.TypedData
import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.data.types.DeployAccountTransactionV1Payload
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
 * @param chainId the chain id of the Starknet network
 */
class StandardAccount (
    override val address: Felt,
    private val signer: Signer,
    private val provider: Provider,
    override val chainId: StarknetChainId,
) : Account {
    /**
     * @param provider a provider used to interact with Starknet
     * @param address the address of the account contract
     * @param privateKey a private key used to create a signer
     * @param chainId the chain id of the Starknet network
     */
    constructor(address: Felt, privateKey: Felt, provider: Provider, chainId: StarknetChainId) : this(
        address = address,
        signer = StarkCurveSigner(privateKey),
        provider = provider,
        chainId = chainId,
    )
    private lateinit var cairoVersion: CairoVersion

    private fun determineCairoVersion() {
        if(::cairoVersion.isInitialized) return
        val contract = provider.getClassAt(address).send()
        cairoVersion = if (contract is ContractClass) CairoVersion.ONE else CairoVersion.ZERO
    }

    override fun signV1(calls: List<Call>, params: ExecutionParams, forFeeEstimate: Boolean): InvokeTransactionV1Payload {
        determineCairoVersion()
        val calldata = AccountCalldataTransformer.callsToExecuteCalldata(calls, cairoVersion)
        val signVersion = if (forFeeEstimate) TransactionVersion.V1_QUERY else TransactionVersion.V1
        val tx = TransactionFactory.makeInvokeV1Transaction(
            senderAddress = address,
            calldata = calldata,
            chainId = chainId,
            nonce = params.nonce,
            maxFee = params.maxFee,
            version = signVersion,
        )

        val signedTransaction = tx.copy(signature = signer.signTransaction(tx))

        return signedTransaction.toPayload()
    }

    override fun signV3(calls: List<Call>, params: InvokeParamsV3, forFeeEstimate: Boolean): InvokeTransactionV3Payload {
        determineCairoVersion()
        val calldata = AccountCalldataTransformer.callsToExecuteCalldata(calls, cairoVersion)
        val signVersion = if (forFeeEstimate) TransactionVersion.V3_QUERY else TransactionVersion.V3
        val tx = TransactionFactory.makeInvokeV3Transaction(
            senderAddress = address,
            calldata = calldata,
            chainId = chainId,
            nonce = params.nonce,
            version = signVersion,
            resourceBounds = params.resourceBounds,
        )

        val signedTransaction = tx.copy(signature = signer.signTransaction(tx))

        return signedTransaction.toPayload()
    }

    override fun signDeployAccountV1(
        classHash: Felt,
        calldata: Calldata,
        salt: Felt,
        maxFee: Felt,
        nonce: Felt,
        forFeeEstimate: Boolean,
    ): DeployAccountTransactionV1Payload {
        val signVersion = if (forFeeEstimate) TransactionVersion.V1_QUERY else TransactionVersion.V1
        val tx = TransactionFactory.makeDeployAccountV1Transaction(
            classHash = classHash,
            contractAddress = address,
            salt = salt,
            calldata = calldata,
            chainId = chainId,
            maxFee = maxFee,
            version = signVersion,
            nonce = nonce,
        )
        val signedTransaction = tx.copy(signature = signer.signTransaction(tx))

        return signedTransaction.toPayload()
    }

    override fun signDeployAccountV3(
        classHash: Felt,
        calldata: Calldata,
        salt: Felt,
        params: DeployAccountParamsV3,
        forFeeEstimate: Boolean,
    ): DeployAccountTransactionV3Payload {
        val signVersion = if (forFeeEstimate) TransactionVersion.V3_QUERY else TransactionVersion.V3
        val tx = TransactionFactory.makeDeployAccountV3Transaction(
            classHash = classHash,
            senderAddress = address,
            salt = salt,
            calldata = calldata,
            chainId = chainId,
            version = signVersion,
            nonce = params.nonce,
            resourceBounds = params.resourceBounds,
        )
        val signedTransaction = tx.copy(signature = signer.signTransaction(tx))

        return signedTransaction.toPayload()
    }

    override fun signDeclareV2(
        sierraContractDefinition: Cairo1ContractDefinition,
        casmContractDefinition: CasmContractDefinition,
        params: ExecutionParams,
        forFeeEstimate: Boolean,
    ): DeclareTransactionV2Payload {
        val signVersion = if (forFeeEstimate) TransactionVersion.V2_QUERY else TransactionVersion.V2
        val tx = TransactionFactory.makeDeclareV2Transaction(
            contractDefinition = sierraContractDefinition,
            senderAddress = address,
            chainId = chainId,
            nonce = params.nonce,
            maxFee = params.maxFee,
            version = signVersion,
            casmContractDefinition = casmContractDefinition,
        )
        val signedTransaction = tx.copy(signature = signer.signTransaction(tx))

        return signedTransaction.toPayload()
    }

    override fun signDeclareV3(
        sierraContractDefinition: Cairo1ContractDefinition,
        casmContractDefinition: CasmContractDefinition,
        params: DeclareParamsV3,
        forFeeEstimate: Boolean,
    ): DeclareTransactionV3Payload {
        val signVersion = if (forFeeEstimate) TransactionVersion.V3_QUERY else TransactionVersion.V3
        val tx = TransactionFactory.makeDeclareV3Transaction(
            contractDefinition = sierraContractDefinition,
            senderAddress = address,
            chainId = chainId,
            nonce = params.nonce,
            version = signVersion,
            resourceBounds = params.resourceBounds,
            casmContractDefinition = casmContractDefinition,
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

    override fun executeV1(calls: List<Call>, maxFee: Felt): Request<InvokeFunctionResponse> {
        return getNonce().compose { nonce ->
            val signParams = ExecutionParams(nonce = nonce, maxFee = maxFee)
            val payload = signV1(calls, signParams)

            return@compose provider.invokeFunction(payload)
        }
    }

    override fun executeV3(calls: List<Call>, l1ResourceBounds: ResourceBounds): Request<InvokeFunctionResponse> {
        return getNonce().compose { nonce ->
            val signParams = InvokeParamsV3(
                nonce = nonce,
                l1ResourceBounds = l1ResourceBounds,
            )
            val payload = signV3(calls, signParams, false)

            return@compose provider.invokeFunction(payload)
        }
    }

    override fun executeV1(calls: List<Call>, estimateFeeMultiplier: Double): Request<InvokeFunctionResponse> {
        return estimateFeeV1(calls).compose { estimateFee ->
            val maxFee = estimateFee.values.first().toMaxFee(estimateFeeMultiplier)
            executeV1(calls, maxFee)
        }
    }

    override fun executeV3(
        calls: List<Call>,
        estimateAmountMultiplier: Double,
        estimateUnitPriceMultiplier: Double,
    ): Request<InvokeFunctionResponse> {
        return estimateFeeV3(calls).compose { estimateFee ->
            val resourceBounds = estimateFee.values.first().toResourceBounds(
                amountMultiplier = estimateAmountMultiplier,
                unitPriceMultiplier = estimateUnitPriceMultiplier,
            )
            executeV3(calls, resourceBounds.l1Gas)
        }
    }

    override fun executeV1(calls: List<Call>): Request<InvokeFunctionResponse> {
        return estimateFeeV1(calls).compose { estimateFee ->
            val maxFee = estimateFee.values.first().toMaxFee()
            executeV1(calls, maxFee)
        }
    }

    override fun executeV3(calls: List<Call>): Request<InvokeFunctionResponse> {
        return estimateFeeV3(calls).compose { estimateFee ->
            val resourceBounds = estimateFee.values.first().toResourceBounds()
            executeV3(calls, resourceBounds.l1Gas)
        }
    }

    override fun executeV1(call: Call, maxFee: Felt): Request<InvokeFunctionResponse> {
        return executeV1(listOf(call), maxFee)
    }

    override fun executeV3(call: Call, l1ResourceBounds: ResourceBounds): Request<InvokeFunctionResponse> {
        return executeV3(listOf(call), l1ResourceBounds)
    }

    override fun executeV1(call: Call, estimateFeeMultiplier: Double): Request<InvokeFunctionResponse> {
        return executeV1(listOf(call), estimateFeeMultiplier)
    }

    override fun executeV3(
        call: Call,
        estimateAmountMultiplier: Double,
        estimateUnitPriceMultiplier: Double,
    ): Request<InvokeFunctionResponse> {
        return executeV3(
            calls = listOf(call),
            estimateAmountMultiplier = estimateAmountMultiplier,
            estimateUnitPriceMultiplier = estimateUnitPriceMultiplier,
        )
    }

    override fun executeV1(call: Call): Request<InvokeFunctionResponse> {
        return executeV1(listOf(call))
    }

    override fun executeV3(call: Call): Request<InvokeFunctionResponse> {
        return executeV3(listOf(call))
    }

    override fun getNonce(): Request<Felt> = getNonce(BlockTag.PENDING)

    override fun getNonce(blockTag: BlockTag) = provider.getNonce(address, blockTag)

    override fun getNonce(blockHash: Felt) = provider.getNonce(address, blockHash)

    override fun getNonce(blockNumber: Int) = provider.getNonce(address, blockNumber)

    override fun estimateFeeV1(call: Call): Request<EstimateFeeResponseList> {
        return estimateFeeV1(listOf(call))
    }

    override fun estimateFeeV3(call: Call): Request<EstimateFeeResponseList> {
        return estimateFeeV3(listOf(call))
    }

    override fun estimateFeeV1(call: Call, skipValidate: Boolean): Request<EstimateFeeResponseList> {
        return estimateFeeV1(listOf(call), skipValidate)
    }

    override fun estimateFeeV3(call: Call, skipValidate: Boolean): Request<EstimateFeeResponseList> {
        return estimateFeeV3(listOf(call), skipValidate)
    }

    override fun estimateFeeV1(call: Call, blockTag: BlockTag): Request<EstimateFeeResponseList> {
        return estimateFeeV1(listOf(call), blockTag)
    }

    override fun estimateFeeV3(call: Call, blockTag: BlockTag): Request<EstimateFeeResponseList> {
        return estimateFeeV3(listOf(call), blockTag)
    }

    override fun estimateFeeV1(
        call: Call,
        blockTag: BlockTag,
        skipValidate: Boolean,
    ): Request<EstimateFeeResponseList> {
        return estimateFeeV1(listOf(call), blockTag, skipValidate)
    }

    override fun estimateFeeV3(
        call: Call,
        blockTag: BlockTag,
        skipValidate: Boolean,
    ): Request<EstimateFeeResponseList> {
        return estimateFeeV3(listOf(call), blockTag, skipValidate)
    }

    override fun estimateFeeV1(calls: List<Call>): Request<EstimateFeeResponseList> {
        return estimateFeeV1(calls, BlockTag.PENDING, false)
    }

    override fun estimateFeeV3(calls: List<Call>): Request<EstimateFeeResponseList> {
        return estimateFeeV3(calls, BlockTag.PENDING, false)
    }

    override fun estimateFeeV1(calls: List<Call>, skipValidate: Boolean): Request<EstimateFeeResponseList> {
        return estimateFeeV1(calls, BlockTag.PENDING, skipValidate)
    }

    override fun estimateFeeV3(calls: List<Call>, skipValidate: Boolean): Request<EstimateFeeResponseList> {
        return estimateFeeV3(calls, BlockTag.PENDING, skipValidate)
    }

    override fun estimateFeeV1(calls: List<Call>, blockTag: BlockTag): Request<EstimateFeeResponseList> {
        return estimateFeeV1(calls, blockTag, false)
    }

    override fun estimateFeeV3(calls: List<Call>, blockTag: BlockTag): Request<EstimateFeeResponseList> {
        return estimateFeeV3(calls, blockTag, false)
    }

    override fun estimateFeeV1(
        calls: List<Call>,
        blockTag: BlockTag,
        skipValidate: Boolean,
    ): Request<EstimateFeeResponseList> {
        return getNonce(blockTag).compose { nonce ->
            val simulationFlags = prepareSimulationFlagsForFeeEstimate(skipValidate)
            val payload = buildEstimateFeeV1Payload(calls, nonce)
            return@compose provider.getEstimateFee(payload, blockTag, simulationFlags)
        }
    }

    override fun estimateFeeV3(
        calls: List<Call>,
        blockTag: BlockTag,
        skipValidate: Boolean,
    ): Request<EstimateFeeResponseList> {
        return getNonce(blockTag).compose { nonce ->
            val payload = buildEstimateFeeV3Payload(calls, nonce)
            val simulationFlags = prepareSimulationFlagsForFeeEstimate(skipValidate)
            return@compose provider.getEstimateFee(payload, blockTag, simulationFlags)
        }
    }

    private fun buildEstimateFeeV1Payload(calls: List<Call>, nonce: Felt): List<TransactionPayload> {
        val executionParams = ExecutionParams(nonce = nonce, maxFee = Felt.ZERO)
        val payload = signV1(calls, executionParams, true)

        val signedTransaction = TransactionFactory.makeInvokeV1Transaction(
            senderAddress = payload.senderAddress,
            calldata = payload.calldata,
            chainId = chainId,
            nonce = nonce,
            maxFee = payload.maxFee,
            signature = payload.signature,
            version = payload.version,
        )
        return listOf(signedTransaction.toPayload())
    }

    private fun buildEstimateFeeV3Payload(calls: List<Call>, nonce: Felt): List<TransactionPayload> {
        val executionParams = InvokeParamsV3(
            nonce = nonce,
            l1ResourceBounds = ResourceBounds.ZERO,
        )
        val payload = signV3(calls, executionParams, true)

        val signedTransaction = TransactionFactory.makeInvokeV3Transaction(
            senderAddress = payload.senderAddress,
            calldata = payload.calldata,
            chainId = chainId,
            nonce = nonce,
            signature = payload.signature,
            version = payload.version,
            resourceBounds = payload.resourceBounds,
        )
        return listOf(signedTransaction.toPayload())
    }

    private fun prepareSimulationFlagsForFeeEstimate(skipValidate: Boolean): Set<SimulationFlagForEstimateFee> {
        return if (skipValidate) {
            setOf(SimulationFlagForEstimateFee.SKIP_VALIDATE)
        } else {
            emptySet()
        }
    }

    private fun estimateVersion(version: Felt): Felt {
        return BigInteger.valueOf(2).pow(128)
            .add(version.value)
            .toFelt
    }
}
