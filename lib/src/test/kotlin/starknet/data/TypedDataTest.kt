package starknet.data

import com.swmansion.starknet.data.TypedData
import com.swmansion.starknet.data.TypedData.Context
import com.swmansion.starknet.data.TypedData.MerkleTreeType
import com.swmansion.starknet.data.TypedData.Revision
import com.swmansion.starknet.data.selectorFromName
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.MerkleTree
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import java.io.File

internal fun loadTypedData(path: String): TypedData {
    val content = File("src/test/resources/typed_data/$path").readText()

    return TypedData.fromJsonString(content)
}

internal class TypedDataTest {

    companion object {
        internal class CasesRev0 {
            companion object {
                val TD by lazy { loadTypedData("rev_0/typed_data_example.json") }
                val TD_FELT_ARR by lazy { loadTypedData("rev_0/typed_data_felt_array_example.json") }
                val TD_STRING by lazy { loadTypedData("rev_0/typed_data_long_string_example.json") }
                val TD_STRUCT_ARR by lazy { loadTypedData("rev_0/typed_data_struct_array_example.json") }
                val TD_SESSION by lazy { loadTypedData("rev_0/typed_data_session_example.json") }
                val TD_VALIDATE by lazy { loadTypedData("rev_0/typed_data_validate_example.json") }
            }
        }

        internal class CasesRev1 {
            companion object {
                val TD_BASIC_TYPES by lazy { loadTypedData("rev_1/typed_data_basic_types_example.json") }
                val TD_PRESET_TYPES by lazy { loadTypedData("rev_1/typed_data_preset_types_example.json") }
                val TD_ENUM_TYPE by lazy { loadTypedData("rev_1/typed_data_enum_example.json") }
                val TD by lazy { loadTypedData("rev_1/typed_data_example.json") }
                val TD_STRUCT_ARR by lazy { loadTypedData("rev_1/typed_data_struct_array_example.json") }
            }
        }

        @JvmStatic
        fun encodeTypeArguments() = listOf(
            Arguments.of(CasesRev0.TD, "Mail", "Mail(from:Person,to:Person,contents:felt)Person(name:felt,wallet:felt)"),
            Arguments.of(
                CasesRev0.TD_STRUCT_ARR,
                "Mail",
                "Mail(from:Person,to:Person,posts_len:felt,posts:Post*)Person(name:felt,wallet:felt)Post(title:felt,content:felt)",
            ),
            Arguments.of(
                CasesRev1.TD,
                "Mail",
                """
                    "Mail"("from":"Person","to":"Person","contents":"felt")"Person"("name":"felt","wallet":"felt")
                """.trimIndent(),
            ),
            Arguments.of(
                CasesRev1.TD_STRUCT_ARR,
                "Mail",
                """
                    "Mail"("from":"Person","to":"Person","posts_len":"felt","posts":"Post*")"Person"("name":"felt","wallet":"felt")"Post"("title":"felt","content":"felt")
                """.trimIndent(),
            ),
            Arguments.of(
                CasesRev1.TD_BASIC_TYPES,
                "Example",
                """
                    "Example"("n0":"felt","n1":"bool","n2":"string","n3":"selector","n4":"u128","n5":"i128","n6":"ContractAddress","n7":"ClassHash","n8":"timestamp","n9":"shortstring")
                """.trimIndent(),
            ),
            Arguments.of(
                CasesRev1.TD_PRESET_TYPES,
                "Example",
                """
                    "Example"("n0":"TokenAmount","n1":"NftId")"NftId"("collection_address":"ContractAddress","token_id":"u256")"TokenAmount"("token_address":"ContractAddress","amount":"u256")"u256"("low":"u128","high":"u128")
                """.trimIndent(),
            ),
            Arguments.of(
                CasesRev1.TD_ENUM_TYPE,
                "Example",
                """
                    "Example"("someEnum":"MyEnum")"MyEnum"("Variant 1":(),"Variant 2":("u128","u128*"),"Variant 3":("u128"))
                """.trimIndent(),
            ),
        )

        @JvmStatic
        fun getTypeHashArguments() = listOf(
            Arguments.of(CasesRev0.TD, "StarkNetDomain", "0x1bfc207425a47a5dfa1a50a4f5241203f50624ca5fdf5e18755765416b8e288"),
            Arguments.of(CasesRev0.TD, "Person", "0x2896dbe4b96a67110f454c01e5336edc5bbc3635537efd690f122f4809cc855"),
            Arguments.of(CasesRev0.TD, "Mail", "0x13d89452df9512bf750f539ba3001b945576243288137ddb6c788457d4b2f79"),
            Arguments.of(CasesRev0.TD_STRING, "String", "0x1933fe9de7e181d64298eecb44fc43b4cec344faa26968646761b7278df4ae2"),
            Arguments.of(CasesRev0.TD_STRING, "Mail", "0x1ac6f84a5d41cee97febb378ddabbe1390d4e8036df8f89dee194e613411b09"),
            Arguments.of(CasesRev0.TD_FELT_ARR, "Mail", "0x5b03497592c0d1fe2f3667b63099761714a895c7df96ec90a85d17bfc7a7a0"),
            Arguments.of(CasesRev0.TD_STRUCT_ARR, "Post", "0x1d71e69bf476486b43cdcfaf5a85c00bb2d954c042b281040e513080388356d"),
            Arguments.of(CasesRev0.TD_STRUCT_ARR, "Mail", "0x873b878e35e258fc99e3085d5aaad3a81a0c821f189c08b30def2cde55ff27"),
            Arguments.of(CasesRev0.TD_SESSION, "Session", "0x1aa0e1c56b45cf06a54534fa1707c54e520b842feb21d03b7deddb6f1e340c"),
            Arguments.of(CasesRev0.TD_SESSION, "Policy", "0x2f0026e78543f036f33e26a8f5891b88c58dc1e20cbbfaf0bb53274da6fa568"),
            Arguments.of(CasesRev0.TD_VALIDATE, "Validate", "0x1fc17ee4903c000b1c8c6c1424136d4efc4759d1e83915e981b18bc1074a72d"),
            Arguments.of(CasesRev0.TD_VALIDATE, "Airdrop", "0x37dcb14df3270824843bbbf50c72a724bcb303179dfcce56b653262cbb6957c"),
            Arguments.of(CasesRev1.TD_BASIC_TYPES, "Example", "0x1f94cd0be8b4097a41486170fdf09a4cd23aefbc74bb2344718562994c2c111"),
            // TODO: verify whether the hash is correct
            Arguments.of(CasesRev1.TD_PRESET_TYPES, "Example", "0x1a25a8bb84b761090b1fadaebe762c4b679b0d8883d2bedda695ea340839a55"),
            Arguments.of(CasesRev1.TD_ENUM_TYPE, "Example", "0x380a54d417fb58913b904675d94a8a62e2abc3467f4b5439de0fd65fafdd1a8"),
        )

        @JvmStatic
        fun getStructHashArguments() = listOf(
            Arguments.of(
                CasesRev0.TD,
                "StarkNetDomain",
                "domain",
                "0x54833b121883a3e3aebff48ec08a962f5742e5f7b973469c1f8f4f55d470b07",
            ),
            Arguments.of(CasesRev0.TD, "Mail", "message", "0x4758f1ed5e7503120c228cbcaba626f61514559e9ef5ed653b0b885e0f38aec"),
            Arguments.of(
                CasesRev0.TD_STRING,
                "Mail",
                "message",
                "0x1d16b9b96f7cb7a55950b26cc8e01daa465f78938c47a09d5a066ca58f9936f",
            ),
            Arguments.of(
                CasesRev0.TD_FELT_ARR,
                "Mail",
                "message",
                "0x26186b02dddb59bf12114f771971b818f48fad83c373534abebaaa39b63a7ce",
            ),
            Arguments.of(
                CasesRev0.TD_STRUCT_ARR,
                "Mail",
                "message",
                "0x5650ec45a42c4776a182159b9d33118a46860a6e6639bb8166ff71f3c41eaef",
            ),
            Arguments.of(
                CasesRev0.TD_SESSION,
                "Session",
                "message",
                "0x73602062421caf6ad2e942253debfad4584bff58930981364dcd378021defe8",
            ),
            Arguments.of(
                CasesRev0.TD_VALIDATE,
                "Validate",
                "message",
                "0x389e55e4a3d36c6ba04f46f1021a695c934d6782eaf64e47ac059a06a2520c2",
            ),
        )

        @JvmStatic
        fun getMessageHashArguments() = listOf(
            Arguments.of(
                CasesRev0.TD,
                "0xcd2a3d9f938e13cd947ec05abc7fe734df8dd826",
                "0x6fcff244f63e38b9d88b9e3378d44757710d1b244282b435cb472053c8d78d0",
            ),
            Arguments.of(
                CasesRev0.TD_STRING,
                "0xcd2a3d9f938e13cd947ec05abc7fe734df8dd826",
                "0x691b977ee0ee645647336f01d724274731f544ad0d626b078033d2541ee641d",
            ),
            Arguments.of(
                CasesRev0.TD_FELT_ARR,
                "0xcd2a3d9f938e13cd947ec05abc7fe734df8dd826",
                "0x30ab43ef724b08c3b0a9bbe425e47c6173470be75d1d4c55fd5bf9309896bce",
            ),
            Arguments.of(
                CasesRev0.TD_STRUCT_ARR,
                "0xcd2a3d9f938e13cd947ec05abc7fe734df8dd826",
                "0x5914ed2764eca2e6a41eb037feefd3d2e33d9af6225a9e7fe31ac943ff712c",
            ),
            Arguments.of(
                CasesRev0.TD_SESSION,
                "0xcd2a3d9f938e13cd947ec05abc7fe734df8dd826",
                "0x5d28fa1b31f92e63022f7d85271606e52bed89c046c925f16b09e644dc99794",
            ),
            Arguments.of(
                CasesRev0.TD_VALIDATE,
                "0xcd2a3d9f938e13cd947ec05abc7fe734df8dd826",
                "0x6038f35de58f40a6afa9d359859b2f930e5eb987580ba6875324cc4dbfcee",
            ),
        )
    }

