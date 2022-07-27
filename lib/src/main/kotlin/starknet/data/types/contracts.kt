@file:JvmName("Contracts")

package starknet.data.types

enum class AbiEntryType {
    FELT,
    FELT_ARRAY,
    STRING
}

data class AbiEntry(
    val name: String,
    val type: AbiEntryType
)

sealed class AbiElement

data class FunctionAbi(
    val name: String,
    val inputs: List<AbiEntry>,
    val outputs: List<AbiEntry>,
) : AbiElement()

data class StructAbi(
    val name: String,
) : AbiElement()

data class Abi(
    val values: List<AbiElement>
)

data class Program(
    val code: String
)

data class CompiledContract(
    val abi: Abi,
    // TODO: Add entrypoints, once they are defined
    val program: Program,
)
