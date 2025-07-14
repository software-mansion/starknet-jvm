package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

internal object RevertibleFunctionInvocationPolymorphicSerializer : JsonContentPolymorphicSerializer<RevertibleFunctionInvocation>(
    RevertibleFunctionInvocation::class,
) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<RevertibleFunctionInvocation> {
        val isRevertReason = element.jsonObject.contains("revert_reason")
        return when (isRevertReason) {
            true -> RevertReason.serializer()
            false -> FunctionInvocation.serializer()
        }
    }
}
