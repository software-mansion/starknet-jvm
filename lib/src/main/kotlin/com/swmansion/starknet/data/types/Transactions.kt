package com.swmansion.starknet.data.types

import com.swmansion.starknet.crypto.StarknetCurve
import com.swmansion.starknet.extensions.base64Gzipped
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

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

@Serializable
enum class BlockTag(val tag: String) {
    LATEST("latest"), PENDING("pending")
}

@Serializable(with = BlockHashOrTagSerializer::class)
sealed class BlockHashOrTag {
    data class Hash(
        val blockHash: Felt,
    ) : BlockHashOrTag() {
        override fun string(): String {
            return blockHash.hexString()
        }
    }

    data class Tag(
        val blockTag: BlockTag,
    ) : BlockHashOrTag() {
        override fun string(): String {
            return blockTag.tag
        }
    }

    abstract fun string(): String
}

class BlockHashOrTagSerializer : KSerializer<BlockHashOrTag> {
    override fun deserialize(decoder: Decoder): BlockHashOrTag {
        val value = decoder.decodeString()

        if (BlockTag.values().map { it.tag }.contains(value)) {
            val tag = BlockTag.valueOf(value)
            return BlockHashOrTag.Tag(tag)
        }

        return BlockHashOrTag.Hash(Felt.fromHex(value))
    }

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("BlockHashOrTag", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BlockHashOrTag) {
        encoder.encodeString(value.string())
    }
}

@Serializable
data class InvokeFunctionPayload(
    @SerialName("function_invocation")
    val invocation: Call,

    val signature: Signature?,

    @SerialName("max_fee")
    val maxFee: Felt?,

    val version: Felt?,
)

class InvalidContractException(missingKey: String) :
    Exception("Attempted to parse an invalid contract. Missing key: $missingKey")

data class ContractDefinition(var contract: String) {
    private val program: JsonElement
    private val entryPointsByType: JsonElement
    private val abi: JsonElement?

    init {
        val (program, entryPointsByType, abi) = parseContract(contract)
        this.program = program
        this.entryPointsByType = entryPointsByType
        this.abi = abi
    }

    private fun parseContract(contract: String): Triple<JsonElement, JsonElement, JsonElement> {
        val compiledContract = Json.parseToJsonElement(contract).jsonObject
        val program = compiledContract["program"] ?: throw InvalidContractException("program")
        val entryPointsByType =
            compiledContract["entry_points_by_type"] ?: throw InvalidContractException("entry_points_by_type")
        val abi = compiledContract["abi"] ?: JsonArray(emptyList())
        return Triple(program, entryPointsByType, abi)
    }

    fun toJson(): JsonObject {
        return buildJsonObject {
            put("program", program.toString().base64Gzipped())
            put("entry_points_by_type", entryPointsByType)
            if (abi != null) put("abi", abi) else putJsonArray("abi") { emptyList<Any>() }
        }
    }

    fun toRpcJson(): JsonObject {
        return buildJsonObject {
            put("program", program.toString().base64Gzipped())
            put("entry_points_by_type", entryPointsByType)
        }
    }
}

data class DeployTransactionPayload(
    val contractDefinition: ContractDefinition,
    val salt: Felt,
    val constructorCalldata: Calldata,
    val version: Felt,
)

data class DeclareTransactionPayload(
    val contractDefinition: ContractDefinition,
    val maxFee: Felt,
    val nonce: Felt,
    val signature: Signature,
    val version: Felt,
)

sealed class Transaction {
    abstract val type: TransactionType

    abstract fun getHash(): Felt
}

data class DeclareTransaction(
    val nonce: Felt,
    val contractClass: CompiledContract,
    val signerAddress: Felt,
    val signature: Signature,
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
    val nonce: Felt?,
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

    override fun getHash(): Felt = StarknetCurve.pedersenOnElements(
        type.txPrefix,
        version,
        contractAddress,
        entrypointSelector,
        StarknetCurve.pedersenOnElements(calldata),
        maxFee,
        chainId,
    )
}
