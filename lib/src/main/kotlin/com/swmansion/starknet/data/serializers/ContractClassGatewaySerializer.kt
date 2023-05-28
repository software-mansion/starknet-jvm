package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.ContractClassBase
import com.swmansion.starknet.data.types.DeprecatedContractClass
import com.swmansion.starknet.data.types.GatewayContractClass
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
        return if ("sierra_program" in element.jsonObject) Cairo1ContractClassGatewaySerializer else Cairo0ContractClassGatewaySerializer
    }
}

internal object Cairo1ContractClassGatewaySerializer : KSerializer<GatewayContractClass> {
    @Serializable
    data class ContractClassGateway(
        @SerialName("sierra_program")
        val sierraProgram: JsonElement,

        @SerialName("entry_points_by_type")
        val entryPointsByType: GatewayContractClass.EntryPointsByType,

        @SerialName("contract_class_version")
        val contractClassVersion: JsonElement,

        val abi: JsonElement?,
    )

    override fun deserialize(decoder: Decoder): GatewayContractClass {
        val response = ContractClassGateway.serializer().deserialize(decoder)
        val sierraProgram = response.sierraProgram.toString().base64Gzipped()
        val contractClassVersion = response.contractClassVersion.toString()
        val abi = response.abi.toString()

        return GatewayContractClass(sierraProgram, response.entryPointsByType, contractClassVersion, abi)
    }

    override val descriptor: SerialDescriptor
        get() = GatewayContractClass.serializer().descriptor

    override fun serialize(encoder: Encoder, value: GatewayContractClass) {
        throw SerializationException("Class used for deserialization only.")
    }
}

internal object Cairo0ContractClassGatewaySerializer : KSerializer<DeprecatedContractClass> {
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
        get() = DeprecatedContractClass.serializer().descriptor

    override fun serialize(encoder: Encoder, value: DeprecatedContractClass) {
        throw SerializationException("Class used for deserialization only.")
    }
}