    @ParameterizedTest
    @MethodSource("encodeTypeArguments")
    fun `encode type`(data: TypedData, typeName: String, expectedResult: String) {
        val encodedType = data.encodeType(typeName)

        assertEquals(expectedResult, encodedType)
    }

    @Test
    fun `selector type`() {
        val selector = "transfer"
        val selectorHash = selectorFromName(selector)

        val rawSelectorValueHash = CasesRev0.TD_SESSION.encodeValue(
            typeName = "felt",
            value = Json.encodeToJsonElement(selectorHash),
        )
        val selectorValueHash = CasesRev0.TD_SESSION.encodeValue(
            typeName = "selector",
            value = Json.encodeToJsonElement(selector),
        )

        assertEquals(rawSelectorValueHash, selectorValueHash)
        assertEquals(
            "felt" to Felt.fromHex("0x83afd3f4caedc6eebf44246fe54e38c95e3179a5ec9ea81740eca5b482d12e"),
            selectorValueHash,
        )
    }

    @Nested
    inner class MerkletreeTest {
        @Test
        fun `merkletree type`() {
            val tree = MerkleTree(
                listOf(
                    Felt(1),
                    Felt(2),
                    Felt(3),
                ),
            )
            val leaves = tree.leafHashes

            val merkleTreeHash = CasesRev0.TD_SESSION.encodeValue(
                typeName = "merkletree",
                value = Json.encodeToJsonElement(leaves),
            ).second

            assertEquals(tree.rootHash, merkleTreeHash)
            assertEquals(Felt.fromHex("0x15ac9e457789ef0c56e5d559809e7336a909c14ee2511503fa7af69be1ba639"), merkleTreeHash)
        }

        @Test
        fun `merkletree with custom types`() {
            val leaves = listOf(
                mapOf("contractAddress" to "0x1", "selector" to "transfer"),
                mapOf("contractAddress" to "0x2", "selector" to "transfer"),
                mapOf("contractAddress" to "0x3", "selector" to "transfer"),
            )

            val hashedLeaves = leaves.map { leaf ->
                CasesRev0.TD_SESSION.encodeValue(
                    typeName = "Policy",
                    value = Json.encodeToJsonElement(leaf),
                ).second
            }
            val tree = MerkleTree(hashedLeaves)

            val merkleTreeHash = CasesRev0.TD_SESSION.encodeValue(
                typeName = "merkletree",
                value = Json.encodeToJsonElement(leaves),
                context = Context(parent = "Session", key = "root"),
            ).second

            assertEquals(tree.rootHash, merkleTreeHash)
            assertEquals(
                Felt.fromHex("0x12354b159e3799dc0ebe86d62dde4ce7b300538d471e5a7fef23dcbac076011"),
                merkleTreeHash,
            )
        }

        @Test
        fun `merkletree from empty leaves`() {
            assertThrows<IllegalArgumentException>("Cannot build Merkle tree from an empty list of leaves.") {
                CasesRev0.TD_SESSION.encodeValue(
                    typeName = "merkletree",
                    value = Json.encodeToJsonElement(emptyList<Felt>()),
                    context = Context(parent = "Session", key = "root"),
                )
            }
        }

        @Test
        fun `merkletree with invalid contains`() {
            assertThrows<IllegalArgumentException>("Merkletree 'contains' field cannot be an array, got 'felt*' in type 'root'.") {
                MerkleTreeType(
                    name = "root",
                    type = "merkletree",
                    contains = "felt*",
                )
            }
        }

        @Test
        fun `merkletree with invalid context`() {
            val leaves = listOf(
                mapOf("contractAddress" to "0x1", "selector" to "transfer"),
                mapOf("contractAddress" to "0x2", "selector" to "transfer"),
                mapOf("contractAddress" to "0x3", "selector" to "transfer"),
            )

            val invalidParentContext = Context(parent = "UndefinedParent", key = "root")
            val invalidKeyContext = Context(parent = "Session", key = "undefinedKey")

            assertThrows<IllegalArgumentException>("Parent type '${invalidParentContext.parent}' is not defined in types.") {
                CasesRev0.TD_SESSION.encodeValue("merkletree", Json.encodeToJsonElement(leaves), invalidParentContext)
            }
            assertThrows<IllegalArgumentException>("Key '${invalidKeyContext.key}' is not defined in type '${invalidKeyContext.parent}'.") {
                CasesRev0.TD_SESSION.encodeValue("merkletree", Json.encodeToJsonElement(leaves), invalidKeyContext)
            }
        }
    }

