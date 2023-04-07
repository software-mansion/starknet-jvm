package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.ContractClass
import com.swmansion.starknet.data.types.ContractClassBase
import com.swmansion.starknet.data.types.DeprecatedContractClass
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

internal object JsonRpcContractClassPolymorphicSerializer : JsonContentPolymorphicSerializer<ContractClassBase>(ContractClassBase::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out ContractClassBase> {
        return if ("sierra_program" in element.jsonObject) (ContractClass.serializer()) else DeprecatedContractClass.serializer()
    }
}
