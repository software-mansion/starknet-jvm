package com.swmansion.starknet.data

import com.swmansion.starknet.crypto.StarknetCurve
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.extensions.encodeShortString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class StarkNetType(val name: String, val type: String)

typealias TypedDataTypes = Map<String, List<StarkNetType>>

data class TypedData(
    val types: TypedDataTypes,
    val primaryType: String,
    val domain: JsonObject,
    val message: JsonObject,
) {
    private fun getDependencies(typeName: String): List<String> {
        val deps = mutableListOf<String>(typeName)
        val toVisit = mutableListOf(typeName)

        while (toVisit.isNotEmpty()) {
            val type = toVisit.removeFirst()
            val params = types[type] ?: return emptyList()

            for (param in params) {
                val typeStripped = stripPointer(param.type)

                if (types.containsKey(typeStripped) && !deps.contains(typeStripped)) {
                    deps.add(typeStripped)
                    toVisit.add(typeStripped)
                }
            }
        }

        return deps
    }

    private fun encodeType(type: String): String {
        val deps = getDependencies(type)

        val sorted = deps.subList(1, deps.size).sorted().toTypedArray()
        val newDeps = listOf(deps[0], *sorted)

        val result = newDeps.joinToString("") { dependency ->
            "$dependency(${types[dependency]?.map { "${it.name}:${it.type}" }?.joinToString(",")})"
        }

        return result
    }

    private fun encodeValue(typeName: String, value: JsonElement): Pair<String, Felt> {
        if (types.containsKey(typeName)) {
            return typeName to getStructHash(typeName, value as JsonObject)
        }

        if (types.containsKey(stripPointer(typeName))) {
            val array = value as JsonArray
            val hashes = array.map { struct -> getStructHash(stripPointer(typeName), struct as JsonObject) }
            val hash = StarknetCurve.pedersenOnElements(hashes)

            return typeName to hash
        }

        if (typeName == "felt*") {
            val array = value as JsonArray
            val feltArray = array.map { Felt.fromHex(it.jsonPrimitive.content) }
            val hash = StarknetCurve.pedersenOnElements(feltArray)

            return typeName to hash
        }

        if (value.jsonPrimitive.isString) {
            return try {
                val feltValue = Felt.fromHex(value.jsonPrimitive.content)
                "felt" to feltValue
            } catch (e: Exception) {
                val encodedString = value.jsonPrimitive.content.encodeShortString()

                "felt" to encodedString
            }
        }

        return "felt" to Felt(value.jsonPrimitive.int)
    }

    private fun encodeData(typeName: String, data: JsonObject): List<Felt> {
        val values = mutableListOf<Felt>()

        for (param in types.getValue(typeName)) {
            val encodedValue = encodeValue(param.type, data.getValue(param.name))
            values.add(encodedValue.second)
        }

        return values
    }

    fun getTypeHash(typeName: String): Felt {
        return selectorFromName(encodeType(typeName))
    }

    fun getStructHash(typeName: String, data: JsonObject): Felt {
        val encodedData = encodeData(typeName, data)

        return StarknetCurve.pedersenOnElements(getTypeHash(typeName), *encodedData.toTypedArray())
    }

    fun getMessageHash(accountAddress: Felt): Felt {
        return StarknetCurve.pedersenOnElements(
            "StarkNet Message".encodeShortString(),
            getStructHash("StarkNetDomain", domain),
            accountAddress,
            getStructHash(primaryType, message),
        )
    }
}

private fun isPointer(value: String): Boolean {
    return value.endsWith("*")
}

private fun stripPointer(value: String): String {
    return value.removeSuffix("*")
}
