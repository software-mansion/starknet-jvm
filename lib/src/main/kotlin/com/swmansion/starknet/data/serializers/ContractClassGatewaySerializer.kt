package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.ContractClass
import com.swmansion.starknet.extensions.base64Gzipped
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement

internal object ContractClassGatewaySerializer : KSerializer<ContractClass> {
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

        return ContractClass(program, response.entryPointsByType, null)
    }

    override val descriptor: SerialDescriptor
        get() = ContractClass.serializer().descriptor

    override fun serialize(encoder: Encoder, value: ContractClass) {
        throw SerializationException("Class used for deserialization only.")
    }
}
