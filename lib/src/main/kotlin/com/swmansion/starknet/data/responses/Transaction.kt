package com.swmansion.starknet.data.responses

import com.swmansion.starknet.crypto.StarknetCurve
import com.swmansion.starknet.data.types.Calldata
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.Signature
import com.swmansion.starknet.data.types.StarknetChainId
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

enum class TransactionType(val txPrefix: Felt) {
    DECLARE(Felt.fromHex("0x6465636c617265")), // encodeShortString('declare'),
    DEPLOY(Felt.fromHex("0x6465706c6f79")), // encodeShortString('deploy'),
    INVOKE(Felt.fromHex("0x696e766f6b65")), // encodeShortString('invoke'),
}

@Serializable
sealed class Transaction {
    abstract val hash: Felt
    abstract val maxFee: Felt
    abstract val version: Felt
    abstract val signature: Signature
    abstract val nonce: Felt
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@SerialName("DEPLOY")
// OptIn needed because @JsonNames is part of the experimental serialization api
data class DeployTransaction(
    @JsonNames("contract_address")
    val contractAddress: Felt,

    @JsonNames("contract_address_salt")
    val contractAddressSalt: Felt,

    @JsonNames("constructor_calldata", "calldata")
    val constructorCalldata: Calldata,

    @JsonNames("class_hash")
    val classHash: Felt,

    @JsonNames("transaction_hash", "txn_hash")
    override val hash: Felt,

    @JsonNames("max_fee")
    override val maxFee: Felt = Felt.ZERO,

    @JsonNames("version")
    override val version: Felt = Felt.ZERO,

    @JsonNames("signature")
    override val signature: Signature = emptyList(),

    @JsonNames("nonce")
    override val nonce: Felt = Felt.ZERO,
) : Transaction()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@SerialName("INVOKE_FUNCTION")
data class InvokeTransaction(
    @JsonNames("contract_address")
    val contractAddress: Felt,

    @JsonNames("calldata")
    val calldata: Calldata,

    @JsonNames("entry_point_selector")
    val entryPointSelector: Felt,

    @JsonNames("transaction_hash", "txn_hash")
    override val hash: Felt,

    @JsonNames("max_fee")
    override val maxFee: Felt = Felt.ZERO,

    @JsonNames("version")
    override val version: Felt = Felt.ZERO,

    @JsonNames("signature")
    override val signature: Signature = emptyList(),

    @JsonNames("nonce")
    override val nonce: Felt = Felt.ZERO,
) : Transaction()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@SerialName("DECLARE")
data class DeclareTransaction(
    @JsonNames("class_hash", "contract_class")
    val classHash: Felt,

    @JsonNames("sender_address")
    val senderAddress: Felt,

    @JsonNames("transaction_hash", "txn_hash")
    override val hash: Felt,

    @JsonNames("max_fee")
    override val maxFee: Felt = Felt.ZERO,

    @JsonNames("version")
    override val version: Felt = Felt.ZERO,

    @JsonNames("signature")
    override val signature: Signature = emptyList(),

    @JsonNames("nonce")
    override val nonce: Felt = Felt.ZERO,
) : Transaction()

fun makeInvokeTransaction(
    contractAddress: Felt,
    calldata: Calldata,
    entryPointSelector: Felt,
    maxFee: Felt = Felt.ZERO,
    version: Felt = Felt.ZERO,
    signature: Signature = emptyList(),
    nonce: Felt = Felt.ZERO,
    chainId: StarknetChainId,
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
