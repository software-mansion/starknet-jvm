@file:JvmName("Contracts")

package starknet.data.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class AbiEntryType {
    FELT,
    FELT_ARRAY,
    STRING
}

@Serializable
data class AbiEntry(
    val name: String,
    val type: AbiEntryType
)

@Serializable
sealed class AbiElement

@Serializable
data class FunctionAbi(
    val name: String,
    val inputs: List<AbiEntry>,
    val outputs: List<AbiEntry>,
) : AbiElement()

@Serializable
data class StructAbi(
    val name: String,
) : AbiElement()

@Serializable
data class Abi(
    val values: List<AbiElement>
)

@Serializable
data class Program(
    val code: String
)

@Serializable
data class EntryPoint(
    val selector: Felt,
    val offset: Felt
)

@Serializable
data class EntryPointsByType(
    @SerialName("CONSTRUCTOR")
    val constructor: List<EntryPoint>,

    @SerialName("EXTERNAL")
    val external: List<EntryPoint>,

    @SerialName("L1_HANDLER")
    val l1Handler: List<EntryPoint>
)

@Serializable
data class CompiledContract(
    @SerialName("entry_points_by_type")
    val entryPointsByType: EntryPointsByType,

    @SerialName("program")
    val program: String,

    @SerialName("abi")
    val abi: List<String> = emptyList(),
)
