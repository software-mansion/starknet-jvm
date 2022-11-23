package com.swmansion.starknet.data.types.transactions

import com.swmansion.starknet.data.TransactionHashCalculator
import com.swmansion.starknet.data.types.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@JvmSynthetic
internal val INVOKE_VERSION = Felt.ONE

@OptIn(ExperimentalSerializationApi::class)
@Serializable
enum class TransactionType(val txPrefix: Felt) {
    @SerialName("DECLARE")
    DECLARE(Felt.fromHex("0x6465636c617265")), // encodeShortString('declare'),

    @SerialName("DEPLOY")
    DEPLOY(Felt.fromHex("0x6465706c6f79")), // encodeShortString('deploy'),

    @SerialName("DEPLOY_ACCOUNT")
    DEPLOY_ACCOUNT(Felt.fromHex("0x6465706c6f795f6163636f756e74")), // encodeShortString('deploy_account'),

    @SerialName("INVOKE")
    @JsonNames("INVOKE_FUNCTION")
    INVOKE(Felt.fromHex("0x696e766f6b65")), // encodeShortString('invoke'),

    @SerialName("L1_HANDLER")
    L1_HANDLER(Felt.fromHex("0x6c315f68616e646c6572")) // encodeShortString('l1_handler')
}

@Serializable
sealed class Transaction {
    abstract val hash: Felt
    abstract val maxFee: Felt
    abstract val version: Felt
    abstract val signature: Signature
    abstract val nonce: Felt
    abstract val type: TransactionType
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@SerialName("DEPLOY")
// OptIn needed because @JsonNames is part of the experimental serialization api
data class DeployTransaction(
    @SerialName("contract_address")
    val contractAddress: Felt,

    @SerialName("contract_address_salt")
    val contractAddressSalt: Felt,

    @SerialName("constructor_calldata")
    @JsonNames("calldata")
    val constructorCalldata: Calldata,

    @SerialName("class_hash")
    val classHash: Felt,

    @SerialName("transaction_hash")
    @JsonNames("txn_hash")
    override val hash: Felt,

    @SerialName("max_fee")
    override val maxFee: Felt = Felt.ZERO,

    @SerialName("version")
    override val version: Felt = Felt.ZERO,

    @SerialName("signature")
    override val signature: Signature = emptyList(),

    @SerialName("nonce")
    override val nonce: Felt = Felt.ZERO,

    override val type: TransactionType = TransactionType.DEPLOY,
) : Transaction()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@SerialName("INVOKE_FUNCTION")
data class InvokeTransaction(
    @SerialName("contract_address")
    val contractAddress: Felt,

    @SerialName("calldata")
    val calldata: Calldata,

    @SerialName("entry_point_selector")
    val entryPointSelector: Felt = Felt.ZERO,

    @SerialName("transaction_hash")
    @JsonNames("txn_hash")
    override val hash: Felt,

    @SerialName("max_fee")
    override val maxFee: Felt,

    @SerialName("version")
    override val version: Felt,

    @SerialName("signature")
    override val signature: Signature,

    @SerialName("nonce")
    override val nonce: Felt,

    override val type: TransactionType = TransactionType.INVOKE,
) : Transaction() {
    fun toPayload(): InvokeFunctionPayload {
        val invocation = Call(
            contractAddress = contractAddress,
            calldata = calldata,
            entrypoint = entryPointSelector,
        )

        return InvokeFunctionPayload(
            invocation = invocation,
            signature = signature,
            maxFee = maxFee,
            nonce = nonce,
        )
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@SerialName("DECLARE")
data class DeclareTransaction(
    @SerialName("class_hash")
    val classHash: Felt,

    @SerialName("sender_address")
    val senderAddress: Felt,

    @SerialName("transaction_hash")
    @JsonNames("txn_hash")
    override val hash: Felt,

    @SerialName("max_fee")
    override val maxFee: Felt,

    @SerialName("version")
    override val version: Felt,

    @SerialName("signature")
    override val signature: Signature,

    @SerialName("nonce")
    override val nonce: Felt,

    override val type: TransactionType = TransactionType.DECLARE,

    private val contractDefinition: ContractDefinition? = null,
) : Transaction() {
    @Throws(ConvertingToPayloadFailedException::class)
    internal fun toPayload(): DeclareTransactionPayload {
        contractDefinition ?: throw ConvertingToPayloadFailedException()
        return DeclareTransactionPayload(
            contractDefinition = contractDefinition,
            senderAddress = senderAddress,
            maxFee = maxFee,
            nonce = nonce,
            signature = signature,
            version = version,
        )
    }

    internal class ConvertingToPayloadFailedException : Exception()
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@SerialName("L1_HANDLER")
data class L1HandlerTransaction(
    @SerialName("contract_address")
    val contractAddress: Felt,

    @SerialName("calldata")
    val calldata: Calldata,

    @SerialName("entry_point_selector")
    val entryPointSelector: Felt,

    @SerialName("transaction_hash")
    @JsonNames("txn_hash")
    override val hash: Felt,

    @SerialName("max_fee")
    override val maxFee: Felt = Felt.ZERO,

    @SerialName("version")
    override val version: Felt,

    @SerialName("signature")
    override val signature: Signature = emptyList(),

    @SerialName("nonce")
    override val nonce: Felt,

    override val type: TransactionType = TransactionType.L1_HANDLER,
) : Transaction()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@SerialName("DEPLOY_ACCOUNT")
data class DeployAccountTransaction(
    @SerialName("class_hash")
    @JsonNames("contract_class")
    val classHash: Felt,

    @SerialName("contract_address_salt")
    val contractAddressSalt: Felt,

    @SerialName("constructor_calldata")
    @JsonNames("calldata")
    val constructorCalldata: Calldata,

    @SerialName("transaction_hash")
    @JsonNames("txn_hash")
    override val hash: Felt,

    @SerialName("max_fee")
    override val maxFee: Felt,

    @SerialName("version")
    override val version: Felt,

    @SerialName("signature")
    override val signature: Signature,

    @SerialName("nonce")
    override val nonce: Felt,

    override val type: TransactionType = TransactionType.DEPLOY_ACCOUNT,
) : Transaction() {
    internal fun toPayload(): DeployAccountTransactionPayload {
        return DeployAccountTransactionPayload(
            classHash = classHash,
            salt = contractAddressSalt,
            constructorCalldata = constructorCalldata,
            version = version,
            nonce = nonce,
            maxFee = maxFee,
            signature = signature,
        )
    }
}

object TransactionFactory {
    @JvmStatic
    fun makeInvokeTransaction(
        contractAddress: Felt,
        calldata: Calldata,
        entryPointSelector: Felt,
        chainId: StarknetChainId,
        nonce: Felt,
        maxFee: Felt = Felt.ZERO,
        signature: Signature = emptyList(),
    ): InvokeTransaction {
        val hash = TransactionHashCalculator.calculateInvokeTxHash(
            contractAddress = contractAddress,
            calldata = calldata,
            chainId = chainId,
            version = INVOKE_VERSION,
            nonce = nonce,
            maxFee = maxFee,
        )

        return InvokeTransaction(contractAddress, calldata, entryPointSelector, hash, maxFee, INVOKE_VERSION, signature, nonce)
    }

    @JvmStatic
    fun makeDeployAccountTransaction(
        classHash: Felt,
        salt: Felt,
        calldata: Calldata,
        chainId: StarknetChainId,
        version: Felt,
        maxFee: Felt = Felt.ZERO,
        signature: Signature = emptyList(),
    ): DeployAccountTransaction {
        val nonce = Felt.ZERO
        val hash = TransactionHashCalculator.calculateDeployAccountTxHash(
            classHash = classHash,
            calldata = calldata,
            salt = salt,
            chainId = chainId,
            version = version,
            maxFee = maxFee,
        )
        return DeployAccountTransaction(
            classHash = classHash,
            contractAddressSalt = salt,
            constructorCalldata = calldata,
            version = version,
            nonce = nonce,
            maxFee = maxFee,
            hash = hash,
            signature = signature,
        )
    }

    @JvmStatic
    fun makeDeclareTransaction(
        classHash: Felt,
        senderAddress: Felt,
        contractDefinition: ContractDefinition,
        chainId: StarknetChainId,
        maxFee: Felt,
        version: Felt,
        nonce: Felt,
        signature: Signature = emptyList(),
    ): DeclareTransaction {
        val hash = TransactionHashCalculator.calculateDeclareTxHash(
            classHash = classHash,
            chainId = chainId,
            senderAddress = senderAddress,
            maxFee = maxFee,
            version = version,
            nonce = nonce,
        )
        return DeclareTransaction(
            classHash = classHash,
            senderAddress = senderAddress,
            contractDefinition = contractDefinition,
            hash = hash,
            maxFee = maxFee,
            version = version,
            signature = signature,
            nonce = nonce,
        )
    }
}
