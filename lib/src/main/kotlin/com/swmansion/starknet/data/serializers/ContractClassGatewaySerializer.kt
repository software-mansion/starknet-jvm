package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.ContractClass
import com.swmansion.starknet.data.types.ContractClassBase
import com.swmansion.starknet.data.types.DeprecatedContractClass
import com.swmansion.starknet.extensions.base64Gzipped
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject


internal object GatewayContractClassPolymorphicSerializer : JsonContentPolymorphicSerializer<ContractClassBase>(ContractClassBase::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out ContractClassBase> {
        return if ("sierra_program" in element.jsonObject) (ContractClass.serializer()) else ContractClassGatewaySerializer
    }
}

internal object ContractClassGatewaySerializer : KSerializer<DeprecatedContractClass> {
    @Serializable
    data class ContractClassGateway(
        val program: JsonElement,

        @SerialName("entry_points_by_type")
        val entryPointsByType: DeprecatedContractClass.EntryPointsByType,
    )

    override fun deserialize(decoder: Decoder): DeprecatedContractClass {
        val response = ContractClassGateway.serializer().deserialize(decoder)
        val programString = response.program.toString()
        val program = programString.base64Gzipped()
        // FIXME: It doesn't produce the same output as the rpc endpoint

        return DeprecatedContractClass(program, response.entryPointsByType, null)
    }

    override val descriptor: SerialDescriptor
        get() = ContractClass.serializer().descriptor

    override fun serialize(encoder: Encoder, value: DeprecatedContractClass) {
        throw SerializationException("Class used for deserialization only.")
    }
}
