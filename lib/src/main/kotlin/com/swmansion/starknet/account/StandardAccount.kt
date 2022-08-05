package com.swmansion.starknet.account

import com.swmansion.starknet.data.EXECUTE_ENTRY_POINT_NAME
import com.swmansion.starknet.data.selectorFromName
import com.swmansion.starknet.data.types.*
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

    override fun sign(calls: List<Call>, params: ExecutionParams): InvokeTransaction {
        val calldata = callsToExecuteCalldata(calls, params.nonce)
        val tx = InvokeTransaction(
            contractAddress = address,
            entrypointSelector = selectorFromName(EXECUTE_ENTRY_POINT_NAME),
            calldata = calldata,
            chainId = provider.chainId.value,
            nonce = params.nonce,
            maxFee = params.maxFee,
            version = params.version,
        )
        return tx.copy(signature = signer.signTransaction(tx))
    }

    override fun execute(calls: List<Call>, params: CallParams): InvokeFunctionResponse {
        TODO("To be implemented")
        val nonce = params.nonce ?: getNonce()
        val maxFee = params.maxFee ?: Felt(0) // FIXME: Estimate fee
        val version = params.version ?: Felt(0)

        val calldata = callsToExecuteCalldata(calls, nonce)
        val call = Call(address, selectorFromName(EXECUTE_ENTRY_POINT_NAME), calldata)
    }

    override fun getNonce(): Felt {
        val nonceCall = Call(address, "get_nonce")
        val request = provider.callContract(nonceCall, BlockTag.LATEST)
        val response = request.send()

        return response.result.first()
    }

    override fun estimateFee(calls: List<Call>, params: CallParams): Felt {
        TODO("Not yet implemented")
    }
}
