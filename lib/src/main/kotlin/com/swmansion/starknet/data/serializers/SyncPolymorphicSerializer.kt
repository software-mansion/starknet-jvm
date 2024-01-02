package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.NotSyncingResponse
import com.swmansion.starknet.data.types.Syncing
import com.swmansion.starknet.data.types.SyncingResponse
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.*

internal object SyncPolymorphicSerializer : JsonContentPolymorphicSerializer<Syncing>(Syncing::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<Syncing> = when (element) {
        JsonPrimitive(false) -> NotSyncingTransformingSerializer
        else -> SyncingResponse.serializer()
    }
}

internal object NotSyncingTransformingSerializer :
    JsonTransformingSerializer<NotSyncingResponse>(NotSyncingResponse.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement = buildJsonObject { put("status", element) }
}
