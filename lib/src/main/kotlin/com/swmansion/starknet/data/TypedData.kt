package com.swmansion.starknet.data

import com.swmansion.starknet.crypto.StarknetCurve
import com.swmansion.starknet.data.serializers.TypedDataTypeBaseSerializer
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.MerkleTree
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

/**
 * Sign message for off-chain usage. Follows standard proposed [here](https://github.com/argentlabs/argent-x/discussions/14).
 *
 * ```java
 * String typedDataString = """
 * {
 *     "types": {
 *         "StarkNetDomain": [
 *             {"name": "name", "type": "felt"},
 *             {"name": "version", "type": "felt"},
 *             {"name": "chainId", "type": "felt"},
 *         ],
 *         "Person": [
 *             {"name": "name", "type": "felt"},
 *             {"name": "wallet", "type": "felt"},
 *         ],
 *         "Mail": [
 *             {"name": "from", "type": "Person"},
 *             {"name": "to", "type": "Person"},
 *             {"name": "contents", "type": "felt"},
 *         ],
 *     },
 *     "primaryType": "Mail",
 *     "domain": {"name": "StarkNet Mail", "version": "1", "chainId": 1},
 *     "message": {
 *         "from": {
 *             "name": "Cow",
 *             "wallet": "0xCD2a3d9F938E13CD947Ec05AbC7FE734Df8DD826",
 *         },
 *         "to": {
 *             "name": "Bob",
 *             "wallet": "0xbBbBBBBbbBBBbbbBbbBbbbbBBbBbbbbBbBbbBBbB",
 *         },
 *         "contents": "Hello, Bob!",
 *     },
 * }
 * """;
 *
 * // Create a TypedData instance from string
 * TypedData typedData = TypedData.fromJsonString(typedDataString);
 *
 * // Get a message hash
 * Felt messageHash = typedData.getMessageHash(accountAddress);
 * ```
 */
