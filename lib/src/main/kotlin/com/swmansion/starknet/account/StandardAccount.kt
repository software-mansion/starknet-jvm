package com.swmansion.starknet.account

import com.swmansion.starknet.crypto.StarknetCurve
import com.swmansion.starknet.crypto.estimatedFeeToMaxFee
import com.swmansion.starknet.data.EXECUTE_ENTRY_POINT_NAME
import com.swmansion.starknet.data.selectorFromName
import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.data.types.transactions.DeployAccountTransactionPayload
import com.swmansion.starknet.data.types.transactions.InvokeFunctionPayload
import com.swmansion.starknet.data.types.transactions.TransactionFactory
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
    override val address: Felt,
    private val signer: Signer,
    private val provider: Provider,
) : Account {
    private val version = Felt.ONE

    /**
     * @param provider a provider used to interact with StarkNet
     * @param address the address of the account contract
     * @param privateKey a private key used to create a signer
     */
    constructor(address: Felt, privateKey: Felt, provider: Provider) : this(
        address,
        StarkCurveSigner(privateKey),
        provider,
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

    override fun signDeployAccount(
        classHash: Felt,
        calldata: Calldata,
        salt: Felt,
        maxFee: Felt,
    ): DeployAccountTransactionPayload {
        val tx = TransactionFactory.makeDeployAccountTransaction(
            classHash = classHash,
            salt = salt,
            calldata = calldata,
            chainId = provider.chainId,
            maxFee = maxFee,
            version = version,
        )
        val signedTransaction = tx.copy(signature = signer.signTransaction(tx))

        return signedTransaction.toPayload()
    }

    override fun sign(
        contractDefinition: ContractDefinition,
        classHash: Felt,
        params: ExecutionParams,
    ): DeclareTransactionPayload {
        val hash = StarknetCurve.pedersenOnElements(
            TransactionType.DECLARE.txPrefix,
            version,
            address,
            Felt.ZERO,
            StarknetCurve.pedersenOnElements(classHash),
            params.maxFee,
            provider.chainId.value,
            params.nonce,
        )
        val transaction = DeclareTransaction(
            classHash = classHash,
            senderAddress = address,
            hash = hash,
            maxFee = params.maxFee,
            version = version,
            signature = emptyList(),
            nonce = params.nonce,
            contractDefinition = contractDefinition,
        )
        return transaction.copy(signature = signer.signTransaction(transaction)).toPayload()
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

    override fun getNonce(): Request<Felt> = provider.getNonce(address, BlockTag.PENDING)

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
