package com.swmansion.starknet.account

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
}
