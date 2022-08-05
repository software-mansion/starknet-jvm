package com.swmansion.starknet.account

import com.swmansion.starknet.data.types.*
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

    /**
     * Execute single call.
     *
     * Execute single call on starknet.
     *
     * @param call a call to be executed.
     * @param params additional execution parameters for the transaction.
     * @return Invoke function response, containing transaction hash.
     */
    fun execute(call: Call, params: CallParams): InvokeFunctionResponse {
        return execute(listOf(call), params)
    }

    /**
     * Execute a list of calls.
     *
     * Execute a list of calls on starknet.
     *
     * @param calls a list of calls to be executed.
     * @param params additional execution parameters for the transaction.
     * @return Invoke function response, containing transaction hash.
     */
    fun execute(calls: List<Call>, params: CallParams): InvokeFunctionResponse

    /**
     * Estimate fee for a call.
     *
     * Estimate fee for a call on starknet.
     *
     * @param call a call used to estimate a fee.
     * @param params additional execution parameters for the transaction.
     * @return Field value representing estimated fee.
     */
    fun estimateFee(call: Call, params: CallParams): Felt {
        return estimateFee(listOf(call), params)
    }

    /**
     * Estimate fee for a list of calls.
     *
     * Estimate fee for a list of calls on starknet.
     *
     * @param calls a list of calls used to estimate a fee.
     * @param params additional execution parameters for the transaction.
     * @return estimated fee as field value.
     */
    fun estimateFee(calls: List<Call>, params: CallParams): Felt

    /**
     * Get account nonce.
     *
     * Get current account nonce.
     *
     * @return nonce as field value.
     */
    fun getNonce(): Felt
}
