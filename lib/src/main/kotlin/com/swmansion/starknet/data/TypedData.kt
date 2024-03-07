package com.swmansion.starknet.data

import com.swmansion.starknet.crypto.HashMethod
import com.swmansion.starknet.crypto.StarknetCurve
import com.swmansion.starknet.data.serializers.TypedDataTypeBaseSerializer
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.MerkleTree
import com.swmansion.starknet.extensions.toFelt
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
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
    @SerialName("types")
    val customTypes: Map<String, List<Type>>,

    val primaryType: String,

    val domain: Domain,

    val message: JsonObject,
) {
    constructor(
        customTypes: Map<String, List<Type>>,
        primaryType: String,
        domain: String,
        message: String,
    ) : this(
        customTypes = customTypes,
        primaryType = primaryType,
        domain = Json.decodeFromString(domain),
        message = Json.parseToJsonElement(message).jsonObject,
    )

    private val revision = domain.revision ?: TypedDataRevision.V0

    @Transient
    val types: Map<String, List<Type>> = run {
        val presetTypes = when (revision) {
            TypedDataRevision.V0 -> presetTypesV0
            TypedDataRevision.V1 -> presetTypesV1
        }
        customTypes + presetTypes
    }

    private val hashMethod by lazy {
        when (revision) {
            TypedDataRevision.V0 -> HashMethod.PEDERSEN
            TypedDataRevision.V1 -> HashMethod.POSEIDON
        }
    }

    init {
        verifyTypes()
    }

    private fun hashArray(values: List<Felt>) = hashMethod.hash(values)

    private fun verifyTypes() {
        val reservedTypes = when (revision) {
            TypedDataRevision.V0 -> reservedTypesV0
            TypedDataRevision.V1 -> reservedTypesV1
        }
        reservedTypes.forEach { require(it !in customTypes) { "Types must not contain $it." } }

        require(domain.separatorName in customTypes) { "Types must contain ${domain.separatorName}." }

        val referencedTypes = customTypes.values.flatten().flatMap {
            when (it) {
                is EnumType -> extractEnumTypes(it.type) + it.contains
                is MerkleTreeType -> listOf(it.contains)
                is StandardType -> listOf(stripPointer(it.type))
            }
        }.distinct() + domain.separatorName + primaryType

        customTypes.keys.forEach {
            require(it.isNotEmpty()) { "Types cannot be empty." }
            require(!it.isArray()) { "Types cannot end in *. $it was found." }
            require(!it.startsWith("(") || !it.endsWith(")")) { "Types cannot be enclosed in parenthesis. $it was found." }
            require(!it.contains(",")) { "Types cannot contain commas. $it was found." }
            require(it in referencedTypes) { "Dangling types are not allowed. Unreferenced type $it was found." }
        }
    }

    @Serializable
    data class Domain(
        val name: JsonPrimitive,
        val version: JsonPrimitive,
        val chainId: JsonPrimitive,
        val revision: TypedDataRevision? = null,
    ) {
        val separatorName = when (revision ?: TypedDataRevision.V0) {
            TypedDataRevision.V0 -> "StarkNetDomain"
            TypedDataRevision.V1 -> "StarknetDomain"
        }
    }

    @Serializable(with = TypedDataTypeBaseSerializer::class)
    sealed class Type {
        abstract val name: String
        abstract val type: String
    }

    @Serializable
    data class StandardType(
        override val name: String,
        override val type: String,
    ) : Type()

    @Serializable
    data class MerkleTreeType(
        override val name: String,
        override val type: String = "merkletree",
        val contains: String,
    ) : Type() {
        init {
            require(!contains.isArray()) {
                "Merkletree 'contains' field cannot be an array, got '$contains' in type '$name'."
            }
        }
    }

    @Serializable
    data class EnumType(
        override val name: String,
        override val type: String = "enum",
        val contains: String,
    ) : Type()

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

            params.forEach { param ->
                val extractedTypes = when {
                    param is EnumType && revision == TypedDataRevision.V1 -> listOf(param.contains)
                    param.type.isEnum() && revision == TypedDataRevision.V1 -> extractEnumTypes(param.type)
                    else -> listOf(param.type)
                }.map { stripPointer(it) }

                extractedTypes.forEach {
                    if (it in types && it !in deps) {
                        deps.add(it)
                        toVisit.add(it)
                    }
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
        fun escape(typeName: String) = when (revision) {
            TypedDataRevision.V0 -> typeName
            TypedDataRevision.V1 -> "\"$typeName\""
        }

        val fields = types.getOrElse(dependency) {
            throw IllegalArgumentException("Dependency [$dependency] is not defined in types.")
        }
        val encodedFields = fields.joinToString(",") {
            val targetType = when {
                it is EnumType && revision == TypedDataRevision.V1 -> it.contains
                else -> it.type
            }
            val typeString = when {
                targetType.isEnum() -> extractEnumTypes(targetType).joinToString("", transform = ::escape)
                else -> escape(targetType)
            }
            "${it.name}:$typeString"
        }

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
        if (typeName in types) {
            return typeName to getStructHash(typeName, value.jsonObject)
        }

        if (typeName.isArray()) {
            val hashes = value.jsonArray.map {
                encodeValue(stripPointer(typeName), it).second
            }

            return typeName to hashArray(hashes)
        }

        return when (typeName) {
            "enum" -> {
                require(revision == TypedDataRevision.V1) { "'enum' basic type is not supported in revision ${revision.value}." }

                val (variantKey, variantData) = value.jsonObject.entries.single()
                val parent = context.parent ?: throw IllegalArgumentException("Parent is not defined for 'enum' type.")
                val parentType = types.getOrElse(parent) {
                    throw IllegalArgumentException("Parent '$parent' is not defined in types.")
                }.first()
                require(parentType is EnumType)
                val enumType = types.getOrElse(parentType.contains) { throw IllegalArgumentException("Type '${parentType.contains}' is not defined in types") }
                val variantType = enumType.find { it.name == variantKey }
                    ?: throw IllegalArgumentException("Key '$variantKey' is not defined in parent '$parent'.")

                val variantIndex = extractEnumTypes(variantType.type).indexOf(variantData.jsonPrimitive.content)

                val encodedSubtypes = extractEnumTypes(variantType.type)
                    .filter { it.isNotEmpty() }
                    .mapIndexed { index, subtype ->
                        val subtypeData = variantData.jsonArray[index]
                        encodeValue(subtype, subtypeData).second
                    }

                "enum" to hashArray(listOf(variantIndex.toFelt) + encodedSubtypes)
            }
            "felt" -> "felt" to feltFromPrimitive(value.jsonPrimitive)
            "string" -> "string" to feltFromPrimitive(value.jsonPrimitive)
            "raw" -> "raw" to feltFromPrimitive(value.jsonPrimitive)
            "selector" -> "felt" to prepareSelector(value.jsonPrimitive.content)
            "merkletree" -> {
                val merkleTreeType = getMerkleTreeType(context)
                val array = value as JsonArray
                val structHashes = array.map { struct -> encodeValue(merkleTreeType, struct).second }
                val root = MerkleTree(structHashes, hashMethod).rootHash
                "felt" to root
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

        return hashArray(listOf(getTypeHash(typeName)) + encodedData)
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

        return hashArray(listOf(getTypeHash(typeName)) + encodedData)
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

        private val reservedTypesV1 by lazy {
            reservedTypesV0 + listOf("enum", "bool", "u128", "ContractAddress", "ClassHash", "timestamp", "shortstring") + presetTypesV1.keys
        }

        private val presetTypesV0: Map<String, List<Type>> by lazy { emptyMap() }

        private val presetTypesV1: Map<String, List<Type>> by lazy {
            mapOf(
                "u256" to listOf(
                    StandardType("low", "u128"),
                    StandardType("high", "u128"),
                ),
                "TokenAmount" to listOf(
                    StandardType("token_address", "ContractAddress"),
                    StandardType("amount", "u256"),
                ),
                "NftId" to listOf(
                    StandardType("collection_address", "ContractAddress"),
                    StandardType("token_id", "u256"),
                ),
            )
        }

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

internal fun String.isArray() = endsWith("*")

internal fun String.isEnum() = startsWith("(") && endsWith(")")
