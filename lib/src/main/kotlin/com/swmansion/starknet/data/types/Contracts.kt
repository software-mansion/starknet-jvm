package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.serializers.DeprecatedCairoEntryPointSerializer
import com.swmansion.starknet.extensions.base64Gzipped
import kotlinx.serialization.*
import kotlinx.serialization.json.*

typealias Cairo2ContractDefinition = Cairo1ContractDefinition

@OptIn(ExperimentalSerializationApi::class)
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

@OptIn(ExperimentalSerializationApi::class)
@Serializable
enum class StateMutabilityType {
    @JsonNames("view")
    VIEW,
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
    val stateMutability: StateMutabilityType? = null,
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

@Serializable(with = DeprecatedCairoEntryPointSerializer::class)
data class DeprecatedCairoEntryPoint(
    val offset: NumAsHex,
    val selector: Felt,
)

@Serializable
data class SierraEntryPoint(
    @SerialName("function_idx")
    val functionIdx: Int,
    val selector: Felt,
)

@Serializable
data class DeprecatedEntryPointsByType(
    @SerialName("CONSTRUCTOR")
    val constructor: List<DeprecatedCairoEntryPoint>,

    @SerialName("EXTERNAL")
    val external: List<DeprecatedCairoEntryPoint>,

    @SerialName("L1_HANDLER")
    val l1Handler: List<DeprecatedCairoEntryPoint>,
)

@Serializable
data class EntryPointsByType(
    @SerialName("CONSTRUCTOR")
    val constructor: List<SierraEntryPoint>,

    @SerialName("EXTERNAL")
    val external: List<SierraEntryPoint>,

    @SerialName("L1_HANDLER")
    val l1Handler: List<SierraEntryPoint>,
)

@Serializable
data class Cairo0ContractDefinition @JvmOverloads constructor(
    private val contract: String,
    @Transient
    private val ignoreUnknownJsonKeys: Boolean = false,
) {
    private val program: JsonElement
    private val entryPointsByType: JsonElement
    private val abi: JsonElement?

    private val deserializationJson by lazy { Json { ignoreUnknownKeys = ignoreUnknownJsonKeys } }

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

        val sourceEntryPointsByType =
            compiledContract["entry_points_by_type"] ?: throw InvalidContractException("entry_points_by_type")
        val deserializedEntryPointsByType = deserializationJson.decodeFromJsonElement(DeprecatedEntryPointsByType.serializer(), sourceEntryPointsByType)
        val entryPointsByType = Json.encodeToJsonElement(deserializedEntryPointsByType)

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

@Serializable
data class Cairo1ContractDefinition @JvmOverloads constructor(
    private val contract: String,
    @Transient
    private val ignoreUnknownJsonKeys: Boolean = false,
) {
    private val sierraProgram: JsonElement
    private val entryPointsByType: JsonElement
    private val contractClassVersion: JsonElement
    private val abi: JsonElement?

    internal val deserializationJson by lazy { Json { ignoreUnknownKeys = ignoreUnknownJsonKeys } }

    private val jsonWithPrettyPrint by lazy { Json { prettyPrint = true } }

    class InvalidContractException(missingKey: String) :
        RuntimeException("Attempted to parse an invalid contract. Missing key: $missingKey")

    init {
        val (sierraProgram, entryPointsByType, contractClassVersion, abi) = parseContract(contract)
        this.sierraProgram = sierraProgram
        this.entryPointsByType = entryPointsByType
        this.contractClassVersion = contractClassVersion
        this.abi = abi
    }

    private fun parseContract(contract: String): Array<JsonElement> {
        val compiledContract = Json.parseToJsonElement(contract).jsonObject
        val sierraProgram = compiledContract["sierra_program"] ?: throw InvalidContractException("sierra_program")
        val entryPointsByType =
            compiledContract["entry_points_by_type"] ?: throw InvalidContractException("entry_points_by_type")
        val contractClassVersion = compiledContract["contract_class_version"] ?: throw InvalidContractException("contract_class_version")
        val abi = compiledContract["abi"] ?: JsonPrimitive("")
        return arrayOf(sierraProgram, entryPointsByType, contractClassVersion, abi)
    }

    fun toJson(): JsonObject {
        return buildJsonObject {
            put("sierra_program", sierraProgram)
            put("entry_points_by_type", entryPointsByType)
            put("contract_class_version", contractClassVersion)
            if (abi != null) put(
                "abi",
                jsonWithPrettyPrint.encodeToString(abi)
                    .lineSequence().map { it.trim() }.joinToString("\n")
                    .replace("\n", "")
                    .replace(",\"", ", \"")
                    .replace(",{", ", {")
                    .replace(",[", ", ["),
            ) else put("abi", "")
        }
    }
}

@Serializable
data class CasmContractDefinition @JvmOverloads constructor(
    private val contract: String,
    @Transient
    private val ignoreUnknownJsonKeys: Boolean = false,
) {
    private val casmClassVersion: JsonElement = JsonPrimitive("COMPILED_CLASS_V1")
    private val prime: JsonElement
    private val hints: JsonElement
    private val compilerVersion: JsonElement
    private val entryPointsByType: JsonElement
    private val bytecode: JsonElement

    internal val deserializationJson by lazy { Json { ignoreUnknownKeys = ignoreUnknownJsonKeys } }

    class InvalidContractException(missingKey: String) :
        RuntimeException("Attempted to parse an invalid contract. Missing key: $missingKey")

    init {
        val (entryPointsByType, bytecode, prime, hints, compilerVersion) = parseContract(contract)
        this.entryPointsByType = entryPointsByType
        this.bytecode = bytecode
        this.prime = prime
        this.hints = hints
        this.compilerVersion = compilerVersion
    }

    private fun parseContract(contract: String): Array<JsonElement> {
        val compiledContract = Json.parseToJsonElement(contract).jsonObject
        val entryPointsByType =
            compiledContract["entry_points_by_type"] ?: throw InvalidContractException("entry_points_by_type")
        val bytecode = compiledContract["bytecode"] ?: throw InvalidContractException("bytecode")
        val prime = compiledContract["prime"] ?: throw InvalidContractException("prime")
        val hints = compiledContract["hints"] ?: throw InvalidContractException("hints")
        val compilerVersion = compiledContract["compiler_version"] ?: throw InvalidContractException("compiler_version")
        return arrayOf(entryPointsByType, bytecode, prime, hints, compilerVersion)
    }

    fun toJson(): JsonObject {
        return buildJsonObject {
            put("casm_class_version", casmClassVersion)
            put("entry_points_by_type", entryPointsByType)
            put("bytecode", bytecode)
            put("prime", prime)
            put("hints", buildJsonArray { hints.toString() })
            put("compiler_version", compilerVersion)
        }
    }
}

sealed class ContractClassBase

@Serializable
data class DeprecatedContractClass(
    val program: String,

    @SerialName("entry_points_by_type")
    val entryPointsByType: DeprecatedEntryPointsByType,

    val abi: List<AbiElement>? = null,
) : ContractClassBase()

@Serializable
data class ContractClass(
    @SerialName("sierra_program")
    val sierraProgram: List<Felt>,

    @SerialName("entry_points_by_type")
    val entryPointsByType: EntryPointsByType,

    @SerialName("contract_class_version")
    val contractClassVersion: String,

    val abi: String? = null,
) : ContractClassBase()

@Serializable
data class CasmContractClass(
    @SerialName("casm_class_version")
    val casmClassVersion: String = "COMPILED_CLASS_V1",

    val prime: String,

    val hints: List<String>,

    @SerialName("compiler_version")
    val compilerVersion: String,

    @SerialName("entry_points_by_type")
    val entryPointsByType: EntryPointsByType,

    val bytecode: List<Felt>,
) {
    @Serializable
    data class EntryPointsByType(
        @SerialName("CONSTRUCTOR")
        val constructor: List<CasmEntryPoint>,

        @SerialName("EXTERNAL")
        val external: List<CasmEntryPoint>,

        @SerialName("L1_HANDLER")
        val l1Handler: List<CasmEntryPoint>,
    )
}

@Serializable
data class CasmEntryPoint(
    val selector: Felt,
    val offset: Int,
    val builtins: List<String>? = null,
)

@Serializable
data class GetClassPayload(
    @SerialName("class_hash")
    val classHash: Felt,

    @SerialName("block_id")
    var blockId: BlockId,
)

@Serializable
data class GetClassAtPayload(
    @SerialName("block_id")
    val blockId: BlockId,

    @SerialName("contract_address")
    val contractAddress: Felt,
)
