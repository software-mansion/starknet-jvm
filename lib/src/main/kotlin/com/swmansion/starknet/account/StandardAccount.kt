package com.swmansion.starknet.account

import com.swmansion.starknet.crypto.HashMethod
import com.swmansion.starknet.crypto.StarknetCurveSignature
import com.swmansion.starknet.data.TypedData
import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.extensions.compose
import com.swmansion.starknet.extensions.toFelt
import com.swmansion.starknet.helpers.hashMethodFromRpcVersion
import com.swmansion.starknet.provider.Provider
import com.swmansion.starknet.provider.Request
import com.swmansion.starknet.provider.exceptions.RequestFailedException
import com.swmansion.starknet.signer.Signer
import com.swmansion.starknet.signer.StarkCurveSigner
import io.github.z4kn4fein.semver.Version
import io.github.z4kn4fein.semver.toVersion
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
    private val hashMethod: HashMethod =
        hashMethodFromRpcVersion(provider.getSpecVersion().send().value.toVersion())
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
            tip = params.tip,
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
            tip = params.tip,
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
            tip = params.tip,
            hashMethod = hashMethod,
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

        return request.toBooleanRequest()
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

    override fun executeV3(calls: List<Call>, resourceBounds: ResourceBoundsMapping): Request<InvokeFunctionResponse> {
        return executeV3(calls, resourceBounds, Uint64.ZERO)
    }

    override fun executeV3(calls: List<Call>, resourceBounds: ResourceBoundsMapping, tip: Uint64): Request<InvokeFunctionResponse> {
        return getNonce().compose { nonce ->
            val signParams = InvokeParamsV3(
                nonce = nonce,
                resourceBounds = resourceBounds,
                tip = tip,
            )
            val payload = signV3(calls, signParams, false)
            return@compose provider.invokeFunction(payload)
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
     * @sample starknet.account.StandardAccountTest.InvokeTest.executeV3MultipleCalls
     */
    override fun executeV3(calls: List<Call>): Request<InvokeFunctionResponse> {
        return estimateFeeV3(calls).compose { estimateFee ->
            val resourceBounds = estimateFee.values.first().toResourceBounds()
            executeV3(calls, resourceBounds)
        }
    }

    override fun executeV3(calls: List<Call>, tip: Uint64): Request<InvokeFunctionResponse> {
        return estimateFeeV3(calls).compose { estimateFee ->
            val resourceBounds = estimateFee.values.first().toResourceBounds()
            executeV3(calls, resourceBounds, tip)
        }
    }

    override fun executeV3(call: Call, resourceBounds: ResourceBoundsMapping): Request<InvokeFunctionResponse> {
        return executeV3(listOf(call), resourceBounds)
    }

    override fun executeV3(
        call: Call,
        resourceBounds: ResourceBoundsMapping,
        tip: Uint64,
    ): Request<InvokeFunctionResponse> {
        return executeV3(listOf(call), resourceBounds, tip)
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
     * @sample starknet.account.StandardAccountTest.InvokeTest.executeV3SingleCall
     */
    override fun executeV3(call: Call): Request<InvokeFunctionResponse> {
        return executeV3(listOf(call))
    }

    override fun executeV3(call: Call, tip: Uint64): Request<InvokeFunctionResponse> {
        return executeV3(listOf(call), tip)
    }

    /**
     * @sample starknet.account.StandardAccountTest.NonceTest.getNonce
     */
    override fun getNonce(): Request<Felt> = getNonce(BlockTag.PRE_CONFIRMED)

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

    override fun estimateFeeV3(call: Call): Request<EstimateFeeResponseList> {
        return estimateFeeV3(listOf(call))
    }

    override fun estimateFeeV3(call: Call, skipValidate: Boolean): Request<EstimateFeeResponseList> {
        return estimateFeeV3(listOf(call), skipValidate)
    }

    override fun estimateFeeV3(call: Call, blockTag: BlockTag): Request<EstimateFeeResponseList> {
        return estimateFeeV3(listOf(call), blockTag)
    }

    override fun estimateFeeV3(
        call: Call,
        blockTag: BlockTag,
        skipValidate: Boolean,
    ): Request<EstimateFeeResponseList> {
        return estimateFeeV3(listOf(call), blockTag, skipValidate)
    }

    override fun estimateFeeV3(calls: List<Call>): Request<EstimateFeeResponseList> {
        return estimateFeeV3(calls, BlockTag.PRE_CONFIRMED, false)
    }

    /**
     * @sample starknet.account.StandardAccountTest.InvokeEstimateTest.estimateFeeForInvokeV3Transaction
     */
    override fun estimateFeeV3(calls: List<Call>, skipValidate: Boolean): Request<EstimateFeeResponseList> {
        return estimateFeeV3(calls, BlockTag.PRE_CONFIRMED, skipValidate)
    }

    override fun estimateFeeV3(calls: List<Call>, blockTag: BlockTag): Request<EstimateFeeResponseList> {
        return estimateFeeV3(calls, blockTag, false)
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

    override fun isValidOutsideExecutionNonce(nonce: Felt): Request<Boolean> {
        return provider.callContract(
            Call(
                contractAddress = address,
                entrypoint = "is_valid_outside_execution_nonce",
                nonce.toCalldata(),
            ),
        ).toBooleanRequest()
    }

    override fun getOutsideExecutionNonce(): Felt {
        return getOutsideExecutionNonce(retryLimit = 10)
    }

    override fun getOutsideExecutionNonce(retryLimit: Int): Felt {
        repeat(retryLimit) {
            val randomNonce = generatePrivateKey()
            if (isValidOutsideExecutionNonce(randomNonce).send()) {
                return randomNonce
            }
        }
        throw NonceGenerationException()
    }

    override fun signOutsideExecutionCallV2(
        caller: Felt,
        executeAfter: Felt,
        executeBefore: Felt,
        calls: List<Call>,
        nonce: Felt,
    ): Call {
        val execution = OutsideExecutionV2(
            caller = caller,
            nonce = nonce,
            executeAfter = executeAfter,
            executeBefore = executeBefore,
            calls = calls.map {
                OutsideCallV2(
                    to = it.contractAddress,
                    selector = it.entrypoint,
                    calldata = it.calldata,
                )
            },
        )
        val message = execution.toTypedData(chainId)

        val (r, s) = signTypedData(message)

        val outsideTransaction = OutsideTransaction(
            outsideExecution = execution,
            signature = StarknetCurveSignature(r, s),
            signerAddress = address,
        )
        return Call(
            contractAddress = address,
            "execute_from_outside_v2",
            outsideTransaction.toCalldata(),
        )
    }

    override fun signOutsideExecutionCallV2(
        caller: Felt,
        executeAfter: Felt,
        executeBefore: Felt,
        call: Call,
        nonce: Felt,
    ): Call {
        return signOutsideExecutionCallV2(
            caller = caller,
            executeAfter = executeAfter,
            executeBefore = executeBefore,
            calls = listOf(call),
            nonce = nonce,
        )
    }

    override fun signOutsideExecutionCallV2(
        caller: Felt,
        executeAfter: Felt,
        executeBefore: Felt,
        calls: List<Call>,
    ): Call {
        return signOutsideExecutionCallV2(
            caller = caller,
            executeAfter = executeAfter,
            executeBefore = executeBefore,
            calls = calls,
            nonce = getOutsideExecutionNonce(),
        )
    }

    override fun signOutsideExecutionCallV2(
        caller: Felt,
        executeAfter: Felt,
        executeBefore: Felt,
        call: Call,
    ): Call {
        return signOutsideExecutionCallV2(
            caller = caller,
            executeAfter = executeAfter,
            executeBefore = executeBefore,
            calls = listOf(call),
            nonce = getOutsideExecutionNonce(),
        )
    }

    private fun buildEstimateFeeV3Payload(calls: List<Call>, nonce: Felt): List<ExecutableTransaction> {
        val executionParams = InvokeParamsV3(
            nonce = nonce,
            resourceBounds = ResourceBoundsMapping.ZERO,
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

    private fun Request<FeltArray>.toBooleanRequest(): Request<Boolean> {
        val request = this@toBooleanRequest
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
}
