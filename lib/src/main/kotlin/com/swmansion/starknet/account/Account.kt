package com.swmansion.starknet.account

import com.swmansion.starknet.data.TypedData
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
     * @param forFeeEstimate when set to `true`, it changes the version to `2^128+version` so the signed transaction can only be used for fee estimation
     * @return signed invoke function payload
     */
    fun sign(call: Call, params: ExecutionParams, forFeeEstimate: Boolean): InvokeTransactionPayload {
        return sign(listOf(call), params, forFeeEstimate)
    }

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
        return sign(listOf(call), params, false)
    }

    /**
     * Sign multiple calls as a single transaction.
     *
     * Sign a list of calls to be executed on StarkNet.
     *
     * @param calls a list of calls to be signed
     * @param params additional execution parameters for the transaction
     * @param forFeeEstimate when set to `true`, it changes the version to `2^128+version` so the signed transaction can only be used for fee estimation
     * @return signed invoke function payload
     */
    fun sign(calls: List<Call>, params: ExecutionParams, forFeeEstimate: Boolean): InvokeTransactionPayload

    /**
     * Sign multiple calls as a single transaction.
     *
     * Sign a list of calls to be executed on StarkNet.
     *
     * @param calls a list of calls to be signed
     * @param params additional execution parameters for the transaction
     * @return signed invoke function payload
     */
    fun sign(calls: List<Call>, params: ExecutionParams): InvokeTransactionPayload {
        return sign(calls, params, false)
    }

    /**
     * Sign deploy account transaction.
     *
     * Sign a deploy account transaction that requires prefunding deployed address.
     *
     * @param classHash hash of the contract that will be deployed. Has to be declared first!
     * @param calldata constructor calldata for the contract deployment
     * @param salt salt used to calculate address of the new contract
     * @param maxFee max fee to be consumed by this transaction
     * @param forFeeEstimate when set to `true`, it changes the version to `2^128+version` so the signed transaction can only be used for fee estimation
     * @return signed deploy account payload
     */
    fun signDeployAccount(
        classHash: Felt,
        calldata: Calldata,
        salt: Felt,
        maxFee: Felt,
        forFeeEstimate: Boolean,
    ): DeployAccountTransactionPayload

    /**
     * Sign deploy account transaction.
     *
     * Sign a deploy account transaction that requires prefunding deployed address.
     *
     * @param classHash hash of the contract that will be deployed. Has to be declared first!
     * @param calldata constructor calldata for the contract deployment
     * @param salt salt used to calculate address of the new contract
     * @param maxFee max fee to be consumed by this transaction
     * @return signed deploy account payload
     */
    fun signDeployAccount(
        classHash: Felt,
        calldata: Calldata,
        salt: Felt,
        maxFee: Felt,
    ): DeployAccountTransactionPayload {
        return signDeployAccount(classHash, calldata, salt, maxFee, false)
    }

    /**
     * Sign a version 1 declare transaction.
     *
     * Prepare and sign a version 1 declare transaction to be executed on StarkNet.
     *
     * @param contractDefinition a definition of the contract to be declared
     * @param classHash a class hash of the contract to be declared
     * @param params additional execution parameters for the transaction
     * @param forFeeEstimate when set to `true`, it changes the version to `2^128+version` so the signed transaction can only be used for fee estimation
     * @return signed declare transaction payload
     */
    fun signDeclare(
        contractDefinition: Cairo0ContractDefinition,
        classHash: Felt,
        params: ExecutionParams,
        forFeeEstimate: Boolean = false,
    ): DeclareTransactionV1Payload

    /**
     * Sign a version 2 declare transaction.
     *
     * Prepare and sign a version 2 declare transaction to be executed on StarkNet.
     *
     * @param sierraContractDefinition a cairo 1 sierra compiled definition of the contract to be declared
     * @param casmContractDefinition a casm representation of cairo 1 compiled contract to be declared
     * @param params additional execution parameters for the transaction
     * @param forFeeEstimate when set to `true`, it changes the version to `2^128+version` so the signed transaction can only be used for fee estimation
     * @return signed declare transaction payload
     */
    fun signDeclare(
        sierraContractDefinition: Cairo1ContractDefinition,
        casmContractDefinition: CasmContractDefinition,
        params: ExecutionParams,
        forFeeEstimate: Boolean = false,
    ): DeclareTransactionV2Payload

    /**
     * Sign TypedData for off-chain usage with this account privateKey
     *
     * @param typedData a TypedData instance to sign
     * @return a signature of typedData provided
     */
    fun signTypedData(typedData: TypedData): Signature

    /**
     * Verify a signature of TypedData on StarkNet
     *
     * @param typedData a TypedData instance which signature will be verified
     * @param signature a signature of typedData
     * @return `true` if signature is valid, `false` otherwise
     */
    fun verifyTypedDataSignature(typedData: TypedData, signature: Signature): Request<Boolean>

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
    fun estimateFee(call: Call): Request<List<EstimateFeeResponse>> {
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
    fun estimateFee(calls: List<Call>): Request<List<EstimateFeeResponse>>

    /**
     * Get account nonce.
     *
     * Get account nonce for latest state.
     *
     * @return nonce as field value.
     */
    fun getNonce(): Request<Felt>
}
