package com.swmansion.starknet.account

import com.swmansion.starknet.crypto.estimatedFeeToMaxFee
import com.swmansion.starknet.data.EXECUTE_ENTRY_POINT_NAME
import com.swmansion.starknet.data.selectorFromName
import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.data.types.transactions.*
import com.swmansion.starknet.extensions.compose
import com.swmansion.starknet.provider.Provider
import com.swmansion.starknet.provider.Request
import com.swmansion.starknet.signer.Signer
import com.swmansion.starknet.signer.StarkCurveSigner

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
) : Account {
    private val version = Felt.ONE

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
        val calldata = callsToExecuteCalldata(calls)
        val tx = TransactionFactory.makeInvokeTransaction(
            contractAddress = address,
            entryPointSelector = selectorFromName(EXECUTE_ENTRY_POINT_NAME),
            calldata = calldata,
            chainId = provider.chainId,
            maxFee = params.maxFee,
            nonce = params.nonce,
            version = version,
        )

        val signedTransaction = tx.copy(signature = signer.signTransaction(tx))

        return signedTransaction.toPayload()
    }

    override fun execute(calls: List<Call>, maxFee: Felt): Request<InvokeFunctionResponse> {
        return getNonce().compose { nonce ->
            val signParams = ExecutionParams(nonce = nonce, maxFee = maxFee)
            val payload = sign(calls, signParams)

            return@compose provider.invokeFunction(payload)
        }
    }

    override fun execute(calls: List<Call>): Request<InvokeFunctionResponse> {
        return estimateFee(calls).compose { estimateFee ->
            val maxFee = estimatedFeeToMaxFee(estimateFee.overallFee)
            execute(calls, maxFee)
        }
    }

    override fun getNonce(): Request<Felt> {
        return provider.getNonce(address)
    }

    override fun estimateFee(calls: List<Call>): Request<EstimateFeeResponse> {
        return getNonce().compose { buildEstimateFeeRequest(calls, it) }
    }

    private fun buildEstimateFeeRequest(calls: List<Call>, nonce: Felt): Request<EstimateFeeResponse> {
        val executionParams = ExecutionParams(nonce = nonce, maxFee = Felt.ZERO)
        val payload = sign(calls, executionParams)

        val signedTransaction = TransactionFactory.makeInvokeTransaction(
            contractAddress = payload.invocation.contractAddress,
            calldata = payload.invocation.calldata,
            entryPointSelector = payload.invocation.entrypoint,
            chainId = provider.chainId,
            maxFee = payload.maxFee,
            version = payload.version,
            signature = payload.signature,
            nonce = nonce,
        )

        return provider.getEstimateFee(signedTransaction, BlockTag.LATEST)
    }
}
