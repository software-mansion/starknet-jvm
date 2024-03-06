package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.TypedData
import com.swmansion.starknet.data.TypedData.TypeBase
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.*

internal object TypedDataTypeBaseSerializer : JsonContentPolymorphicSerializer<TypeBase>(TypeBase::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<TypeBase> {
        val type = element.jsonObject["type"]?.jsonPrimitive?.content

        return when (type) {
            "merkletree" -> TypedData.MerkleTreeType.serializer()
            "enum" -> when (element.jsonObject["contains"]?.jsonPrimitive?.content) {
                null -> TypedData.Type.serializer()
                else -> TypedData.EnumType.serializer()
            }
            is String -> TypedData.Type.serializer()
            null -> throw IllegalArgumentException("Input element does not contain mandatory field 'type'")
            else -> throw IllegalArgumentException("Unknown type '$type'")
        }
    }
}
