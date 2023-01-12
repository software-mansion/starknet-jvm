package com.swmansion.starknet.account

import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.data.types.transactions.*
import com.swmansion.starknet.provider.Request

/**
 * An account interface.
 *
 * Implementers of this interface provide methods for signing transactions.
 */
interface Account {
    val address: Felt

    /**
     * Sign a transaction.
     *
     * Sign a transaction to be executed on StarkNet.
     *
     * @param call a call to be signed
     * @param params additional execution parameters for the transaction
     * @return signed invoke function payload
     */
    fun sign(call: Call, params: ExecutionParams): InvokeTransactionPayload {
        return sign(listOf(call), params)
    }

    /**
     * Sign multiple calls as a single transaction.
     *
     * Sign a list of calls to be executed on StarkNet.
     *
     * @param calls a list of calls to be signed
     * @param params additional execution parameters for the transaction
     * @return signed invoke function payload
     */
    fun sign(calls: List<Call>, params: ExecutionParams): InvokeTransactionPayload

    /**
     * Sign deploy account transaction.
     *
     * Sign a deploy account transaction that requires prefunding deployed address.
     *
     * @param classHash hash of the contract that will be deployed. Has to be declared first!
     * @param calldata constructor calldata for the contract deployment
     * @param salt salt used to calculate address of the new contract
     * @return signed deploy account payload
     */
    fun signDeployAccount(
        classHash: Felt,
        calldata: Calldata,
        salt: Felt,
        maxFee: Felt,
    ): DeployAccountTransactionPayload

    /**
     * Sign a declare transaction.
     *
     * Prepare and sign a declare transaction to be executed on StarkNet.
     *
     * @param contractDefinition a definition of the contract to be declared
     * @param classHash a class hash of the contract to be declared
     * @param params additional execution parameters for the transaction
     * @return signed declare transaction payload
     */
    fun signDeclare(
        contractDefinition: ContractDefinition,
        classHash: Felt,
        params: ExecutionParams,
    ): DeclareTransactionPayload

    /**
     * Execute a list of calls
     *
     * Execute list of calls on starknet.
     *
     * @param calls a list of calls to be executed.
     * @param maxFee a max fee to pay for the transaction.
     * @return Invoke function response, containing transaction hash.
     */
    fun execute(calls: List<Call>, maxFee: Felt): Request<InvokeFunctionResponse>

    /**
     * Execute single call.
     *
     * Execute single call on starknet.
     *
     * @param call a call to be executed.
     * @param maxFee a max fee to pay for the transaction.
     * @return Invoke function response, containing transaction hash.
     */
    fun execute(call: Call, maxFee: Felt): Request<InvokeFunctionResponse> {
        return execute(listOf(call), maxFee)
    }

    /**
     * Execute a list of calls with automatically estimated fee.
     *
     * @param calls a list of calls to be executed.
     * @return Invoke function response, containing transaction hash.
     */
    fun execute(calls: List<Call>): Request<InvokeFunctionResponse>

    /**
     * Execute single call with automatically estimated fee
     *
     * @param call a call to be executed.
     * @return Invoke function response, containing transaction hash.
     */
    fun execute(call: Call): Request<InvokeFunctionResponse> {
        return execute(listOf(call))
    }

    /**
     * Estimate fee for a call.
     *
     * Estimate fee for a signed call on starknet.
     *
     * @param call a call used to estimate a fee.
     * @return Field value representing estimated fee.
     */
    fun estimateFee(call: Call): Request<EstimateFeeResponse> {
        return estimateFee(listOf(call))
    }

    /**
     * Estimate fee for a list of calls.
     *
     * Estimate fee for a signed list of calls on starknet.
     *
     * @param calls a list of calls used to estimate a fee.
     * @return estimated fee as field value.
     */
    fun estimateFee(calls: List<Call>): Request<EstimateFeeResponse>

    /**
     * Get account nonce.
     *
     * Get account nonce for latest state.
     *
     * @return nonce as field value.
     */
    fun getNonce(): Request<Felt>
}
