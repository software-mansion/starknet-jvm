package com.swmansion.starknet.data.types.transactions

import com.swmansion.starknet.crypto.StarknetCurve
import com.swmansion.starknet.data.types.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
enum class TransactionType(val txPrefix: Felt) {
    @SerialName("DECLARE")
    DECLARE(Felt.fromHex("0x6465636c617265")), // encodeShortString('declare'),

    @SerialName("DEPLOY")
    DEPLOY(Felt.fromHex("0x6465706c6f79")), // encodeShortString('deploy'),

    @SerialName("INVOKE")
    @JsonNames("INVOKE_FUNCTION")
    INVOKE(Felt.fromHex("0x696e766f6b65")), // encodeShortString('invoke'),
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
    val entryPointSelector: Felt,

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

    override val type: TransactionType = TransactionType.INVOKE,
) : Transaction() {
    internal fun toPayload(): InvokeFunctionPayload {
        val invocation = Call(
            contractAddress = contractAddress,
            calldata = calldata,
            entrypoint = entryPointSelector,
        )

        return InvokeFunctionPayload(
            invocation = invocation,
            signature = signature,
            maxFee = maxFee,
            version = version,
        )
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@SerialName("DECLARE")
data class DeclareTransaction(
    @SerialName("class_hash")
    @JsonNames("contract_class")
    val classHash: Felt,

    @SerialName("sender_address")
    val senderAddress: Felt,

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

    override val type: TransactionType = TransactionType.DECLARE,
) : Transaction()

object TransactionFactory {
    @JvmStatic
    fun makeInvokeTransaction(
        contractAddress: Felt,
        calldata: Calldata,
        entryPointSelector: Felt,
        chainId: StarknetChainId,
        maxFee: Felt = Felt.ZERO,
        version: Felt = Felt.ZERO,
        signature: Signature = emptyList(),
        nonce: Felt = Felt.ZERO,
    ): InvokeTransaction {
        val hash = StarknetCurve.pedersenOnElements(
            TransactionType.INVOKE.txPrefix,
            version,
            contractAddress,
            entryPointSelector,
            StarknetCurve.pedersenOnElements(calldata),
            maxFee,
            chainId.value,
        )

        return InvokeTransaction(contractAddress, calldata, entryPointSelector, hash, maxFee, version, signature, nonce)
    }
}
