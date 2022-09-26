package com.swmansion.starknet.extensions

import com.swmansion.starknet.data.types.Felt
import kotlinx.serialization.json.*

@JvmSynthetic
internal fun JsonObjectBuilder.put(key: String, value: Felt?): JsonElement? =
    put(key, JsonPrimitive(value?.hexString() ?: Felt.ZERO.hexString()))

@JvmSynthetic
internal fun JsonArrayBuilder.add(value: Felt): Boolean = add(JsonPrimitive(value.hexString()))