    @Nested
    inner class InvalidTypesTest {
        private val domainTypeV0 = "StarkNetDomain" to listOf(
            TypedData.StandardType("name", "felt"),
            TypedData.StandardType("version", "felt"),
            TypedData.StandardType("chainId", "felt"),
        )
        private val domainTypeV1 = "StarknetDomain" to listOf(
            TypedData.StandardType("name", "shortstring"),
            TypedData.StandardType("version", "shortstring"),
            TypedData.StandardType("chainId", "shortstring"),
            TypedData.StandardType("revision", "shortstring"),
        )
        private val domainObjectV0 = """
            {
                "name": "DomainV0",
                "version": 1,
                "chainId": 2137
            }
        """.trimIndent()
        private val domainObjectV1 = """
            {
                "name": "DomainV1",
                "version": "1",
                "chainId": "2137",
                "revision": "1"
            }
        """.trimIndent()

        private val basicTypesV0 = setOf("felt", "bool", "string", "selector", "merkletree")
        private val basicTypesV1 = basicTypesV0 + setOf("enum", "u128", "i128", "ContractAddress", "ClassHash", "timestamp", "shortstring")
        private val presetTypesV0 = emptySet<String>()
        private val presetTypesV1 = setOf("u256", "TokenAmount", "NftId")

        @ParameterizedTest
        @EnumSource(Revision::class)
        fun `basic types redefinition`(revision: Revision) {
            val types = when (revision) {
                Revision.V0 -> basicTypesV0
                Revision.V1 -> basicTypesV1
            }

            types.forEach { type ->
                val exception = assertThrows<IllegalArgumentException> {
                    makeTypedData(revision, type)
                }
                assertEquals("Types must not contain basic types. [$type] was found.", exception.message)
            }
        }

        @ParameterizedTest
        @EnumSource(Revision::class)
        fun `preset types redefinition`(revision: Revision) {
            val types = when (revision) {
                Revision.V0 -> presetTypesV0
                Revision.V1 -> presetTypesV1
            }

            types.forEach { type ->
                val exception = assertThrows<IllegalArgumentException> {
                    makeTypedData(revision, type)
                }
                assertEquals("Types must not contain preset types. [$type] was found.", exception.message)
            }
        }

        @Test
        fun `type with asterisk`() {
            val types = listOf("felt*", "u256*", "mytype*")
            types.forEach { type ->
                val exception = assertThrows<IllegalArgumentException> {
                    makeTypedData(Revision.V1, type)
                }
                assertEquals("Types cannot end in *. [$type] was found.", exception.message)
            }
        }

        @Test
        fun `type with parentheses`() {
            val types = listOf("(left", "right)", "(both)")
            types.forEach { type ->
                val exception = assertThrows<IllegalArgumentException> {
                    makeTypedData(Revision.V1, type)
                }
                assertEquals("Types cannot be enclosed in parentheses. [$type] was found.", exception.message)
            }
        }

        @Test
        fun `type with commas`() {
            val types = listOf(",mytype", "my,type", "mytype,")
            types.forEach { type ->
                val exception = assertThrows<IllegalArgumentException> {
                    makeTypedData(Revision.V1, type)
                }
                assertEquals("Types cannot contain commas. [$type] was found.", exception.message)
            }
        }

        @Test
        fun `dangling types`() {
            val exception = assertThrows<IllegalArgumentException> {
                TypedData(
                    customTypes = mapOf(
                        domainTypeV1,
                        "dangling" to emptyList(),
                        "mytype" to emptyList(),
                    ),
                    primaryType = "mytype",
                    domain = domainObjectV1,
                    message = "{\"mytype\": 1}",
                )
            }
            assertEquals("Dangling types are not allowed. Unreferenced type [dangling] was found.", exception.message)
        }

        @Test
        fun `missing dependency`() {
            val td = TypedData(
                customTypes = mapOf(
                    domainTypeV1,
                    "house" to listOf(TypedData.StandardType("fridge", "ice cream")),
                ),
                primaryType = "house",
                domain = domainObjectV1,
                message = "{\"fridge\": 1}",
            )
            val exception = assertThrows<IllegalArgumentException> {
                td.getStructHash("house", "{\"fridge\": 1}")
            }
            assertEquals("Type [ice cream] is not defined in types.", exception.message)
        }

        private fun makeTypedData(
            revision: Revision,
            includedType: String,
        ) {
            val (domainType, domainObject) = when (revision) {
                Revision.V0 -> domainTypeV0 to domainObjectV0
                Revision.V1 -> domainTypeV1 to domainObjectV1
            }

            TypedData(
                customTypes = mapOf(
                    domainType,
                    includedType to emptyList(),
                ),
                primaryType = includedType,
                domain = domainObject,
                message = "{\"$includedType\": 1}",
            )
        }
    }

    @ParameterizedTest
    @MethodSource("getTypeHashArguments")
    fun `type hash calculation`(data: TypedData, typeName: String, expectedResult: String) {
        val hash = data.getTypeHash(typeName)

        assertEquals(Felt.fromHex(expectedResult), hash)
    }

    @ParameterizedTest
    @MethodSource("getStructHashArguments")
    fun `struct hash calculation`(data: TypedData, typeName: String, dataSource: String, expectedResult: String) {
        val dataStruct = if (dataSource == "domain") {
            Json.encodeToString(data.domain)
        } else {
            Json.encodeToString(data.message)
        }

        val hash = data.getStructHash(typeName, dataStruct)

        assertEquals(Felt.fromHex(expectedResult), hash)
    }

    @ParameterizedTest
    @MethodSource("getMessageHashArguments")
    fun `message hash calculation`(data: TypedData, address: String, expectedResult: String) {
        val hash = data.getMessageHash(Felt.fromHex(address))

        assertEquals(Felt.fromHex(expectedResult), hash)
    }
}
