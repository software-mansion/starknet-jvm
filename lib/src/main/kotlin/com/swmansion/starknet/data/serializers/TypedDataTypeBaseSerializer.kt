package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.TypedData
import com.swmansion.starknet.data.TypedData.TypeBase
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.*

internal object TypedDataTypeBaseSerializer : JsonContentPolymorphicSerializer<TypeBase>(TypeBase::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out TypeBase> {
        val jsonElement = element.jsonObject

        val type = jsonElement["type"]?.jsonPrimitive?.content
        return when (type) {
            "merkletree" -> TypedData.MerkleTreeType.serializer()
            is String -> TypedData.Type.serializer()
            else -> throw IllegalArgumentException("Input element does not contain mandatory field 'type'")
        }
    }
}
