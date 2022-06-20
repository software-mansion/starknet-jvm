@file:JvmName("Transactions")

package starknet.types

import starknet.crypto.StarknetCurveSignature
import types.Felt

typealias Calldata = List<Felt>
typealias Signature = List<Felt>

enum class TransactionStatus {
    NOT_RECEIVED, RECEIVED, PENDING, ACCEPTED_ON_L1, ACCEPTED_ON_L2, REJECTED
}

enum class TransactionType {
    DECLARE, DEPLOY, INVOKE_FUNCTION
}

data class Invocation(
    val contractAddress: Felt, val entrypoint: Felt, val calldata: Calldata?, val signature: Signature?
)

data class InvocationDetails(
    val nonce: Felt?, val maxFee: Felt?, val version: Felt?
)

sealed class Transaction

data class DeclareTransaction(
    val nonce: Felt, val contractClass: CompiledContract, val signerAddress: Felt, val signature: StarknetCurveSignature
) : Transaction()

data class DeployTransaction(
    val contractDefinition: CompiledContract,
    val contractAddressSalt: Felt,
    val constructorCalldata: Calldata,
    val nonce: Felt?
) : Transaction()

data class InvokeFunctionTransaction(
    val contractAddress: Felt,
    val signature: Signature?,
    val entrypointSelector: Felt,
    val calldata: Calldata,
    val nonce: Felt?,
    val maxFee: Felt?,
    val version: Felt?
)






