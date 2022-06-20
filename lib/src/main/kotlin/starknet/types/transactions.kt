@file:JvmName("Transactions")

package starknet.types

import starknet.crypto.StarknetSignature
import types.Felt

typealias Calldata = List<Felt>

enum class TransactionStatus {
    NOT_RECEIVED,
    RECEIVED,
    PENDING,
    ACCEPTED_ON_L1,
    ACCEPTED_ON_L2,
    REJECTED
}

enum class TransactionType {
    DECLARE,
    DEPLOY,
    INVOKE_FUNCTION
}

data class Invocation(
    val contractAddress: Felt,
    val entrypoint: Felt,
    val calldata: Calldata?,
    val signature: StarknetSignature?
)

data class InvocationDetails(
    val nonce: Felt?,
    val maxFee: Felt?,
    val version: Felt?
)

sealed class Transaction

data class DeclareTransaction(
    val nonce: Felt,
    val contractClass: CompiledContract,
    val signerAddress: Felt,
    val signature: StarknetSignature
): Transaction()

data class DeployTransaction(
    val contractDefinition: CompiledContract,
    val contractAddressSalt: Felt,
    val constructorCalldata: Calldata,
    val nonce: Felt?
): Transaction()

data class InvokeFunctionTransaction(
    val contractAddress: Felt,
    val signature: StarknetSignature?,
    val entrypointSelector: Felt,
    val calldata: Calldata,
    val nonce: Felt?,
    val maxFee: Felt?,
    val version: Felt?
)






