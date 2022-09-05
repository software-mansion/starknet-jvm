package com.swmansion.starknet.account

import com.swmansion.starknet.crypto.FeeUtils
import com.swmansion.starknet.data.EXECUTE_ENTRY_POINT_NAME
import com.swmansion.starknet.data.selectorFromName
import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.data.types.transactions.*
import com.swmansion.starknet.provider.Provider
import com.swmansion.starknet.signer.Signer
import com.swmansion.starknet.signer.StarkCurveSigner
import kotlin.math.max

/**
 * Standard account used in StarkNet.
 *
 * @param provider a provider used to interact with StarkNet
 * @param address the address of the account contract
 * @param signer a signer instance used to sign transactions
 */
class StandardAccount(
    private val provider: Provider,
    override val address: Felt,
    private val signer: Signer,
) : Account,
    Provider by provider {

    /**
     * @param provider a provider used to interact with StarkNet
     * @param address the address of the account contract
     * @param privateKey a private key used to create a signer
     */
    constructor(provider: Provider, address: Felt, privateKey: Felt) : this(
        provider,
        address,
        StarkCurveSigner(privateKey),
    )

    override fun sign(calls: List<Call>, params: ExecutionParams): InvokeFunctionPayload {
        val calldata = callsToExecuteCalldata(calls, params.nonce)
        val tx = TransactionFactory.makeInvokeTransaction(
            contractAddress = address,
            entryPointSelector = selectorFromName(EXECUTE_ENTRY_POINT_NAME),
            calldata = calldata,
            chainId = provider.chainId,
            maxFee = params.maxFee,
            nonce = params.nonce,
            version = params.version,
        )

        val signedTransaction = tx.copy(signature = signer.signTransaction(tx))

        return signedTransaction.toPayload()
    }

    override fun execute(calls: List<Call>, params: CallParams?): InvokeFunctionResponse {
        val nonce = params?.nonce ?: getNonce()
        val version = params?.version ?: Felt(0)

        val maxFee: Felt = if (params?.maxFee != null) {
            params.maxFee
        } else {
            val estimateFeeParams = EstimateFeeParams(nonce = nonce)
            val estimateFeeResponse = estimateFee(calls, estimateFeeParams)
            Felt(FeeUtils.estimatedFeeToMaxFee(estimateFeeResponse.overallFee))
        }

        val signParams = ExecutionParams(nonce = nonce, maxFee = maxFee, version = version)
        val payload = sign(calls, signParams)

        return invokeFunction(payload).send()
    }

    override fun getNonce(): Felt {
        val nonceCall = Call(address, "get_nonce")
        val request = provider.callContract(nonceCall, BlockTag.LATEST)
        val response = request.send()

        return response.result.first()
    }

    override fun estimateFee(calls: List<Call>, params: EstimateFeeParams?): EstimateFeeResponse {
        val nonce = params?.nonce ?: getNonce()

        val executionParams = ExecutionParams(nonce = nonce, maxFee = Felt.ZERO, version = Felt.ZERO)
        val payload = sign(calls, executionParams)

        val signedTransaction = TransactionFactory.makeInvokeTransaction(
            contractAddress = payload.invocation.contractAddress,
            calldata = payload.invocation.calldata,
            entryPointSelector = payload.invocation.entrypoint,
            chainId = chainId,
            maxFee = payload.maxFee,
            version = payload.version,
            signature = payload.signature,
            nonce = nonce
        )

        return getEstimateFee(signedTransaction, BlockTag.LATEST).send()
    }
}
