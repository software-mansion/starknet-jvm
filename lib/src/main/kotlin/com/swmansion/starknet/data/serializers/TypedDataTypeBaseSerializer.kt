package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.TypedData
import com.swmansion.starknet.data.TypedData.Type
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.*

internal object TypedDataTypeBaseSerializer : JsonContentPolymorphicSerializer<Type>(Type::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<Type> {
        val type = element.jsonObject["type"]?.jsonPrimitive?.content

        return when (type) {
            "merkletree" -> TypedData.MerkleTreeType.serializer()
            "enum" -> when (element.jsonObject["contains"]?.jsonPrimitive?.content) {
                null -> TypedData.StandardType.serializer()
                else -> TypedData.EnumType.serializer()
            }
            is String -> TypedData.StandardType.serializer()
            null -> throw IllegalArgumentException("Input element does not contain mandatory field 'type'")
            else -> throw IllegalArgumentException("Unknown type '$type'")
        }
    }
}
