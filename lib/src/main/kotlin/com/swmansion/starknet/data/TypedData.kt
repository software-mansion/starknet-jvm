package com.swmansion.starknet.data

import com.swmansion.starknet.crypto.Poseidon
import com.swmansion.starknet.crypto.StarknetCurve
import com.swmansion.starknet.data.serializers.TypedDataTypeBaseSerializer
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.MerkleTree
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

/**
 * TypedData revision.
 *
 * The revision of the specification to be used.
 *
 * [V0] - Legacy revision, represents the de facto spec before [SNIP-12](https://github.com/starknet-io/SNIPs/blob/main/SNIPS/snip-12.md) was published.
 * [V1] - Initial and current revision, represents the spec after [SNIP-12](https://github.com/starknet-io/SNIPs/blob/main/SNIPS/snip-12.md) was published.
 */
@Serializable
enum class TypedDataRevision(val value: Felt) {
    @SerialName("0")
    V0(Felt.ZERO),

    @SerialName("1")
    V1(Felt.ONE),
}

/**
 * Sign message for off-chain usage. Follows standard proposed [here](https://github.com/starknet-io/SNIPs/blob/main/SNIPS/snip-12.md).
 *
 * ```java
 * String typedDataString = """
 * {
 *    "types": {
 *        "StarkNetDomain": [
 *            {"name": "name", "type": "string"},
 *            {"name": "version", "type": "felt"},
 *            {"name": "chainId", "type": "felt"}
 *        ],
 *        "Airdrop": [
 *            {"name": "address", "type": "felt"},
 *            {"name": "amount", "type": "felt"}
 *        ],
 *        "Validate": [
 *            {"name": "id", "type": "felt"},
 *            {"name": "from", "type": "felt"},
 *            {"name": "amount", "type": "felt"},
 *            {"name": "nameGamer", "type": "string"},
 *            {"name": "endDate", "type": "felt"},
 *            {"name": "itemsAuthorized", "type": "felt*"},
 *            {"name": "chkFunction", "type": "selector"},
 *            {"name": "rootList", "type": "merkletree", "contains": "Airdrop"}
 *        ]
 *    },
 *    "primaryType": "Validate",
 *    "domain": {
 *        "name": "myDapp",
 *        "version": "1",
 *        "chainId": "SN_GOERLI"
 *    },
 *    "message": {
 *        "id": "0x0000004f000f",
 *        "from": "0x2c94f628d125cd0e86eaefea735ba24c262b9a441728f63e5776661829a4066",
 *        "amount": "400",
 *        "nameGamer": "Hector26",
 *        "endDate": "0x27d32a3033df4277caa9e9396100b7ca8c66a4ef8ea5f6765b91a7c17f0109c",
 *        "itemsAuthorized": ["0x01", "0x03", "0x0a", "0x0e"],
 *        "chkFunction": "check_authorization",
 *        "rootList": [
 *            {
 *                "address": "0x69b49c2cc8b16e80e86bfc5b0614a59aa8c9b601569c7b80dde04d3f3151b79",
 *                "amount": "1554785"
 *            },
 *            {
 *                "address": "0x7447084f620ba316a42c72ca5b8eefb3fe9a05ca5fe6430c65a69ecc4349b3b",
 *                "amount": "2578248"
 *            },
 *            {
 *                "address": "0x3cad9a072d3cf29729ab2fad2e08972b8cfde01d4979083fb6d15e8e66f8ab1",
 *                "amount": "4732581"
 *            },
 *            {
 *                "address": "0x7f14339f5d364946ae5e27eccbf60757a5c496bf45baf35ddf2ad30b583541a",
 *                "amount": "913548"
 *            }
 *        ]
 *    }
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
    val domain: Domain,
    val message: JsonObject,
) {
    private val revision = domain.revision ?: TypedDataRevision.V0

    private val domainSeparatorName = when (revision) {
        TypedDataRevision.V0 -> "StarkNetDomain"
        TypedDataRevision.V1 -> "StarknetDomain"
    }
    init {
        verifyTypes()
    }

    private fun verifyTypes() {
        val reservedTypes = when (revision) {
            TypedDataRevision.V0 -> reservedTypesV0
            TypedDataRevision.V1 -> reservedTypesV1
        }
        reservedTypes.forEach { require(it !in types) { "Types must not contain $it." } }

        require(domainSeparatorName in types) { "Types must contain $domainSeparatorName." }

        val referencedTypes = types.values.flatten().flatMap {
            when (it) {
                is EnumType -> extractEnumTypes(it.type) + it.contains
                is MerkleTreeType -> listOf(it.contains)
                is Type -> listOf(stripPointer(it.type))
            }
        }.distinct() + domainSeparatorName + primaryType

        types.keys.forEach {
            require(it.isNotEmpty()) { "Types cannot be empty." }
            require(!it.endsWith("*")) { "Types cannot end in *. $it was found." }
            require(!it.startsWith("(") || !it.endsWith(")")) { "Types cannot be enclosed in parenthesis. $it was found." }
            require(!it.contains(",")) { "Types cannot contain commas. $it was found." }
            require(it in referencedTypes) { "Dangling types are not allowed. Unreferenced type $it was found." }
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
        domain = Json.decodeFromString(domain),
        message = Json.parseToJsonElement(message).jsonObject,
    )

    @Serializable
    data class Domain(
        val name: JsonPrimitive,
        val version: JsonPrimitive,
        val chainId: JsonPrimitive,
        val revision: TypedDataRevision? = null,
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
    ) : TypeBase() {
        init {
            require(!contains.endsWith("*")) {
                "Merkletree 'contains' field cannot be an array, got '$contains' in type '$name'."
            }
        }
    }

    @Serializable
    data class EnumType(
        override val name: String,
        override val type: String = "enum",
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

            require(merkleType is MerkleTreeType) { "Key '$key' in parent '$parent' is not a merkletree." }

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
                val root = MerkleTree(structHashes).rootHash
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

    private fun extractEnumTypes(value: String): List<String> {
        val enumPattern = Regex("""\(\s*([^,]+)\s*\)""")
        val matches = enumPattern.findAll(value)
        return matches.map { it.groupValues[1].trim() }.toList()
    }

    fun getStructHash(typeName: String, data: String): Felt {
        val encodedData = encodeData(typeName, Json.parseToJsonElement(data).jsonObject)

        return StarknetCurve.pedersenOnElements(getTypeHash(typeName), *encodedData.toTypedArray())
    }

    fun getMessageHash(accountAddress: Felt): Felt {
        return StarknetCurve.pedersenOnElements(
            Felt.fromShortString("StarkNet Message"),
            getStructHash("StarkNetDomain", Json.encodeToJsonElement(domain).jsonObject),
            accountAddress,
            getStructHash(primaryType, message),
        )
    }

    companion object {
        private val reservedTypesV0 by lazy { listOf("felt", "bool", "string", "selector", "merkletree", "raw") }

        private val reservedTypesV1 by lazy { reservedTypesV0 + listOf("enum", "bool", "u128", "ContractAddress", "ClassHash", "timestamp", "shortstring") + listOf("u256", "NftId", "TokenAmount") }

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
