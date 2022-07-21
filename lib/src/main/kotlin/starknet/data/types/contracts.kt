@file:JvmName("Contracts")

package starknet.data.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import starknet.data.toHex
import java.math.BigInteger

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

@Serializable
data class ContractEntryPoint(
    val offset: String,
    val selector: Felt
) {
    constructor(offset: Int, selector: Felt) :
        this(toHex(offset), selector)
}

@Serializable
data class ContractClass(
    val program: String,
    @SerialName("entry_points_by_type") val entryPointsByType: EntryPointsByType
) {
    @Serializable
    data class EntryPointsByType(
        @SerialName("CONSTRUCTOR") val constructor: List<ContractEntryPoint>,
        @SerialName("EXTERNAL") val external: List<ContractEntryPoint>,
        @SerialName("L1_HANDLER") val l1Handler: List<ContractEntryPoint>,
    )
}

@Serializable
data class GetClassPayload(
    @SerialName("class_hash") val classHash: Felt
)

@Serializable
data class GetClassAtPayload(
    @SerialName("block_id") val blockId: String,
    @SerialName("contract_address") val contractAddress: Felt
)

