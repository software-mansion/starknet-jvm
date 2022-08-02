package com.swmansion.starknet.extensions

import com.swmansion.starknet.data.types.Felt
import kotlinx.serialization.json.*

fun JsonObjectBuilder.put(key: String, value: Felt?): JsonElement? =
    put(key, JsonPrimitive(value?.hexString() ?: Felt.ZERO.hexString()))

fun JsonArrayBuilder.add(value: Felt): Boolean = add(JsonPrimitive(value.hexString()))
