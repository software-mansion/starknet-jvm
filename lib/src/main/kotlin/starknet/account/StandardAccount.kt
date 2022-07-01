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
import types.Felt

class StandardAccount(
    private val provider: Provider,
    override val address: Felt,
    private val signer: Signer,
) : Account,
    Provider by provider {

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