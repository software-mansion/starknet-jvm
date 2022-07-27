@file:JvmName("Contracts")

package starknet.data.types

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import starknet.data.toHex
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64
import java.util.zip.GZIPOutputStream

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

object ContractClassGatewaySerializer: KSerializer<ContractClass> {
    @Serializable
    data class ContractClassGateway(
        val program: JsonElement,
        @SerialName("entry_points_by_type") val entryPointsByType: ContractClass.EntryPointsByType
    )

    override fun deserialize(decoder: Decoder): ContractClass {
        val response = ContractClassGateway.serializer().deserialize(decoder)
        val bos = ByteArrayOutputStream()

        val programString = Json.encodeToString(response.program)

        GZIPOutputStream(bos).bufferedWriter(UTF_8).use { writer ->
            writer.write(programString)
        }

        val base64Encoder = Base64.getEncoder()
        val program = base64Encoder.encodeToString(bos.toByteArray())

        // FIXME: It doesn't produce the same output as the rpc endpoint

        return ContractClass(program, response.entryPointsByType)
    }

    override val descriptor: SerialDescriptor
        get() = ContractClass.serializer().descriptor

    override fun serialize(encoder: Encoder, value: ContractClass) {
        TODO("Not implemented. It is used only for deserialization.")
    }
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
