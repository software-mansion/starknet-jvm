package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.provider.rpc.ContractExecutionError
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

internal object ContractExecutionErrorPolymorphicSerializer : JsonContentPolymorphicSerializer<ContractExecutionError>(
    ContractExecutionError::class,
) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<ContractExecutionError> {
        // TODO: When "revert_error" is string, element is JsonLiteral (which is an internal type) instead of JsonPrimitive.
        // This is problematic, because we cannot add branch for JsonLiteral in when statement.
        // Currently it is handled by else branch.
        return when (element) {
            is JsonObject -> ContractExecutionError.InnerCall.serializer()
            else -> ContractExecutionErrorMessageSerializer
        }
    }
}
