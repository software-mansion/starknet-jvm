package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.serializers.JsonRpcContractClassPolymorphicSerializer
import com.swmansion.starknet.extensions.base64Gzipped
import kotlinx.serialization.*
import kotlinx.serialization.json.*

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
@Serializable
enum class AbiEntryType {
    @JsonNames("function")
    FUNCTION,

    @JsonNames("constructor")
    CONSTRUCTOR,

    @JsonNames("l1_handler")
    L1_HANDLER,

    @JsonNames("struct")
    STRUCT,

    @JsonNames("event")
    EVENT,
}

@Serializable
sealed class AbiElement

@Serializable
data class AbiEntry(
    val name: String,
    val type: String,
)

@SerialName("function")
@Serializable
data class FunctionAbiEntry(
    val name: String,
    val inputs: List<AbiEntry>,
    val outputs: List<AbiEntry>,
    val type: AbiEntryType,
) : AbiElement()

@SerialName("event")
@Serializable
data class EventAbiEntry(
    val name: String,
    val keys: List<AbiEntry>,
    val data: List<AbiEntry>,
) : AbiElement() {
    val type: AbiEntryType = AbiEntryType.EVENT
}

@Serializable
data class StructMember(
    val name: String,
    val type: String,
    val offset: Int,
)

@SerialName("struct")
@Serializable
data class StructAbiEntry(
    val name: String,
    val size: Int,
    val members: List<StructMember>,
) : AbiElement() {
    val type: AbiEntryType = AbiEntryType.STRUCT
}

@Serializable
data class DeprecatedCairoEntryPoint(
    val offset: Felt,
    val selector: Felt,
)

@Serializable
data class SierraEntryPoint(
        @SerialName("function_idx")
        val functionIdx: Int,
        val selector: Felt,
)

@Serializable
data class ContractDefinition(private val contract: String) {
    private val program: JsonElement
    private val entryPointsByType: JsonElement
    private val abi: JsonElement?

    class InvalidContractException(missingKey: String) :
        RuntimeException("Attempted to parse an invalid contract. Missing key: $missingKey")

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
}

//@Serializable(with = JsonRpcContractClassPolymorphicSerializer::class)
sealed class ContractClassBase

@Serializable
data class DeprecatedContractClass(
    val program: String,

    @SerialName("entry_points_by_type")
    val entryPointsByType: EntryPointsByType,

    val abi: List<AbiElement>? = null,
) : ContractClassBase() {
    @Serializable
    data class EntryPointsByType(
        @SerialName("CONSTRUCTOR")
        val constructor: List<DeprecatedCairoEntryPoint>,

        @SerialName("EXTERNAL")
        val external: List<DeprecatedCairoEntryPoint>,

        @SerialName("L1_HANDLER")
        val l1Handler: List<DeprecatedCairoEntryPoint>,
    )
}

@Serializable
data class ContractClass(
        @SerialName("sierra_program")
        val sierraProgram: List<Felt>,

        @SerialName("sierra_version")
        val sierraVersion: String,

        @SerialName("entry_points_by_type")
        val entryPointsByType: EntryPointsByType,

        val abi: String,
) : ContractClassBase() {
    @Serializable
    data class EntryPointsByType(
            @SerialName("CONSTRUCTOR")
            val constructor: List<SierraEntryPoint>,

            @SerialName("EXTERNAL")
            val external: List<SierraEntryPoint>,

            @SerialName("L1_HANDLER")
            val l1Handler: List<SierraEntryPoint>,
    )
}

@Serializable
data class GetClassPayload(
    @SerialName("class_hash")
    val classHash: Felt,

    @SerialName("block_id")
    var blockId: String,
)

@Serializable
data class GetClassAtPayload(
    @SerialName("block_id")
    val blockId: String,

    @SerialName("contract_address")
    val contractAddress: Felt,
)
