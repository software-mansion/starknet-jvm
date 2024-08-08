package com.swmansion.starknet.account

import com.swmansion.starknet.data.TypedData
import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.provider.Request

/**
 * An account interface.
 *
 * Implementers of this interface provide methods for signing transactions.
 */
interface Account {
    val address: Felt
    val chainId: StarknetChainId

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
    fun signV1(call: Call, params: ExecutionParams, forFeeEstimate: Boolean): InvokeTransactionV1Payload {
        return signV1(listOf(call), params, forFeeEstimate)
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
    fun signV3(call: Call, params: InvokeParamsV3, forFeeEstimate: Boolean): InvokeTransactionV3Payload {
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
    fun signV1(call: Call, params: ExecutionParams): InvokeTransactionV1Payload {
        return signV1(listOf(call), params, false)
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
    fun signV3(call: Call, params: InvokeParamsV3): InvokeTransactionV3Payload {
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
    fun signV1(calls: List<Call>, params: ExecutionParams, forFeeEstimate: Boolean): InvokeTransactionV1Payload

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
    fun signV3(calls: List<Call>, params: InvokeParamsV3, forFeeEstimate: Boolean): InvokeTransactionV3Payload

    /**
     * Sign multiple calls as a single version 1 invoke transaction.
     *
     * Sign a list of calls to be executed on Starknet.
     *
     * @param calls a list of calls to be signed
     * @param params additional execution parameters for the transaction
     * @return signed invoke transaction version 1 payload
     */
    fun signV1(calls: List<Call>, params: ExecutionParams): InvokeTransactionV1Payload {
        return signV1(calls, params, false)
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
    fun signV3(calls: List<Call>, params: InvokeParamsV3): InvokeTransactionV3Payload {
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
    fun signDeployAccountV1(
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
     * Sign a deploy account transaction that requires prefunding said account.
     *
     * @param classHash hash of the contract that will be deployed
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
     * Sign version 1 deploy account transaction.
     *
     * Sign a deploy account transaction that requires prefunding deployed address.
     *
     * @param classHash hash of the contract that will be deployed. Has to be declared first!
     * @param calldata constructor calldata for the contract deployment
     * @param salt salt used to calculate address of the new contract
     * @param maxFee max fee to be consumed by this transaction
     * @return signed deploy account payload
     */
    fun signDeployAccountV1(
        classHash: Felt,
        calldata: Calldata,
        salt: Felt,
        maxFee: Felt,
    ): DeployAccountTransactionV1Payload {
        return signDeployAccountV1(classHash, calldata, salt, maxFee, Felt.ZERO, false)
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
     * Sign version 3 deploy account transaction.
     *
     * Sign a deploy account transaction that requires prefunding deployed address.
     *
     * @param classHash hash of the contract that will be deployed. Has to be declared first!
     * @param calldata constructor calldata for the contract deployment
     * @param salt salt used to calculate address of the new contract
     * @return signed deploy account payload
     */
    fun signDeployAccountV3(
        classHash: Felt,
        calldata: Calldata,
        salt: Felt,
        l1ResourceBounds: ResourceBounds,
    ): DeployAccountTransactionV3Payload {
        val params = DeployAccountParamsV3(
            nonce = Felt.ZERO,
            l1ResourceBounds = l1ResourceBounds,
        )
        return signDeployAccountV3(classHash, calldata, salt, params, false)
    }

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
    fun signDeclareV2(
        sierraContractDefinition: Cairo1ContractDefinition,
        casmContractDefinition: CasmContractDefinition,
        params: ExecutionParams,
        forFeeEstimate: Boolean,
    ): DeclareTransactionV2Payload

    /**
     * Sign a version 2 declare transaction.
     *
     * Prepare and sign a version 2 declare transaction to be executed on Starknet.
     *
     * @param sierraContractDefinition a cairo 1/2 sierra compiled definition of the contract to be declared
     * @param casmContractDefinition a casm representation of cairo 1/2 compiled contract to be declared
     * @param params additional execution parameters for the transaction
     * @return signed declare transaction payload
     */
    fun signDeclareV2(
        sierraContractDefinition: Cairo1ContractDefinition,
        casmContractDefinition: CasmContractDefinition,
        params: ExecutionParams,
    ): DeclareTransactionV2Payload {
        return signDeclareV2(sierraContractDefinition, casmContractDefinition, params, false)
    }

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
        forFeeEstimate: Boolean,
    ): DeclareTransactionV3Payload

    /**
     * Sign a version 3 declare transaction.
     *
     * Prepare and sign a version 3 declare transaction to be executed on Starknet.
     *
     * @param sierraContractDefinition a cairo 1/2 sierra compiled definition of the contract to be declared
     * @param casmContractDefinition a casm representation of cairo 1/2 compiled contract to be declared
     * @param params additional parameters for the transaction
     * @return signed declare transaction payload
     */
    fun signDeclareV3(
        sierraContractDefinition: Cairo1ContractDefinition,
        casmContractDefinition: CasmContractDefinition,
        params: DeclareParamsV3,
    ): DeclareTransactionV3Payload {
        return signDeclareV3(sierraContractDefinition, casmContractDefinition, params, false)
    }

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
    fun executeV1(calls: List<Call>, maxFee: Felt): Request<InvokeFunctionResponse>

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
     * Execute single call on Starknet.
     *
     * @param call a call to be executed.
     * @param maxFee a max fee to pay for the transaction.
     * @return Invoke function response, containing transaction hash.
     */
    fun executeV1(call: Call, maxFee: Felt): Request<InvokeFunctionResponse>

    /**
     * Execute single call using version 3 invoke transaction.
     *
     * Execute single call on Starknet.
     *
     * @param call a call to be executed.
     * @param l1ResourceBounds L1 resource bounds for the transaction.
     * @return Invoke function response, containing transaction hash.
     */
    fun executeV3(call: Call, l1ResourceBounds: ResourceBounds): Request<InvokeFunctionResponse>

    /**
     * Execute a list of calls using version 1 invoke transaction with automatically estimated fee
     * that will be multiplied by the specified multiplier when max fee is calculated.
     *
     * @see [EstimateFeeResponse.toMaxFee] for algorithm used to calculate max fee.
     *
     * @param calls a list of calls to be executed.
     * @param estimateFeeMultiplier how big multiplier should be used for the estimated fee.
     *
     * @return Invoke function response, containing transaction hash.
     */
    fun executeV1(calls: List<Call>, estimateFeeMultiplier: Double): Request<InvokeFunctionResponse>

    /**
     * Execute a list of calls using version 3 invoke transaction with automatically estimated fee
     * that will be multiplied by the specified multipliers when resource bounds are calculated.
     *
     * @see [EstimateFeeResponse.toResourceBounds] for algorithm used to calculate resource bounds.
     *
     * @param calls a list of calls to be executed.
     * @param estimateAmountMultiplier how big multiplier should be used for the estimated amount.
     * @param estimateUnitPriceMultiplier how big multiplier should be used for the estimated unit price.
     *
     * @return Invoke function response, containing transaction hash.
     */
    fun executeV3(calls: List<Call>, estimateAmountMultiplier: Double, estimateUnitPriceMultiplier: Double): Request<InvokeFunctionResponse>

    /**
     * Execute single call using version 1 invoke transaction with automatically estimated fee
     * that will be multiplied by the specified multiplier when max fee is calculated.
     *
     * @see [EstimateFeeResponse.toMaxFee] for algorithm used to calculate max fee.
     *
     * @param call a call to be executed.
     * @param estimateFeeMultiplier how big multiplier should be used for the estimated fee.
     *
     * @return Invoke function response, containing transaction hash.
     */
    fun executeV1(call: Call, estimateFeeMultiplier: Double): Request<InvokeFunctionResponse>

    /**
     * Execute single call using version 3 invoke transaction with automatically estimated fee
     * that will be multiplied by the specified multipliers when resource bounds are calculated.
     *
     * @see [EstimateFeeResponse.toResourceBounds] for algorithm used to calculate resource bounds.
     *
     * @param call a call to be executed.
     * @param estimateAmountMultiplier how big multiplier should be used for the estimated amount.
     * @param estimateUnitPriceMultiplier how big multiplier should be used for the estimated unit price.
     *
     * @return Invoke function response, containing transaction hash.
     */
    fun executeV3(call: Call, estimateAmountMultiplier: Double, estimateUnitPriceMultiplier: Double): Request<InvokeFunctionResponse>

    /**
     * Execute a list of calls with automatically estimated fee using version 1 invoke transaction.
     *
     * @param calls a list of calls to be executed.
     * @return Invoke function response, containing transaction hash.
     */
    fun executeV1(calls: List<Call>): Request<InvokeFunctionResponse>

    /**
     * Execute a list of calls with automatically estimated fee using version 3 invoke transaction.
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
    fun executeV1(call: Call): Request<InvokeFunctionResponse>

    /**
     * Execute single call with automatically estimated fee using version 3 invoke transaction.
     *
     * @param call a call to be executed.
     * @return Invoke function response, containing transaction hash.
     */
    fun executeV3(call: Call): Request<InvokeFunctionResponse>

    /**
     * Estimate fee for a call as a version 1 invoke transaction.
     *
     * Estimate fee for a signed call on Starknet.
     *
     * @param call a call used to estimate a fee.
     * @return Field value representing estimated fee.
     */
    fun estimateFeeV1(call: Call): Request<EstimateFeeResponseList>

    /**
     * Estimate fee for a call as a version 3 invoke transaction.
     *
     * Estimate fee for a signed call on Starknet.
     *
     * @param call a call used to estimate a fee.
     * @return Field value representing estimated fee.
     */
    fun estimateFeeV3(call: Call): Request<EstimateFeeResponseList>

    /**
     * Estimate fee for a call as a version 1 invoke transaction.
     *
     * Estimate fee for a signed call on Starknet.
     *
     * @param call a call used to estimate a fee.
     * @param skipValidate when set to `true`, the validation part of the transaction is skipped.
     * @return Field value representing estimated fee.
     */
    fun estimateFeeV1(call: Call, skipValidate: Boolean): Request<EstimateFeeResponseList>

    /**
     * Estimate fee for a call as a version 3 invoke transaction.
     *
     * Estimate fee for a signed call on Starknet.
     *
     * @param call a call used to estimate a fee.
     * @param skipValidate when set to `true`, the validation part of the transaction is skipped.
     * @return Field value representing estimated fee.
     */
    fun estimateFeeV3(call: Call, skipValidate: Boolean): Request<EstimateFeeResponseList>

    /**
     * Estimate fee for a call as a version 1 invoke transaction.
     *
     * Estimate fee for a signed call on Starknet for specified block tag.
     *
     * @param call a call used to estimate a fee.
     * @param blockTag a tag of the block in respect to what the query will be made.
     * @return Field value representing estimated fee.
     */
    fun estimateFeeV1(call: Call, blockTag: BlockTag): Request<EstimateFeeResponseList>

    /**
     * Estimate fee for a call as a version 3 invoke transaction.
     *
     * Estimate fee for a signed call on Starknet for specified block tag.
     *
     * @param call a call used to estimate a fee.
     * @param blockTag a tag of the block in respect to what the query will be made.
     * @return Field value representing estimated fee.
     */
    fun estimateFeeV3(call: Call, blockTag: BlockTag): Request<EstimateFeeResponseList>

    /**
     * Estimate fee for a call as a version 1 invoke transaction.
     *
     * Estimate fee for a signed call on Starknet for specified block tag.
     *
     * @param call a call used to estimate a fee.
     * @param blockTag a tag of the block in respect to what the query will be made.
     * @param skipValidate when set to `true`, the validation part of the transaction is skipped.
     * @return Field value representing estimated fee.
     */
    fun estimateFeeV1(call: Call, blockTag: BlockTag, skipValidate: Boolean): Request<EstimateFeeResponseList>

    /**
     * Estimate fee for a call as a version 3 invoke transaction.
     *
     * Estimate fee for a signed call on Starknet for specified block tag.
     *
     * @param call a call used to estimate a fee.
     * @param blockTag a tag of the block in respect to what the query will be made.
     * @param skipValidate when set to `true`, the validation part of the transaction is skipped.
     * @return Field value representing estimated fee.
     */
    fun estimateFeeV3(call: Call, blockTag: BlockTag, skipValidate: Boolean): Request<EstimateFeeResponseList>

    /**
     * Estimate fee for a list of calls as a version 1 invoke transaction.
     *
     * Estimate fee for a signed list of calls on Starknet.
     *
     * @param calls a list of calls used to estimate a fee.
     * @return estimated fee as field value.
     */
    fun estimateFeeV1(calls: List<Call>): Request<EstimateFeeResponseList>

    /**
     * Estimate fee for a list of calls as a version 3 invoke transaction.
     *
     * Estimate fee for a signed list of calls on Starknet.
     *
     * @param calls a list of calls used to estimate a fee.
     * @return estimated fee as field value.
     */
    fun estimateFeeV3(calls: List<Call>): Request<EstimateFeeResponseList>

    /**
     * Estimate fee for a list of calls as a version 1 invoke transaction.
     *
     * Estimate fee for a signed list of calls on Starknet.
     *
     * @param calls a list of calls used to estimate a fee.
     * @param skipValidate when set to `true`, the validation part of the transaction is skipped.
     * @return estimated fee as field value.
     */
    fun estimateFeeV1(calls: List<Call>, skipValidate: Boolean): Request<EstimateFeeResponseList>

    /**
     * Estimate fee for a list of calls as a version 3 invoke transaction.
     *
     * Estimate fee for a signed list of calls on Starknet.
     *
     * @param calls a list of calls used to estimate a fee.
     * @param skipValidate when set to `true`, the validation part of the transaction is skipped.
     * @return estimated fee as field value.
     */
    fun estimateFeeV3(calls: List<Call>, skipValidate: Boolean): Request<EstimateFeeResponseList>

    /**
     * Estimate fee for a list of calls as a version 1 invoke transaction.
     *
     * Estimate fee for a signed list of calls on Starknet.
     *
     * @param calls a list of calls used to estimate a fee.
     * @param blockTag a tag of the block in respect to what the query will be made.
     * @return estimated fee as field value.
     */
    fun estimateFeeV1(calls: List<Call>, blockTag: BlockTag): Request<EstimateFeeResponseList>

    /**
     * Estimate fee for a list of calls.
     *
     * Estimate fee for a signed list of calls on Starknet.
     *
     * @param calls a list of calls used to estimate a fee.
     * @param blockTag a tag of the block in respect to what the query will be made.
     * @return estimated fee as field value.
     */
    fun estimateFeeV3(calls: List<Call>, blockTag: BlockTag): Request<EstimateFeeResponseList>

    /**
     * Estimate fee for a list of calls using version 3 invoke transaction.
     *
     * Estimate fee for a signed list of calls on Starknet.
     *
     * @param calls a list of calls used to estimate a fee.
     * @param blockTag a tag of the block in respect to what the query will be made.
     * @param skipValidate when set to `true`, the validation part of the transaction is skipped.
     * @return estimated fee as field value.
     */
    fun estimateFeeV1(
        calls: List<Call>,
        blockTag: BlockTag,
        skipValidate: Boolean,
    ): Request<EstimateFeeResponseList>

    /**
     * Estimate fee for a list of calls.
     *
     * Estimate fee for a signed list of calls on Starknet.
     *
     * @param calls a list of calls used to estimate a fee.
     * @param blockTag a tag of the block in respect to what the query will be made.
     * @param skipValidate when set to `true`, the validation part of the transaction is skipped.
     * @return estimated fee as field value.
     */
    fun estimateFeeV3(
        calls: List<Call>,
        blockTag: BlockTag,
        skipValidate: Boolean,
    ): Request<EstimateFeeResponseList>

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
