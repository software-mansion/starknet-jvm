package starknet.extensions

import kotlinx.serialization.json.*
import starknet.data.types.Felt

fun JsonObjectBuilder.put(key: String, value: Felt?): JsonElement? =
    put(key, JsonPrimitive(value?.hexString() ?: Felt.ZERO.hexString()))

fun JsonArrayBuilder.add(value: Felt): Boolean = add(JsonPrimitive(value.hexString()))
