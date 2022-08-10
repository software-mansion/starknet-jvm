package com.swmansion.starknet.account

import com.swmansion.starknet.data.types.Call
import com.swmansion.starknet.data.types.ExecutionParams
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.transactions.*
import com.swmansion.starknet.provider.Provider

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
