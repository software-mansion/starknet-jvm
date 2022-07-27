package starknet.extensions

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import starknet.data.types.Felt

fun JsonObjectBuilder.putFeltAsHex(key: String, value: Felt?): JsonElement? =
    put(key, JsonPrimitive(value?.hexString() ?: Felt.ZERO.hexString()))