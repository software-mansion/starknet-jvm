package starknet.account

import starknet.data.EXECUTE_ENTRY_POINT_NAME
import starknet.data.selectorFromName
import starknet.data.types.Call
import starknet.data.types.ExecutionParams
import starknet.data.types.InvokeTransaction
import starknet.data.types.callsToExecuteCalldata
import starknet.provider.Provider
import starknet.signer.Signer
import types.Felt

class StandardAccount(
    private val provider: Provider,
    private val signer: Signer,
    override val address: Felt
) : Account,
    Provider by provider {
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