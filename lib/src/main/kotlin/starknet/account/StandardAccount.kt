package starknet.account

import starknet.data.EXECUTE_ENTRY_POINT_NAME
import starknet.data.selectorFromName
import starknet.data.types.Call
import starknet.data.types.ExecutionParams
import starknet.data.types.InvokeTransaction
import starknet.data.types.callsToExecuteCalldata
import starknet.provider.Provider
import starknet.signer.Signer
import starknet.signer.StarkCurveSigner
import starknet.data.types.Felt

/**
 * Standard account used in starknet.
 *
 * @param provider a provider used to interact with starknet
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
     * @param provider a provider used to interact with starknet
     * @param address the address of the account contract
     * @param privateKey a private key used to create a signer
     */
    constructor(provider: Provider, address: Felt, privateKey: Felt) : this(
        provider,
        address,
        StarkCurveSigner(privateKey)
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
}