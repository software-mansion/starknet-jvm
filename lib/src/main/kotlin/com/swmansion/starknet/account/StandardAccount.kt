package com.swmansion.starknet.account

import com.swmansion.starknet.data.TypedData
import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.extensions.compose
import com.swmansion.starknet.extensions.toFelt
import com.swmansion.starknet.provider.Provider
import com.swmansion.starknet.provider.Request
import com.swmansion.starknet.provider.exceptions.RequestFailedException
import com.swmansion.starknet.signer.Signer
import com.swmansion.starknet.signer.StarkCurveSigner
import java.math.BigInteger
import java.security.SecureRandom
import java.util.concurrent.CompletableFuture

/**
 * Standard account used in Starknet.
 *
 * @param provider a provider used to interact with Starknet
 * @param address the address of the account contract
 * @param signer a signer instance used to sign transactions
 * @param chainId the chain id of the Starknet network
 * @param cairoVersion the version of Cairo language in which account contract is written
 */
class StandardAccount @JvmOverloads constructor(
    override val address: Felt,
    private val signer: Signer,
    private val provider: Provider,
    override val chainId: StarknetChainId,
    private val cairoVersion: CairoVersion = CairoVersion.ONE,
) : Account {
    /**
     * @param address the address of the account contract
     * @param privateKey a private key used to create a signer
     * @param provider a provider used to interact with Starknet
     * @param chainId the chain id of the Starknet network
     * @param cairoVersion the version of Cairo language in which account contract is written
     */
    @JvmOverloads
    constructor(
        address: Felt,
        privateKey: Felt,
        provider: Provider,
        chainId: StarknetChainId,
        cairoVersion: CairoVersion = CairoVersion.ONE,
    ) : this(
        address = address,
        signer = StarkCurveSigner(privateKey),
        provider = provider,
        chainId = chainId,
        cairoVersion = cairoVersion,
    )

    companion object {
        /**
         * Factory method to create a StandardAccount instance with automatic Cairo version determination.
         *
         * @param address the address of the account contract
         * @param signer a signer instance used to sign transactions
         * @param provider a provider used to interact with Starknet
         * @param chainId the chain id of the Starknet network
         * @return a StandardAccount instance with detected Cairo version
         * @sample starknet.account.StandardAccountTest.createCairo1AccountWithAutomaticVersionDetection
         */
        @JvmStatic
        fun create(
            address: Felt,
            signer: Signer,
            provider: Provider,
            chainId: StarknetChainId,
        ): StandardAccount {
            val cairoVersion = detectCairoVersion(provider, address)
            return StandardAccount(address, signer, provider, chainId, cairoVersion)
        }

        /**
         * Factory method to create a StandardAccount instance with a private key and automatic Cairo version determination.
         *
         * @param address the address of the account contract
         * @param privateKey a private key used to create a signer
         * @param provider a provider used to interact with Starknet
         * @param chainId the chain id of the Starknet network
         * @return a StandardAccount instance with detected Cairo version
         */
        @JvmStatic
        fun create(
            address: Felt,
            privateKey: Felt,
            provider: Provider,
            chainId: StarknetChainId,
        ): StandardAccount {
            val signer = StarkCurveSigner(privateKey)
            val cairoVersion = detectCairoVersion(provider, address)
            return StandardAccount(address, signer, provider, chainId, cairoVersion)
        }

        private fun detectCairoVersion(provider: Provider, address: Felt): CairoVersion {
            val contract = provider.getClassAt(address).send()
            return if (contract is ContractClass) CairoVersion.ONE else CairoVersion.ZERO
        }

        /**
         * Generate a random private key.
         *
         * @return private key
         */
        @JvmStatic
        fun generatePrivateKey(): Felt {
            val random = SecureRandom()
            val randomBytes = ByteArray(32)
            random.nextBytes(randomBytes)
            val randomInt = BigInteger(1, randomBytes)
            val privateKey = randomInt % Felt.PRIME
            return privateKey.toFelt
        }
    }

    /**
     * @sample starknet.account.StandardAccountTest.InvokeTest.signV1MultipleCalls
     */
    override fun signV1(calls: List<Call>, params: ExecutionParams, forFeeEstimate: Boolean): InvokeTransactionV1 {
        val calldata = AccountCalldataTransformer.callsToExecuteCalldata(calls, cairoVersion.version)
        val tx = InvokeTransactionV1(
            senderAddress = address,
            calldata = calldata,
            chainId = chainId,
            nonce = params.nonce,
            maxFee = params.maxFee,
            forFeeEstimate = forFeeEstimate,
        )

        val signedTransaction = tx.copy(signature = signer.signTransaction(tx))

        return signedTransaction
    }

    /**
     * @sample starknet.account.StandardAccountTest.InvokeTest.signV3MultipleCalls
     */
    override fun signV3(calls: List<Call>, params: InvokeParamsV3, forFeeEstimate: Boolean): InvokeTransactionV3 {
        val calldata = AccountCalldataTransformer.callsToExecuteCalldata(calls, cairoVersion.version)
        val tx = InvokeTransactionV3(
            senderAddress = address,
            calldata = calldata,
            chainId = chainId,
            nonce = params.nonce,
            forFeeEstimate = forFeeEstimate,
            resourceBounds = params.resourceBounds,
        )

        val signedTransaction = tx.copy(signature = signer.signTransaction(tx))

        return signedTransaction
    }

    /**
     * @sample starknet.account.StandardAccountTest.DeployAccountTest.signAndSendDeployAccountV1Transaction
     */
    override fun signDeployAccountV1(
        classHash: Felt,
        calldata: Calldata,
        salt: Felt,
        maxFee: Felt,
        nonce: Felt,
        forFeeEstimate: Boolean,
    ): DeployAccountTransactionV1 {
        val tx = DeployAccountTransactionV1(
            classHash = classHash,
            contractAddress = address,
            salt = salt,
            calldata = calldata,
            chainId = chainId,
            maxFee = maxFee,
            forFeeEstimate = forFeeEstimate,
            nonce = nonce,
        )
        val signedTransaction = tx.copy(signature = signer.signTransaction(tx))

        return signedTransaction
    }

    /**
     * @sample starknet.account.StandardAccountTest.DeployAccountTest.signAndSendDeployAccountV3Transaction
     */
    override fun signDeployAccountV3(
        classHash: Felt,
        calldata: Calldata,
        salt: Felt,
        params: DeployAccountParamsV3,
        forFeeEstimate: Boolean,
    ): DeployAccountTransactionV3 {
        val tx = DeployAccountTransactionV3(
            classHash = classHash,
            senderAddress = address,
            salt = salt,
            calldata = calldata,
            chainId = chainId,
            forFeeEstimate = forFeeEstimate,
            nonce = params.nonce,
            resourceBounds = params.resourceBounds,
        )
        val signedTransaction = tx.copy(signature = signer.signTransaction(tx))

        return signedTransaction
    }

    /**
     * @sample starknet.account.StandardAccountTest.DeclareTest.signAndSendDeclareV2Transaction
     */
    override fun signDeclareV2(
        sierraContractDefinition: Cairo1ContractDefinition,
        casmContractDefinition: CasmContractDefinition,
        params: ExecutionParams,
        forFeeEstimate: Boolean,
    ): DeclareTransactionV2 {
        val tx = DeclareTransactionV2(
            contractDefinition = sierraContractDefinition,
            senderAddress = address,
            chainId = chainId,
            nonce = params.nonce,
            maxFee = params.maxFee,
            casmContractDefinition = casmContractDefinition,
            forFeeEstimate = forFeeEstimate,
        )
        val signedTransaction = tx.copy(signature = signer.signTransaction(tx))

        return signedTransaction
    }

    /**
     * @sample starknet.account.StandardAccountTest.DeclareTest.signAndSendDeclareV3Transaction
     */
    override fun signDeclareV3(
        sierraContractDefinition: Cairo1ContractDefinition,
        casmContractDefinition: CasmContractDefinition,
        params: DeclareParamsV3,
        forFeeEstimate: Boolean,
    ): DeclareTransactionV3 {
        val tx = DeclareTransactionV3(
            contractDefinition = sierraContractDefinition,
            senderAddress = address,
            chainId = chainId,
            nonce = params.nonce,
            forFeeEstimate = forFeeEstimate,
            resourceBounds = params.resourceBounds,
            casmContractDefinition = casmContractDefinition,
        )
        val signedTransaction = tx.copy(signature = signer.signTransaction(tx))

        return signedTransaction
    }

    /**
     * @sample starknet.account.StandardAccountTest.SignTypedDataTest.signTypedDataRevision1
     */
    override fun signTypedData(typedData: TypedData): Signature {
        return signer.signTypedData(typedData, address)
    }

    /**
     * @sample starknet.account.StandardAccountTest.SignTypedDataTest.signTypedDataRevision1
     */
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

    override fun executeV3(calls: List<Call>, resourceBounds: ResourceBoundsMapping): Request<InvokeFunctionResponse> {
        return getNonce().compose { nonce ->
            val signParams = InvokeParamsV3(
                nonce = nonce,
                resourceBounds = resourceBounds,
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
            executeV3(calls, resourceBounds)
        }
    }

    /**
     * @sample starknet.account.StandardAccountTest.InvokeTest.executeV1MultipleCalls
     */
    override fun executeV1(calls: List<Call>): Request<InvokeFunctionResponse> {
        return estimateFeeV1(calls).compose { estimateFee ->
            val maxFee = estimateFee.values.first().toMaxFee()
            executeV1(calls, maxFee)
        }
    }

    /**
     * @sample starknet.account.StandardAccountTest.InvokeTest.executeV3MultipleCalls
     */
    override fun executeV3(calls: List<Call>): Request<InvokeFunctionResponse> {
        return estimateFeeV3(calls).compose { estimateFee ->
            val resourceBounds = estimateFee.values.first().toResourceBounds()
            executeV3(calls, resourceBounds)
        }
    }

    override fun executeV1(call: Call, maxFee: Felt): Request<InvokeFunctionResponse> {
        return executeV1(listOf(call), maxFee)
    }

    override fun executeV3(call: Call, resourceBounds: ResourceBoundsMapping): Request<InvokeFunctionResponse> {
        return executeV3(listOf(call), resourceBounds)
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

    /**
     * @sample starknet.account.StandardAccountTest.InvokeTest.executeV1SingleCall
     */
    override fun executeV1(call: Call): Request<InvokeFunctionResponse> {
        return executeV1(listOf(call))
    }

    /**
     * @sample starknet.account.StandardAccountTest.InvokeTest.executeV3SingleCall
     */
    override fun executeV3(call: Call): Request<InvokeFunctionResponse> {
        return executeV3(listOf(call))
    }

    /**
     * @sample starknet.account.StandardAccountTest.NonceTest.getNonce
     */
    override fun getNonce(): Request<Felt> = getNonce(BlockTag.PENDING)

    /**
     * @sample starknet.account.StandardAccountTest.NonceTest.getNonceAtLatestBlockTag
     */
    override fun getNonce(blockTag: BlockTag) = provider.getNonce(address, blockTag)

    /**
     * @sample starknet.account.StandardAccountTest.NonceTest.getNonceAtBlockHash
     */
    override fun getNonce(blockHash: Felt) = provider.getNonce(address, blockHash)

    /**
     * @sample starknet.account.StandardAccountTest.NonceTest.getNonceAtBlockNumber
     */
    override fun getNonce(blockNumber: Int) = provider.getNonce(address, blockNumber)

    /**
     * @sample starknet.account.StandardAccountTest.InvokeEstimateTest.estimateFeeForInvokeV1Transaction
     */
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

    /**
     * @sample starknet.account.StandardAccountTest.InvokeEstimateTest.estimateFeeForInvokeV1TransactionAtLatestBlockTag
     */
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

    /**
     * @sample starknet.account.StandardAccountTest.InvokeEstimateTest.estimateFeeForInvokeV3Transaction
     */
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

    private fun buildEstimateFeeV1Payload(calls: List<Call>, nonce: Felt): List<ExecutableTransaction> {
        val executionParams = ExecutionParams(nonce = nonce, maxFee = Felt.ZERO)
        val payload = signV1(calls, executionParams, true)

        val signedTransaction = InvokeTransactionV1(
            senderAddress = payload.senderAddress,
            calldata = payload.calldata,
            chainId = chainId,
            nonce = nonce,
            maxFee = payload.maxFee,
            signature = payload.signature,
            forFeeEstimate = true,
        )
        return listOf(signedTransaction)
    }

    private fun buildEstimateFeeV3Payload(calls: List<Call>, nonce: Felt): List<ExecutableTransaction> {
        val executionParams = InvokeParamsV3(
            nonce = nonce,
            resourceBounds = ResourceBoundsMapping(
                ResourceBounds.ZERO,
                ResourceBounds.ZERO,
            ),
        )
        val payload = signV3(calls, executionParams, true)

        val signedTransaction = InvokeTransactionV3(
            senderAddress = payload.senderAddress,
            calldata = payload.calldata,
            chainId = chainId,
            nonce = nonce,
            signature = payload.signature,
            resourceBounds = payload.resourceBounds,
            forFeeEstimate = true,
        )
        return listOf(signedTransaction)
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
