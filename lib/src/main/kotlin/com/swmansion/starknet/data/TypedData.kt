package com.swmansion.starknet.data

import com.swmansion.starknet.crypto.StarknetCurve
import com.swmansion.starknet.crypto.StarknetCurveSignature
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.signer.StarkCurveSigner
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
sealed class TypedDataChainId {
    @Serializable
    data class ChainIdString(val chainId: String): TypedDataChainId()

    @Serializable
    data class ChainIdNumber(val chainId: Int): TypedDataChainId()
}

data class StarkNetDomain(
    val name: String?,
    val version: String?,
    val chainId: TypedDataChainId
) {
    constructor(name: String?, version: String?, chainId: String): this(name, version, TypedDataChainId.ChainIdString(chainId))

    constructor(name: String?, version: String?, chainId: Int): this(name, version, TypedDataChainId.ChainIdNumber(chainId))
}

//@Serializable
//abstract class AbstractStarkNetType {
//    abstract val name: String
//    abstract val type: String
//}

@Serializable
data class StarkNetType(val name: String, val type: String)

//@Serializable
//data class StarkNetMerkleType(
//    override val name: String,
//    val contains: String
//) : AbstractStarkNetType() {
//    override val type = "merkletree"
//}

typealias TypedDataTypes = Map<String, List<StarkNetType>>

data class TypedData(
    val types: TypedDataTypes,
    val primaryType: String,
    val domain: StarkNetDomain,
    val message: JsonObject
)

fun getDependencies(types: TypedDataTypes, typeName: String): List<String> {
    val deps = mutableListOf<String>(typeName)
    val toVisit = mutableListOf(typeName)

    while (!toVisit.isEmpty()) {
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

fun encodeType(types: TypedDataTypes, type: String): String {
    val deps = getDependencies(types, type)

    val sorted = deps.subList(1, deps.size).sorted().toTypedArray()
    val newDeps = listOf(deps[0], *sorted)

    val result = newDeps.map { dependency ->
        "${dependency}(${types[dependency]?.map { "${it.name}:${it.type}" }?.joinToString(",")})"
    }.joinToString("")

    return result
}

fun getTypeHash(types: TypedDataTypes, type: String): Felt {
    return selectorFromName(encodeType(types, type))
}

fun encodeValue(types: TypedDataTypes, typeName: String, struct: JsonObject): Pair<String, Felt> {
    assert(types.containsKey(typeName))

    return typeName to getStructHash(types, typeName, struct)
}

fun encodeValue(types: TypedDataTypes, typeName: String, structArray: JsonArray): Pair<String, Felt> {
    assert(isPointer(typeName))
    assert(types.containsKey(stripPointer(typeName)))

    val hashes = structArray.map { struct -> getStructHash(types, stripPointer(typeName), struct as JsonObject) }
    val hash = StarknetCurve.pedersenOnElements(hashes)

    return typeName to hash
}

fun encodeValue(types: TypedDataTypes, typeName: String, feltArray: List<Felt>): Pair<String, Felt> {
    assert(typeName == "felt*")

    val hash = StarknetCurve.pedersenOnElements(feltArray)

    return typeName to hash
}

fun encodeValue(types: TypedDataTypes, typeName: String, value: JsonElement): Pair<String, Felt> {
    if (types.containsKey(typeName)) {
        return encodeValue(types, typeName, value as JsonObject)
    }

    if (types.containsKey(stripPointer(typeName))) {
        return encodeValue(types, typeName, value as JsonArray)
    }

    if (typeName == "felt*") {
        return encodeValue(types, typeName, value as List<Felt>)
    }
}

fun encodeData(types: TypedDataTypes, typeName: String, data: JsonObject): Pair<List<String>, List<Felt>> {
    val test  = types[typeName]?.map { field ->
        if (data[field.name] == null) {
            throw Error("Missing data for ${field.name}")
        }

        val value = data.getValue(field.name)

        if (isPointer(field.type)) {

        }
    }
}

fun getStructHash(types: TypedDataTypes, typeName: String, struct: JsonObject): Felt {
    return Felt.ZERO
}

//fun getStructHash(types: TypedDataTypes, typeName: String, data: JsonObject) {
//    StarknetCurve.pedersenOnElements(encode)
//}

//fun encodeValue(typeName: String, value: Int): Int {
//
//}

fun isPointer(value: String): Boolean {
    return value.endsWith("*")
}

fun stripPointer(value: String): String {
    return value.removeSuffix("*")
}