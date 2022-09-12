package com.swmansion.starknet.account

import com.swmansion.starknet.crypto.FeeUtils
import com.swmansion.starknet.data.EXECUTE_ENTRY_POINT_NAME
import com.swmansion.starknet.data.selectorFromName
import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.data.types.transactions.*
import com.swmansion.starknet.provider.Provider
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
    private val chainId: StarknetChainId,
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
        // TODO(make sure using this parameter is a good idea)
        provider.chainId,
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

    override fun execute(calls: List<Call>, maxFee: Felt): InvokeFunctionResponse {
        val nonce = getNonce()
        val signParams = ExecutionParams(nonce = nonce, maxFee = maxFee)
        val payload = sign(calls, signParams)

        return provider.invokeFunction(payload).send()
    }

    override fun execute(calls: List<Call>): InvokeFunctionResponse {
        val estimateFeeResponse = estimateFee(calls)
        val maxFee = FeeUtils.estimatedFeeToMaxFee(estimateFeeResponse.overallFee)
        return execute(calls, maxFee)
    }

    override fun getNonce(): Felt {
        val request = provider.getNonce(address)

        return request.send()
    }

    override fun estimateFee(calls: List<Call>): EstimateFeeResponse {
        val nonce = getNonce()

        val executionParams = ExecutionParams(nonce = nonce, maxFee = Felt.ZERO)
        val payload = sign(calls, executionParams)

        val signedTransaction = TransactionFactory.makeInvokeTransaction(
            contractAddress = payload.invocation.contractAddress,
            calldata = payload.invocation.calldata,
            entryPointSelector = payload.invocation.entrypoint,
            chainId = chainId,
            maxFee = payload.maxFee,
            version = payload.version,
            signature = payload.signature,
            nonce = nonce,
        )

        return provider.getEstimateFee(signedTransaction, BlockTag.LATEST).send()
    }
}
