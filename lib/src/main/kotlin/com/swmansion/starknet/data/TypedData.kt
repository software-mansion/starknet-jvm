package com.swmansion.starknet.data

import com.swmansion.starknet.crypto.HashMethod
import com.swmansion.starknet.data.serializers.TypedDataTypeBaseSerializer
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.MerkleTree
import com.swmansion.starknet.data.types.StarknetByteArray
import com.swmansion.starknet.extensions.toFelt
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.*

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

    @Transient
    private val revision = domain.revision ?: Revision.V0

    @Transient
    private val types: Map<String, List<Type>> = customTypes + PresetType.values(revision).associate { it.typeName to it.params }

    private val hashMethod by lazy {
        when (revision) {
            Revision.V0 -> HashMethod.PEDERSEN
            Revision.V1 -> HashMethod.POSEIDON
        }
    }

    init {
        verifyTypes()
    }

    private fun hashArray(values: List<Felt>) = hashMethod.hash(values)

    private fun verifyTypes() {
        require(domain.separatorName in customTypes) { "Types must contain '${domain.separatorName}'." }

        BasicType.values(revision).forEach { require(it.typeName !in customTypes) { "Types must not contain basic types. [${it.typeName}] was found." } }
        PresetType.values(revision).forEach { require(it.typeName !in customTypes) { "Types must not contain preset types. [${it.typeName}] was found." } }

        val referencedTypes = customTypes.values.flatten().flatMap {
            when (it) {
                is EnumType -> {
                    require(revision == Revision.V1) { "'enum' basic type is not supported in revision ${revision.value}." }
                    listOf(it.contains)
                }
                is MerkleTreeType -> listOf(it.contains)
                is StandardType -> when {
                    it.type.isEnum() -> {
                        require(revision == Revision.V1) { "Enum types are not supported in revision ${revision.value}." }
                        extractEnumTypes(it.type)
                    }
                    else -> listOf(stripPointer(it.type))
                }
            }
        }.distinct() + domain.separatorName + primaryType

        customTypes.keys.forEach {
            require(it.isNotEmpty()) { "Type names cannot be empty." }
            require(!it.isArray()) { "Type names cannot end in *. [$it] was found." }
            require(!it.startsWith("(") && !it.endsWith(")")) { "Type names cannot be enclosed in parentheses. [$it] was found." }
            require(!it.contains(",")) { "Type names cannot contain commas. [$it] was found." }
            require(it in referencedTypes) { "Dangling types are not allowed. Unreferenced type [$it] was found." }
        }
    }

    /**
     * TypedData revision.
     *
     * The revision of the specification to be used.
     *
     * [V0] - Legacy revision, represents the de facto spec before [SNIP-12](https://github.com/starknet-io/SNIPs/blob/main/SNIPS/snip-12.md) was published.
     * [V1] - Initial and current revision, represents the spec after [SNIP-12](https://github.com/starknet-io/SNIPs/blob/main/SNIPS/snip-12.md) was published.
     */
    @Serializable
    enum class Revision(val value: Int) {
        @SerialName("0")
        V0(0),

        @SerialName("1")
        V1(1),
    }

    @Serializable
    data class Domain(
        val name: JsonPrimitive,
        val version: JsonPrimitive,
        val chainId: JsonPrimitive,
        val revision: Revision? = null,
    ) {
        internal val separatorName = when (revision ?: Revision.V0) {
            Revision.V0 -> "StarkNetDomain"
            Revision.V1 -> "StarknetDomain"
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
                "Merkletree 'contains' field cannot be an array, got [$contains] in type [$name]."
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
        val parent: String,
        val key: String,
    )

    private fun getDependencies(typeName: String): List<String> {
        val deps = mutableListOf(typeName)
        val toVisit = mutableListOf(typeName)

        while (toVisit.isNotEmpty()) {
            val type = toVisit.removeFirst()
            val params = types[type] ?: emptyList()

            params.forEach { param ->
                val extractedTypes = when {
                    param is EnumType -> {
                        require(revision == Revision.V1) { "'enum' basic type is not supported in revision ${revision.value}." }
                        listOf(param.contains)
                    }
                    param.type.isEnum() -> {
                        require(revision == Revision.V1) { "Enum types are not supported in revision ${revision.value}." }
                        extractEnumTypes(param.type)
                    }
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

    internal fun encodeType(type: String): String {
        val deps = getDependencies(type)

        val sorted = deps.subList(1, deps.size).sorted()
        val newDeps = listOf(deps[0]) + sorted

        return newDeps.joinToString("", transform = ::encodeDependency)
    }

    private fun encodeDependency(dependency: String): String {
        fun escape(typeName: String) = when (revision) {
            Revision.V0 -> typeName
            Revision.V1 -> "\"$typeName\""
        }

        val fields = types.getOrElse(dependency) {
            throw IllegalArgumentException("Dependency [$dependency] is not defined in types.")
        }
        val encodedFields = fields.joinToString(",") {
            val targetType = when {
                it is EnumType && revision == Revision.V1 -> it.contains
                else -> it.type
            }
            val typeString = when {
                targetType.isEnum() -> {
                    require(revision == Revision.V1) { "Enum types are not supported in revision ${revision.value}." }

                    extractEnumTypes(targetType).joinToString(
                        separator = ",",
                        prefix = "(",
                        postfix = ")",
                        transform = ::escape,
                    )
                }
                else -> escape(targetType)
            }
            "${escape(it.name)}:$typeString"
        }

        return "${escape(dependency)}($encodedFields)"
    }

    private fun feltFromPrimitive(primitive: JsonPrimitive, allowSigned: Boolean = false): Felt {
        val decimal = primitive.content.toBigIntegerOrNull()
        decimal?.let {
            return if (allowSigned) Felt.fromSigned(it) else Felt(it)
        }
        primitive.booleanOrNull?.let {
            return if (it) Felt.ONE else Felt.ZERO
        }

        if (primitive.isString) {
            if (primitive.content == "") {
                return Felt.ZERO
            }

            return try {
                Felt.fromHex(primitive.content)
            } catch (e: Exception) {
                Felt.fromShortString(primitive.content)
            }
        }

        throw IllegalArgumentException("Unsupported primitive type: $primitive")
    }

    private fun prepareLongString(string: String): Felt {
        val byteArray = StarknetByteArray.fromString(string)

        return hashArray(byteArray.toCalldata())
    }

    private fun prepareSelector(name: String): Felt {
        return try {
            Felt.fromHex(name)
        } catch (e: Exception) {
            selectorFromName(name)
        }
    }

    private inline fun <reified T : Type> resolveType(context: Context): T {
        val (parent, key) = context.parent to context.key

        val parentType = types.getOrElse(parent) { throw IllegalArgumentException("Parent [$parent] is not defined in types.") }
        val targetType = parentType.singleOrNull { it.name == key }
            ?: throw IllegalArgumentException("Key [$key] is not defined in parent [$parent] or multiple definitions are present.")

        require(targetType is T) { "Key [$key] in parent [$parent] is not a '${T::class.simpleName}'." }

        return targetType
    }

    private fun getMerkleTreeLeavesType(context: Context): String {
        val merkleType = resolveType<MerkleTreeType>(context)

        return merkleType.contains
    }

    private fun prepareMerkletreeRoot(value: JsonArray, context: Context): Felt {
        val merkleTreeType = getMerkleTreeLeavesType(context)
        val structHashes = value.map { struct -> encodeValue(merkleTreeType, struct).second }

        return MerkleTree(structHashes, hashMethod).rootHash
    }

    private fun getEnumVariants(context: Context): List<Type> {
        val enumType = resolveType<EnumType>(context)

        val variants = types.getOrElse(enumType.contains) { throw IllegalArgumentException("Type [${enumType.contains}] is not defined in types") }

        return variants
    }

    private fun prepareEnum(value: JsonObject, context: Context): Felt {
        val (variantName, variantData) = value.entries.singleOrNull()?.let { it.key to it.value.jsonArray }
            ?: throw IllegalArgumentException("'enum' value must contain a single variant.")

        val variants = getEnumVariants(context)
        val variantType = variants.singleOrNull { it.name == variantName }
            ?: throw IllegalArgumentException("Variant [$variantName] is not defined in 'enum' type [${context.key}] or multiple definitions are present.")
        val variantIndex = variants.indexOf(variantType)

        val encodedSubtypes = extractEnumTypes(variantType.type).mapIndexed { index, subtype ->
            val subtypeData = variantData[index]
            encodeValue(subtype, subtypeData).second
        }

        return hashArray(listOf(variantIndex.toFelt) + encodedSubtypes)
    }

    internal fun encodeValue(
        typeName: String,
        value: JsonElement,
        context: Context? = null,
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

        val basicType = BasicType.fromName(typeName, revision)
            ?: throw IllegalArgumentException("Type [$typeName] is not defined in types.")

        return when (basicType) {
            BasicType.Felt -> "felt" to feltFromPrimitive(value.jsonPrimitive)
            BasicType.Bool -> "bool" to feltFromPrimitive(value.jsonPrimitive)
            BasicType.StringV0 -> "string" to feltFromPrimitive(value.jsonPrimitive)
            BasicType.StringV1 -> "string" to prepareLongString(value.jsonPrimitive.content)
            BasicType.Selector -> "felt" to prepareSelector(value.jsonPrimitive.content)
            BasicType.I128 -> "i128" to feltFromPrimitive(value.jsonPrimitive, allowSigned = true)
            BasicType.U128, BasicType.ContractAddress, BasicType.ClassHash, BasicType.Timestamp, BasicType.ShortString -> basicType.typeName to feltFromPrimitive(value.jsonPrimitive)
            BasicType.MerkleTree -> {
                requireNotNull(context) { "Context is not provided for 'merkletree' type." }
                "felt" to prepareMerkletreeRoot(value.jsonArray, context)
            }
            BasicType.Enum -> {
                requireNotNull(context) { "Context is not provided for 'enum' type." }
                "felt" to prepareEnum(value.jsonObject, context)
            }
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

    private fun stripPointer(type: String): String {
        return type.removeSuffix("*")
    }

    private fun extractEnumTypes(type: String): List<String> {
        require(type.isEnum()) { "Type [$type] is not an enum." }

        return type.substring(1, type.length - 1).let {
            if (it.isEmpty()) emptyList() else it.split(",")
        }
    }

    fun getStructHash(typeName: String, data: String): Felt {
        val encodedData = encodeData(typeName, Json.parseToJsonElement(data).jsonObject)

        return hashArray(listOf(getTypeHash(typeName)) + encodedData)
    }

    fun getMessageHash(accountAddress: Felt): Felt {
        return hashArray(
            listOf(
                Felt.fromShortString("StarkNet Message"),
                getStructHash(domain.separatorName, Json.encodeToJsonElement(domain).jsonObject),
                accountAddress,
                getStructHash(primaryType, message),
            ),
        )
    }

    companion object {
        private sealed class BasicType {
            abstract val typeName: String

            override fun toString() = typeName

            sealed interface V0
            sealed interface V1

            data object Felt : BasicType(), V0, V1 { override val typeName = "felt" }
            data object Bool : BasicType(), V0, V1 { override val typeName = "bool" }
            data object Selector : BasicType(), V0, V1 { override val typeName = "selector" }
            data object MerkleTree : BasicType(), V0, V1 { override val typeName = "merkletree" }
            data object StringV0 : BasicType(), V0 { override val typeName = "string" }
            data object StringV1 : BasicType(), V1 { override val typeName = "string" }
            data object Enum : BasicType(), V1 { override val typeName = "enum" }
            data object I128 : BasicType(), V1 { override val typeName = "i128" }
            data object U128 : BasicType(), V1 { override val typeName = "u128" }
            data object ContractAddress : BasicType(), V1 { override val typeName = "ContractAddress" }
            data object ClassHash : BasicType(), V1 { override val typeName = "ClassHash" }
            data object Timestamp : BasicType(), V1 { override val typeName = "timestamp" }
            data object ShortString : BasicType(), V1 { override val typeName = "shortstring" }

            companion object {
                fun values(revision: Revision): List<BasicType> {
                    val instances = BasicType::class.sealedSubclasses.mapNotNull { it.objectInstance }

                    return when (revision) {
                        Revision.V0 -> instances.filterIsInstance<V0>()
                        Revision.V1 -> instances.filterIsInstance<V1>()
                    }.map { it as BasicType }
                }

                fun fromName(typeName: String, revision: Revision): BasicType? {
                    return values(revision).find { it.typeName == typeName }
                }
            }
        }

        private sealed class PresetType {
            abstract val typeName: String
            abstract val params: List<Type>

            override fun toString() = typeName

            sealed interface V1

            data object U256 : PresetType(), V1 {
                override val typeName = "u256"
                override val params = listOf(
                    StandardType("low", "u128"),
                    StandardType("high", "u128"),
                )
            }
            data object TokenAmount : PresetType(), V1 {
                override val typeName = "TokenAmount"
                override val params = listOf(
                    StandardType("token_address", "ContractAddress"),
                    StandardType("amount", "u256"),
                )
            }
            data object NftId : PresetType(), V1 {
                override val typeName = "NftId"
                override val params = listOf(
                    StandardType("collection_address", "ContractAddress"),
                    StandardType("token_id", "u256"),
                )
            }
            companion object {
                fun values(revision: Revision): List<PresetType> {
                    return when (revision) {
                        Revision.V0 -> emptyList()
                        Revision.V1 -> PresetType::class.sealedSubclasses.mapNotNull { it.objectInstance }.filterIsInstance<V1>()
                    }.map { it as PresetType }
                }
            }
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
