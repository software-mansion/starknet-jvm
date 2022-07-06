@file:JvmName("Transactions")

package starknet.data.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import starknet.crypto.StarknetCurveSignature
import starknet.crypto.pedersenOnElements
import types.Felt

typealias Calldata = List<Felt>
typealias Signature = List<Felt>

enum class TransactionStatus {
    NOT_RECEIVED, RECEIVED, PENDING, ACCEPTED_ON_L1, ACCEPTED_ON_L2, REJECTED
}

enum class TransactionType(val txPrefix: Felt) {
    DECLARE(Felt.fromHex("0x6465636c617265")), // encodeShortString('declare'),
    DEPLOY(Felt.fromHex("0x6465706c6f79")), // encodeShortString('deploy'),
    INVOKE(Felt.fromHex("0x696e766f6b65")), // encodeShortString('invoke'),
}

enum class StarknetChainId(val value: Felt) {
    MAINNET(Felt.fromHex("0x534e5f4d41494e")), // encodeShortString('SN_MAIN'),
    TESTNET(Felt.fromHex("0x534e5f474f45524c49")), // encodeShortString('SN_GOERLI'),
}

enum class BlockTag(val tag: String) {
    LATEST("latest"),
    PENDING("pending")
}

@Serializable
sealed class BlockHashOrTag() {
    data class Hash(
        val blockHash: Felt
    ) : BlockHashOrTag() {
        override fun string(): String {
            return blockHash.hexString()
        }
    }

    data class Tag(
        val blockTag: BlockTag
    ) : BlockHashOrTag() {
        override fun string(): String {
            return blockTag.tag
        }
    }

    abstract fun string(): String
}

@Serializable
data class InvokeFunctionPayload(
    @SerialName("function_invocation") val invocation: Call,
    val signature: Signature?,
    @SerialName("max_fee") val maxFee: Felt?,
    val version: Felt?
)

sealed class Transaction {
    abstract val type: TransactionType

    abstract fun getHash(): Felt
}

data class DeclareTransaction(
    val nonce: Felt, val contractClass: CompiledContract, val signerAddress: Felt, val signature: StarknetCurveSignature
) : Transaction() {
    override val type = TransactionType.DECLARE
    override fun getHash(): Felt {
        TODO("Not yet implemented")
    }
}

data class DeployTransaction(
    val contractDefinition: CompiledContract,
    val contractAddressSalt: Felt,
    val constructorCalldata: Calldata,
    val nonce: Felt?
) : Transaction() {
    override val type = TransactionType.DEPLOY

    override fun getHash(): Felt {
        TODO("Not yet implemented")
    }
}

@Serializable
data class InvokeFunctionTransaction(
    @SerialName("contract_address") val contractAddress: Felt,
    val signature: Signature?,
    @SerialName("entry_point_selector") val entryPointSelector: String,
    val calldata: List<Felt>?,
    @SerialName("max_fee") val maxFee: Felt,
    val version: Felt,
)

data class InvokeTransaction(
    val contractAddress: Felt,
    val entrypointSelector: Felt,
    val calldata: Calldata,
    val chainId: Felt,
    val nonce: Felt,
    val maxFee: Felt,
    val version: Felt = Felt.ZERO,
    val signature: Signature? = null,
) : Transaction() {
    override val type = TransactionType.INVOKE

    override fun getHash(): Felt = pedersenOnElements(
        type.txPrefix,
        version,
        contractAddress,
        entrypointSelector,
        pedersenOnElements(calldata),
        maxFee,
        chainId,
    )
}

