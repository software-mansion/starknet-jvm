@file:JvmName("Contracts")

package com.swmansion.starknet.data.types

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import com.swmansion.starknet.extensions.base64Gzipped

enum class AbiEntryType {
    FELT,
    FELT_ARRAY,
    STRING
}

@Serializable
data class AbiEntry(
    val name: String,
    val type: AbiEntryType,
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
    val values: List<AbiElement>,
)

@Serializable
data class Program(
    val code: String,
)

@Serializable
data class EntryPoint(
    val selector: Felt,
    val offset: Felt,
)

@Serializable
data class EntryPointsByType(
    @SerialName("CONSTRUCTOR")
    val constructor: List<EntryPoint>,

    @SerialName("EXTERNAL")
    val external: List<EntryPoint>,

    @SerialName("L1_HANDLER")
    val l1Handler: List<EntryPoint>,
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

@Serializable
data class ContractEntryPoint(
    val offset: Felt,
    val selector: Felt,
)

@Serializable
data class ContractClass(
    val program: String,

    @SerialName("entry_points_by_type")
    val entryPointsByType: EntryPointsByType,
) {
    @Serializable
    data class EntryPointsByType(
        @SerialName("CONSTRUCTOR")
        val constructor: List<ContractEntryPoint>,

        @SerialName("EXTERNAL")
        val external: List<ContractEntryPoint>,

        @SerialName("L1_HANDLER")
        val l1Handler: List<ContractEntryPoint>,
    )
}

object ContractClassGatewaySerializer : KSerializer<ContractClass> {
    @Serializable
    data class ContractClassGateway(
        val program: JsonElement,

        @SerialName("entry_points_by_type")
        val entryPointsByType: ContractClass.EntryPointsByType,
    )

    override fun deserialize(decoder: Decoder): ContractClass {
        val response = ContractClassGateway.serializer().deserialize(decoder)

        val programString = response.program.toString()
        val program = programString.base64Gzipped()

        // FIXME: It doesn't produce the same output as the rpc endpoint

        return ContractClass(program, response.entryPointsByType)
    }

    override val descriptor: SerialDescriptor
        get() = ContractClass.serializer().descriptor

    override fun serialize(encoder: Encoder, value: ContractClass) {
        throw Exception("Class used for deserialization only.")
    }
}

@Serializable
data class GetClassPayload(
    @SerialName("class_hash")
    val classHash: Felt,
)

@Serializable
data class GetClassAtPayload(
    @SerialName("block_id")
    val blockId: String,

    @SerialName("contract_address")
    val contractAddress: Felt,
)
