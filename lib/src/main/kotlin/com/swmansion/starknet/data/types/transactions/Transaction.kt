package com.swmansion.starknet.data.types.transactions

import com.swmansion.starknet.data.Cairo1ClassHashCalculator
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
    abstract val hash: Felt?
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
    @SerialName("contract_address_salt")
    val contractAddressSalt: Felt,

    @SerialName("constructor_calldata")
    @JsonNames("calldata")
    val constructorCalldata: Calldata,

    @SerialName("class_hash")
    val classHash: Felt,

    // not in RPC spec
    @SerialName("transaction_hash")
    @JsonNames("txn_hash")
    override val hash: Felt? = null,

    // not in RPC spec
    @SerialName("max_fee")
    override val maxFee: Felt = Felt.ZERO,

    @SerialName("version")
    override val version: Felt,

    // not in RPC spec
    @SerialName("signature")
    override val signature: Signature = emptyList(),

    @SerialName("nonce")
    override val nonce: Felt = Felt.ZERO,

    @SerialName("type")
    override val type: TransactionType = TransactionType.DEPLOY,
) : Transaction()

@Serializable
@SerialName("INVOKE_FUNCTION")
sealed class InvokeTransaction() : Transaction() {
    abstract val calldata: Calldata
    override val type: TransactionType = TransactionType.INVOKE
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class InvokeTransactionV1(
    @SerialName("calldata")
    override val calldata: Calldata,

    @SerialName("sender_address")
    @JsonNames("contract_address")
    val senderAddress: Felt,

    // not in RPC spec
    @SerialName("transaction_hash")
    @JsonNames("txn_hash")
    override val hash: Felt? = null,

    @SerialName("max_fee")
    override val maxFee: Felt,

    @SerialName("version")
    override val version: Felt = INVOKE_VERSION,

    @SerialName("signature")
    override val signature: Signature,

    @SerialName("nonce")
    override val nonce: Felt,

) : InvokeTransaction() {
    fun toPayload(): InvokeTransactionPayload {
        return InvokeTransactionPayload(
            calldata = calldata,
            signature = signature,
            maxFee = maxFee,
            nonce = nonce,
            senderAddress = senderAddress,
            version = version,
        )
    }

    companion object {
        @JvmStatic
        internal fun fromPayload(payload: InvokeTransactionPayload): InvokeTransaction {
            return InvokeTransactionV1(
                senderAddress = payload.senderAddress,
                calldata = payload.calldata,
                hash = Felt.ZERO,
                maxFee = payload.maxFee,
                version = payload.version,
                signature = payload.signature,
                nonce = payload.nonce,
            )
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class InvokeTransactionV0(
    @SerialName("calldata")
    override val calldata: Calldata,

    // not in RPC spec
    @SerialName("transaction_hash")
    @JsonNames("txn_hash")
    override val hash: Felt? = null,

    @SerialName("max_fee")
    override val maxFee: Felt,

    @SerialName("version")
    override val version: Felt = Felt.ZERO,

    @SerialName("signature")
    override val signature: Signature,

    // not in RPC spec
    // TODO: consider make it nullable so it's not included in serialized json
    @SerialName("nonce")
    override val nonce: Felt = Felt.ZERO,

    @SerialName("contract_address")
    val contractAddress: Felt,

    @SerialName("entry_point_selector")
    val entryPointSelector: Felt,
) : InvokeTransaction()

@Serializable
@SerialName("DECLARE")
sealed class DeclareTransaction() : Transaction()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class DeclareTransactionV0(
    @SerialName("class_hash")
    val classHash: Felt? = null,

    @SerialName("sender_address")
    val senderAddress: Felt,

    // not in RPC spec
    @SerialName("transaction_hash")
    @JsonNames("txn_hash")
    override val hash: Felt? = null,

    @SerialName("max_fee")
    override val maxFee: Felt,

    @SerialName("version")
    override val version: Felt = Felt.ZERO,

    @SerialName("signature")
    override val signature: Signature,

    @SerialName("nonce")
    override val nonce: Felt = Felt.ZERO,

    @SerialName("type")
    override val type: TransactionType = TransactionType.DECLARE,

    @SerialName("contract_class")
    val contractDefinition: Cairo0ContractDefinition? = null,
) : DeclareTransaction()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class DeclareTransactionV1(
    @SerialName("class_hash")
    val classHash: Felt? = null,

    @SerialName("sender_address")
    val senderAddress: Felt,

    // not in RPC spec
    @SerialName("transaction_hash")
    @JsonNames("txn_hash")
    override val hash: Felt? = null,

    @SerialName("max_fee")
    override val maxFee: Felt,

    @SerialName("version")
    override val version: Felt = Felt.ONE,

    @SerialName("signature")
    override val signature: Signature,

    @SerialName("nonce")
    override val nonce: Felt,

    @SerialName("type")
    override val type: TransactionType = TransactionType.DECLARE,

    @SerialName("contract_class")
    val contractDefinition: Cairo0ContractDefinition? = null,
) : DeclareTransaction() {
    @Throws(ConvertingToPayloadFailedException::class)
    internal fun toPayload(): DeclareTransactionV1Payload {
        contractDefinition ?: throw ConvertingToPayloadFailedException()

        return DeclareTransactionV1Payload(
            contractDefinition = contractDefinition,
            senderAddress = senderAddress,
            maxFee = maxFee,
            nonce = nonce,
            signature = signature,
        )
    }

    internal class ConvertingToPayloadFailedException : RuntimeException()
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@SerialName("DECLARE")
data class DeclareTransactionV2(
    @SerialName("class_hash")
    val classHash: Felt? = null,

    @SerialName("sender_address")
    val senderAddress: Felt,

    // not in RPC spec
    @SerialName("transaction_hash")
    @JsonNames("txn_hash")
    override val hash: Felt? = null,

    @SerialName("max_fee")
    override val maxFee: Felt,

    @SerialName("version")
    override val version: Felt = Felt(2),

    @SerialName("signature")
    override val signature: Signature,

    @SerialName("nonce")
    override val nonce: Felt,

    @SerialName("compiled_class_hash")
    val compiledClassHash: Felt,

    @SerialName("contract_class")
    val contractDefinition: Cairo1ContractDefinition? = null,

    @SerialName("type")
    override val type: TransactionType = TransactionType.DECLARE,
) : DeclareTransaction() {
    @Throws(ConvertingToPayloadFailedException::class)
    internal fun toPayload(): DeclareTransactionV2Payload {
        contractDefinition ?: throw ConvertingToPayloadFailedException()
        return DeclareTransactionV2Payload(
            contractDefinition = contractDefinition,
            senderAddress = senderAddress,
            maxFee = maxFee,
            nonce = nonce,
            signature = signature,
            compiledClassHash = compiledClassHash,
        )
    }

    internal class ConvertingToPayloadFailedException : RuntimeException()
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

    // not in RPC spec
    @SerialName("transaction_hash")
    @JsonNames("txn_hash")
    override val hash: Felt? = null,

    @SerialName("max_fee")
    override val maxFee: Felt = Felt.ZERO,

    @SerialName("version")
    override val version: Felt,

    @SerialName("signature")
    override val signature: Signature = emptyList(),

    @SerialName("nonce")
    override val nonce: Felt,

    @SerialName("type")
    override val type: TransactionType = TransactionType.L1_HANDLER,
) : Transaction()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@SerialName("DEPLOY_ACCOUNT")
data class DeployAccountTransaction(
    @SerialName("class_hash")
    @JsonNames("class_hash")
    val classHash: Felt,

    // not in RPC spec
    @SerialName("contract_address")
    val contractAddress: Felt = Felt.ZERO,

    @SerialName("contract_address_salt")
    val contractAddressSalt: Felt,

    @SerialName("constructor_calldata")
    @JsonNames("calldata")
    val constructorCalldata: Calldata,

    // not in RPC spec
    @SerialName("transaction_hash")
    @JsonNames("txn_hash")
    override val hash: Felt? = null,

    @SerialName("max_fee")
    override val maxFee: Felt,

    @SerialName("version")
    override val version: Felt,

    @SerialName("signature")
    override val signature: Signature,

    @SerialName("nonce")
    override val nonce: Felt,

    @SerialName("type")
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
        senderAddress: Felt,
        calldata: Calldata,
        chainId: StarknetChainId,
        nonce: Felt,
        maxFee: Felt = Felt.ZERO,
        signature: Signature = emptyList(),
        version: Felt,
    ): InvokeTransactionV1 {
        val hash = TransactionHashCalculator.calculateInvokeTxHash(
            contractAddress = senderAddress,
            calldata = calldata,
            chainId = chainId,
            version = version,
            nonce = nonce,
            maxFee = maxFee,
        )
        return InvokeTransactionV1(
            hash = hash,
            senderAddress = senderAddress,
            calldata = calldata,
            maxFee = maxFee,
            version = version,
            signature = signature,
            nonce = nonce,
        )
    }

    @JvmStatic
    fun makeDeployAccountTransaction(
        classHash: Felt,
        contractAddress: Felt,
        salt: Felt,
        calldata: Calldata,
        chainId: StarknetChainId,
        version: Felt,
        maxFee: Felt = Felt.ZERO,
        signature: Signature = emptyList(),
        nonce: Felt = Felt.ZERO,
    ): DeployAccountTransaction {
        val hash = TransactionHashCalculator.calculateDeployAccountTxHash(
            classHash = classHash,
            calldata = calldata,
            salt = salt,
            chainId = chainId,
            version = version,
            maxFee = maxFee,
            nonce = nonce,
        )
        return DeployAccountTransaction(
            classHash = classHash,
            contractAddress = contractAddress,
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
    fun makeDeclareV1Transaction(
        classHash: Felt,
        senderAddress: Felt,
        contractDefinition: Cairo0ContractDefinition,
        chainId: StarknetChainId,
        maxFee: Felt,
        version: Felt,
        nonce: Felt,
        signature: Signature = emptyList(),
    ): DeclareTransactionV1 {
        val hash = TransactionHashCalculator.calculateDeclareV1TxHash(
            classHash = classHash,
            chainId = chainId,
            senderAddress = senderAddress,
            maxFee = maxFee,
            version = version,
            nonce = nonce,
        )
        return DeclareTransactionV1(
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

    @JvmStatic
    fun makeDeclareV2Transaction(
        senderAddress: Felt,
        contractDefinition: Cairo1ContractDefinition,
        chainId: StarknetChainId,
        maxFee: Felt,
        version: Felt,
        nonce: Felt,
        casmContractDefinition: CasmContractDefinition,
        signature: Signature = emptyList(),
    ): DeclareTransactionV2 {
        val classHash = Cairo1ClassHashCalculator.computeSierraClassHash(contractDefinition)
        val compiledClassHash = Cairo1ClassHashCalculator.computeCasmClassHash(casmContractDefinition)
        val hash = TransactionHashCalculator.calculateDeclareV2TxHash(
            classHash = classHash,
            chainId = chainId,
            senderAddress = senderAddress,
            maxFee = maxFee,
            version = version,
            nonce = nonce,
            compiledClassHash = compiledClassHash,
        )
        return DeclareTransactionV2(
            hash = hash,
            classHash = classHash,
            senderAddress = senderAddress,
            contractDefinition = contractDefinition,
            maxFee = maxFee,
            version = version,
            signature = signature,
            nonce = nonce,
            compiledClassHash = compiledClassHash,
        )
    }
}
