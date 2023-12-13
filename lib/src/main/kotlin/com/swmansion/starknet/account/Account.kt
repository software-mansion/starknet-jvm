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
     * Sign a version 1 invoke transaction.
     *
     * Sign a transaction to be executed on Starknet.
     *
     * @param call a call to be signed
     * @param params additional execution parameters for the transaction
     * @param forFeeEstimate when set to `true`, it changes the version to `2^128+version` so the signed transaction can only be used for fee estimation
     * @return signed invoke transaction version 1 payload
     */
    fun sign(call: Call, params: ExecutionParams, forFeeEstimate: Boolean): InvokeTransactionV1Payload {
        return sign(listOf(call), params, forFeeEstimate)
    }

    /**
     * Sign a version 3 invoke transaction.
     *
     * Sign a transaction to be executed on Starknet.
     *
     * @param call a call to be signed
     * @param params additional execution parameters for the transaction
     * @param forFeeEstimate when set to `true`, it changes the version to `2^128+version` so the signed transaction can only be used for fee estimation
     * @return signed invoke transaction version 3 payload
     */
    fun signV3(call: Call, params: ExecutionParamsV3, forFeeEstimate: Boolean): InvokeTransactionV3Payload {
        return signV3(listOf(call), params, forFeeEstimate)
    }

    /**
     * Sign a version 1 invoke transaction.
     *
     * Sign a transaction to be executed on Starknet.
     *
     * @param call a call to be signed
     * @param params additional execution parameters for the transaction
     * @return signed invoke transaction version 1 payload
     */
    fun sign(call: Call, params: ExecutionParams): InvokeTransactionV1Payload {
        return sign(listOf(call), params, false)
    }

    /**
     * Sign a version 3 invoke transaction.
     *
     * Sign a transaction to be executed on Starknet.
     *
     * @param call a call to be signed
     * @param params additional execution parameters for the transaction
     * @return signed invoke transaction version 3 payload
     */
    fun signV3(call: Call, params: ExecutionParamsV3): InvokeTransactionV3Payload {
        return signV3(listOf(call), params, false)
    }

    /**
     * Sign multiple calls as a single version 1 invoke transaction.
     *
     * Sign a list of calls to be executed on Starknet.
     *
     * @param calls a list of calls to be signed
     * @param params additional execution parameters for the transaction
     * @param forFeeEstimate when set to `true`, it changes the version to `2^128+version` so the signed transaction can only be used for fee estimation
     * @return signed invoke transaction version 1 payload
     */
    fun sign(calls: List<Call>, params: ExecutionParams, forFeeEstimate: Boolean): InvokeTransactionV1Payload

    /**
     * Sign multiple calls as a single version 3 invoke transaction.
     *
     * Sign a list of calls to be executed on Starknet.
     *
     * @param calls a list of calls to be signed
     * @param params additional execution parameters for the transaction
     * @param forFeeEstimate when set to `true`, it changes the version to `2^128+version` so the signed transaction can only be used for fee estimation
     * @return signed invoke transaction version 3 payload
     */
    fun signV3(calls: List<Call>, params: ExecutionParamsV3, forFeeEstimate: Boolean): InvokeTransactionV3Payload

    /**
     * Sign multiple calls as a single version 1 invoke transaction.
     *
     * Sign a list of calls to be executed on Starknet.
     *
     * @param calls a list of calls to be signed
     * @param params additional execution parameters for the transaction
     * @return signed invoke transaction version 1 payload
     */
    fun sign(calls: List<Call>, params: ExecutionParams): InvokeTransactionV1Payload {
        return sign(calls, params, false)
    }

    /**
     * Sign multiple calls as a single version 3 invoke transaction.
     *
     * Sign a list of calls to be executed on Starknet.
     *
     * @param calls a list of calls to be signed
     * @param params additional execution parameters for the transaction
     * @return signed invoke transaction version 3 payload
     */
    fun signV3(calls: List<Call>, params: ExecutionParamsV3): InvokeTransactionV3Payload {
        return signV3(calls, params, false)
    }

    /**
     * Sign version 1 deploy account transaction.
     *
     * Sign a deploy account transaction that requires prefunding deployed address.
     *
     * @param classHash hash of the contract that will be deployed. Has to be declared first!
     * @param calldata constructor calldata for the contract deployment
     * @param salt salt used to calculate address of the new contract
     * @param maxFee max fee to be consumed by this transaction
     * @param nonce nonce
     * @param forFeeEstimate when set to `true`, it changes the version to `2^128+version` so the signed transaction can only be used for fee estimation
     * @return signed deploy account payload
     */
    fun signDeployAccount(
        classHash: Felt,
        calldata: Calldata,
        salt: Felt,
        maxFee: Felt,
        nonce: Felt,
        forFeeEstimate: Boolean,
    ): DeployAccountTransactionV1Payload

    /**
     * Sign version 3 deploy account transaction.
     *
     * Sign a deploy account transaction that requires prefunding deployed address.
     *
     * @param classHash hash of the contract that will be deployed. Has to be declared first!
     * @param calldata constructor calldata for the contract deployment
     * @param salt salt used to calculate address of the new contract
     * @param params additional params for the transaction
     * @param forFeeEstimate when set to `true`, it changes the version to `2^128+version` so the signed transaction can only be used for fee estimation
     * @return signed deploy account payload
     */
    fun signDeployAccountV3(
        classHash: Felt,
        calldata: Calldata,
        salt: Felt,
        params: DeployAccountParamsV3,
        forFeeEstimate: Boolean,
    ): DeployAccountTransactionV3Payload

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
    ): DeployAccountTransactionV1Payload {
        return signDeployAccount(classHash, calldata, salt, maxFee, Felt.ZERO, false)
    }

    /**
     * Sign version 3 deploy account transaction.
     *
     * Sign a deploy account transaction that requires prefunding deployed address.
     *
     * @param classHash hash of the contract that will be deployed. Has to be declared first!
     * @param calldata constructor calldata for the contract deployment
     * @param salt salt used to calculate address of the new contract
     * @param forFeeEstimate when set to `true`, it changes the version to `2^128+version` so the signed transaction can only be used for fee estimation
     * @return signed deploy account payload
     */
    fun signDeployAccountV3(
        classHash: Felt,
        calldata: Calldata,
        salt: Felt,
        l1ResourceBounds: ResourceBounds,
        forFeeEstimate: Boolean,
    ): DeployAccountTransactionV3Payload {
        val params = DeployAccountParamsV3(
            nonce = Felt.ZERO,
            l1ResourceBounds = l1ResourceBounds,
        )
        return signDeployAccountV3(classHash, calldata, salt, params, forFeeEstimate)
    }

    /**
     * Sign a version 1 declare transaction.
     *
     * Prepare and sign a version 1 declare transaction to be executed on Starknet.
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
     * Prepare and sign a version 2 declare transaction to be executed on Starknet.
     *
     * @param sierraContractDefinition a cairo 1/2 sierra compiled definition of the contract to be declared
     * @param casmContractDefinition a casm representation of cairo 1/2 compiled contract to be declared
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
     * Sign a version 3 declare transaction.
     *
     * Prepare and sign a version 3 declare transaction to be executed on Starknet.
     *
     * @param sierraContractDefinition a cairo 1/2 sierra compiled definition of the contract to be declared
     * @param casmContractDefinition a casm representation of cairo 1/2 compiled contract to be declared
     * @param params additional parameters for the transaction
     * @param forFeeEstimate when set to `true`, it changes the version to `2^128+version` so the signed transaction can only be used for fee estimation
     * @return signed declare transaction payload
     */
    fun signDeclareV3(
        sierraContractDefinition: Cairo1ContractDefinition,
        casmContractDefinition: CasmContractDefinition,
        params: DeclareParamsV3,
        forFeeEstimate: Boolean = false,
    ): DeclareTransactionV3Payload

    /**
     * Sign TypedData for off-chain usage with this account privateKey
     *
     * @param typedData a TypedData instance to sign
     * @return a signature of typedData provided
     */
    fun signTypedData(typedData: TypedData): Signature

    /**
     * Verify a signature of TypedData on Starknet
     *
     * @param typedData a TypedData instance which signature will be verified
     * @param signature a signature of typedData
     * @return `true` if signature is valid, `false` otherwise
     */
    fun verifyTypedDataSignature(typedData: TypedData, signature: Signature): Request<Boolean>

    /**
     * Execute a list of calls using version 1 invoke transaction.
     *
     * Execute list of calls on Starknet.
     *
     * @param calls a list of calls to be executed.
     * @param maxFee a max fee to pay for the transaction.
     * @return Invoke function response, containing transaction hash.
     */
    fun execute(calls: List<Call>, maxFee: Felt): Request<InvokeFunctionResponse>

    /**
     * Execute a list of calls using version 3 invoke transaction.
     *
     * Execute list of calls on Starknet.
     *
     * @param calls a list of calls to be executed.
     * @param l1ResourceBounds L1 resource bounds for the transaction.
     * @return Invoke function response, containing transaction hash.
     */
    fun executeV3(calls: List<Call>, l1ResourceBounds: ResourceBounds): Request<InvokeFunctionResponse>

    /**
     * Execute single call using version 1 invoke transaction.
     *
     * Execute single call on starknet.
     *
     * @param call a call to be executed.
     * @param maxFee a max fee to pay for the transaction.
     * @return Invoke function response, containing transaction hash.
     */
    fun execute(call: Call, maxFee: Felt): Request<InvokeFunctionResponse>

    /**
     * Execute single call using version 3 invoke transaction.
     *
     * Execute single call on starknet.
     *
     * @param call a call to be executed.
     * @param l1ResourceBounds L1 resource bounds for the transaction.
     * @return Invoke function response, containing transaction hash.
     */
    fun executeV3(call: Call, l1ResourceBounds: ResourceBounds): Request<InvokeFunctionResponse>

    /**
     * Execute a list of calls with automatically estimated fee.
     *
     * @param calls a list of calls to be executed.
     * @return Invoke function response, containing transaction hash.
     */
    fun execute(calls: List<Call>): Request<InvokeFunctionResponse>

    /**
     * Execute a list of calls with automatically estimated fee.
     *
     * @param calls a list of calls to be executed.
     * @return Invoke function response, containing transaction hash.
     */
    fun executeV3(calls: List<Call>): Request<InvokeFunctionResponse>

    /**
     * Execute single call with automatically estimated fee using version 1 invoke transaction.
     *
     * @param call a call to be executed.
     * @return Invoke function response, containing transaction hash.
     */
    fun execute(call: Call): Request<InvokeFunctionResponse>

    /**
     * Execute single call with automatically estimated fee using version 3 invoke transaction.
     *
     * @param call a call to be executed.
     * @return Invoke function response, containing transaction hash.
     */
    fun executeV3(call: Call): Request<InvokeFunctionResponse>

    /**
     * Estimate fee for a call.
     *
     * Estimate fee for a signed call on starknet.
     *
     * @param call a call used to estimate a fee.
     * @return Field value representing estimated fee.
     */
    fun estimateFee(call: Call): Request<List<EstimateFeeResponse>>

    /**
     * Estimate fee for a call.
     *
     * Estimate fee for a signed call on starknet.
     *
     * @param call a call used to estimate a fee.
     * @param simulationFlags a set of simulation flags used to estimate a fee.
     * @return Field value representing estimated fee.
     */
    fun estimateFee(call: Call, simulationFlags: Set<SimulationFlagForEstimateFee>): Request<List<EstimateFeeResponse>>

    /**
     * Estimate fee for a call.
     *
     * Estimate fee for a signed call on starknet for specified block tag.
     *
     * @param call a call used to estimate a fee.
     * @param blockTag a tag of the block in respect to what the query will be made.
     * @return Field value representing estimated fee.
     */
    fun estimateFee(call: Call, blockTag: BlockTag): Request<List<EstimateFeeResponse>>

    /**
     * Estimate fee for a call.
     *
     * Estimate fee for a signed call on starknet for specified block tag.
     *
     * @param call a call used to estimate a fee.
     * @param blockTag a tag of the block in respect to what the query will be made.
     * @param simulationFlags a set of simulation flags used to estimate a fee.
     * @return Field value representing estimated fee.
     */
    fun estimateFee(call: Call, blockTag: BlockTag, simulationFlags: Set<SimulationFlagForEstimateFee>): Request<List<EstimateFeeResponse>>

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
     * Estimate fee for a list of calls using version 3 invoke transaction.
     *
     * Estimate fee for a signed list of calls on starknet.
     *
     * @param calls a list of calls used to estimate a fee.
     * @return estimated fee as field value.
     */
    fun estimateFeeV3(calls: List<Call>): Request<List<EstimateFeeResponse>>

    /**
     * Estimate fee for a list of calls.
     *
     * Estimate fee for a signed list of calls on starknet.
     *
     * @param calls a list of calls used to estimate a fee.
     * @param simulationFlags a set of simulation flags used to estimate a fee.
     * @return estimated fee as field value.
     */
    fun estimateFee(calls: List<Call>, simulationFlags: Set<SimulationFlagForEstimateFee>): Request<List<EstimateFeeResponse>>

    /**
     * Estimate fee for a list of calls.
     *
     * Estimate fee for a signed list of calls on starknet.
     *
     * @param calls a list of calls used to estimate a fee.
     * @param simulationFlags a set of simulation flags used to estimate a fee.
     * @return estimated fee as field value.
     */
    fun estimateFeeV3(calls: List<Call>, simulationFlags: Set<SimulationFlagForEstimateFee>): Request<List<EstimateFeeResponse>>

    /**
     * Estimate fee for a list of calls.
     *
     * Estimate fee for a signed list of calls on starknet.
     *
     * @param calls a list of calls used to estimate a fee.
     * @param blockTag a tag of the block in respect to what the query will be made.
     * @return estimated fee as field value.
     */
    fun estimateFee(calls: List<Call>, blockTag: BlockTag): Request<List<EstimateFeeResponse>>

    /**
     * Estimate fee for a list of calls.
     *
     * Estimate fee for a signed list of calls on starknet.
     *
     * @param calls a list of calls used to estimate a fee.
     * @param blockTag a tag of the block in respect to what the query will be made.
     * @param simulationFlags a set of simulation flags used to estimate a fee.
     * @return estimated fee as field value.
     */
    fun estimateFeeV3(
        calls: List<Call>,
        blockTag: BlockTag,
        simulationFlags: Set<SimulationFlagForEstimateFee>,
    ): Request<List<EstimateFeeResponse>>

    /**
     * Estimate fee for a list of calls using version 3 invoke transaction.
     *
     * Estimate fee for a signed list of calls on starknet.
     *
     * @param calls a list of calls used to estimate a fee.
     * @param blockTag a tag of the block in respect to what the query will be made.
     * @param simulationFlags a set of simulation flags used to estimate a fee.
     * @return estimated fee as field value.
     */
    fun estimateFee(
        calls: List<Call>,
        blockTag: BlockTag,
        simulationFlags: Set<SimulationFlagForEstimateFee>,
    ): Request<List<EstimateFeeResponse>>

    /**
     * Get account nonce.
     *
     * Get account nonce for pending block.
     *
     * @return nonce as field value.
     */
    fun getNonce(): Request<Felt>

    /**
     * Get account nonce.
     *
     * Get account nonce for specified block tag.
     *
     * @param blockTag block tag used for returning this value.
     * @return nonce as field value.
     */
    fun getNonce(blockTag: BlockTag): Request<Felt>

    /**
     * Get account nonce.
     *
     * Get account nonce for specified block hash.
     *
     * @param blockHash block hash used for returning this value.
     * @return nonce as field value.
     */
    fun getNonce(blockHash: Felt): Request<Felt>

    /**
     * Get account nonce.
     *
     * Get account nonce for specified block number.
     *
     * @param blockNumber block number used for returning this value.
     * @return nonce as field value.
     */
    fun getNonce(blockNumber: Int): Request<Felt>
}
