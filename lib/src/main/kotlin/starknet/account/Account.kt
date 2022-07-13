package starknet.account

import starknet.data.types.Call
import starknet.data.types.ExecutionParams
import starknet.data.types.InvokeTransaction
import starknet.provider.Provider
import starknet.data.types.Felt

/**
 * An account interface.
 *
 * Implementers of this interface provide methods for signing transactions.
 */
interface Account : Provider {
    val address: Felt

    /**
     * Sign a transaction.
     *
     * Sign a transaction to be executed on StarkNet.
     *
     * @param call a call to be signed
     * @param params additional execution parameters for the transaction
     * @return signed invoke function
     */
    fun sign(call: Call, params: ExecutionParams): InvokeTransaction {
        return sign(listOf(call), params)
    }

    // TODO: ABI?
    /**
     * Sign multiple transactions.
     *
     * Sign a list of calls to be executed on StarkNet.
     *
     * @param calls a list of calls to be signed
     * @param params additional execution parameters for the transaction
     * @return signed invoke function
     */
    fun sign(calls: List<Call>, params: ExecutionParams): InvokeTransaction
}