@Suppress("DataClassPrivateConstructor")
@Serializable
data class TypedData private constructor(
    val types: Map<String, List<TypeBase>>,
    val primaryType: String,
    val domain: JsonObject,
    val message: JsonObject,
) {
    init {
        val reservedTypeNames = listOf("felt", "felt*", "string", "string*", "selector", "selector*", "merkletree", "merkletree*", "raw", "raw*")
        reservedTypeNames.forEach {
            require(!types.containsKey(it)) { "Types must not contain $it." }
        }
    }

    constructor(
        types: Map<String, List<TypeBase>>,
        primaryType: String,
        domain: String,
        message: String,
    ) : this(
        types = types,
        primaryType = primaryType,
        domain = Json.parseToJsonElement(domain).jsonObject,
        message = Json.parseToJsonElement(message).jsonObject,
    )

    @Serializable(with = TypedDataTypeBaseSerializer::class)
    sealed class TypeBase {
        abstract val name: String
        abstract val type: String
    }

    @Serializable
    data class Type(
        override val name: String,
        override val type: String,
    ) : TypeBase()

    @Serializable
    data class MerkleTreeType(
        override val name: String,
        override val type: String = "merkletree",
        val contains: String,
    ) : TypeBase()

    data class Context(
        val parent: String?,
        val key: String?,
    )

    private fun getDependencies(typeName: String): List<String> {
        val deps = mutableListOf(typeName)
        val toVisit = mutableListOf(typeName)

        while (toVisit.isNotEmpty()) {
            val type = toVisit.removeFirst()
            val params = types[type] ?: emptyList()

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

        val sorted = deps.subList(1, deps.size).sorted()
        val newDeps = listOf(deps[0]) + sorted

        return newDeps.joinToString("", transform = ::encodeDependency)
    }

    private fun encodeDependency(dependency: String): String {
        val fields =
            types[dependency] ?: throw IllegalArgumentException("Dependency [$dependency] is not defined in types.")
        val encodedFields = fields.joinToString(",") { "${it.name}:${it.type}" }
        return "$dependency($encodedFields)"
    }

    private fun feltFromPrimitive(primitive: JsonPrimitive): Felt {
        when (primitive.isString) {
            true -> {
                if (primitive.content == "") {
                    return Felt.ZERO
                }

                val decimal = primitive.content.toBigIntegerOrNull()
                decimal?.let {
                    return Felt(it)
                }

                return try {
                    Felt.fromHex(primitive.content)
                } catch (e: Exception) {
                    Felt.fromShortString(primitive.content)
                }
            }
            false -> {
                return Felt(primitive.long)
            }
        }
    }

    private fun prepareSelector(name: String): Felt {
        return try {
            Felt.fromHex(name)
        } catch (e: Exception) {
            selectorFromName(name)
        }
    }

    private fun getMerkleTreeType(context: Context): String {
        val (parent, key) = context.parent to context.key

        return if (parent != null && key != null) {
            val parentType = types.getOrElse(parent) { throw IllegalArgumentException("Parent '$parent' is not defined in types.") }
            val merkleType = parentType.find { it.name == key }
                ?: throw IllegalArgumentException("Key '$key' is not defined in parent '$parent'.")
            if (merkleType !is MerkleTreeType) {
                throw IllegalArgumentException("Key '$key' in parent '$parent' is not a merkletree.")
            }
            if (merkleType.contains.endsWith("*")) {
                throw IllegalArgumentException("Merkletree 'contains' field cannot be an array, got '${merkleType.contains}'.")
            }
            merkleType.contains
        } else {
            "raw"
        }
    }

    internal fun encodeValue(
        typeName: String,
        value: JsonElement,
        context: Context = Context(null, null),
    ): Pair<String, Felt> {
        if (types.containsKey(typeName)) {
            return typeName to getStructHash(typeName, value as JsonObject)
        }

        if (types.containsKey(stripPointer(typeName))) {
            val array = value as JsonArray
            val hashes = array.map { struct -> getStructHash(stripPointer(typeName), struct as JsonObject) }
            val hash = StarknetCurve.pedersenOnElements(hashes)

            return typeName to hash
        }

        return when (typeName) {
            "felt*" -> {
                val array = value as JsonArray
                val feltArray = array.map { feltFromPrimitive(it.jsonPrimitive) }
                val hash = StarknetCurve.pedersenOnElements(feltArray)
                typeName to hash
            }
            "felt" -> "felt" to feltFromPrimitive(value.jsonPrimitive)
            "string" -> "string" to feltFromPrimitive(value.jsonPrimitive)
            "raw" -> "raw" to feltFromPrimitive(value.jsonPrimitive)
            "selector" -> "felt" to prepareSelector(value.jsonPrimitive.content)
            "merkletree" -> {
                val merkleTreeType = getMerkleTreeType(context)
                val array = value as JsonArray
                val structHashes = array.map { struct -> encodeValue(merkleTreeType, struct).second }
                val root = MerkleTree(structHashes).root
                "merkletree" to root
            }
            else -> throw IllegalArgumentException("Type [$typeName] is not defined in types.")
        }
    }

    private fun encodeData(typeName: String, data: JsonObject): List<Felt> {
        val values = mutableListOf<Felt>()

        for (param in types.getValue(typeName)) {
            val encodedValue = encodeValue(
                typeName = param.type,
                value = data.getValue(param.name),
                Context(typeName, param.name),
            )
            values.add(encodedValue.second)
        }

        return values
    }

    fun getTypeHash(typeName: String): Felt {
        return selectorFromName(encodeType(typeName))
    }

    private fun getStructHash(typeName: String, data: JsonObject): Felt {
        val encodedData = encodeData(typeName, data)

        return StarknetCurve.pedersenOnElements(getTypeHash(typeName), *encodedData.toTypedArray())
    }

    private fun stripPointer(value: String): String {
        return value.removeSuffix("*")
    }

    fun getStructHash(typeName: String, data: String): Felt {
        val encodedData = encodeData(typeName, Json.parseToJsonElement(data).jsonObject)

        return StarknetCurve.pedersenOnElements(getTypeHash(typeName), *encodedData.toTypedArray())
    }

    fun getMessageHash(accountAddress: Felt): Felt {
        return StarknetCurve.pedersenOnElements(
            Felt.fromShortString("StarkNet Message"),
            getStructHash("StarkNetDomain", domain),
            accountAddress,
            getStructHash(primaryType, message),
        )
    }

    companion object {
        /**
         * Create TypedData from JSON string.
         *
         * @param typedData json string of typed data
         */
        @JvmStatic
        fun fromJsonString(typedData: String): TypedData =
            Json.decodeFromString(serializer(), typedData)
    }
}
