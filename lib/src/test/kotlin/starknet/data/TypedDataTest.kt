package starknet.data

import com.swmansion.starknet.data.TypedData
import com.swmansion.starknet.data.types.Felt
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File

fun loadTypedData(name: String): TypedData {
    val content = File("src/test/resources/typed-data/$name").readText()

    return TypedData.fromJsonString(content)
}

class TypedDataTest {

    companion object {

        private val TD = loadTypedData("typed_data_example.json")
        private val TD_FELT_ARR = loadTypedData("typed_data_felt_array_example.json")
        private val TD_STRING = loadTypedData("typed_data_long_string_example.json")
        private val TD_STRUCT_ARR = loadTypedData("typed_data_struct_array_example.json")

        @JvmStatic
        fun getTypeHashArguments() = listOf(
            Arguments.of(TD, "StarkNetDomain", "0x1bfc207425a47a5dfa1a50a4f5241203f50624ca5fdf5e18755765416b8e288"),
            Arguments.of(TD, "Person", "0x2896dbe4b96a67110f454c01e5336edc5bbc3635537efd690f122f4809cc855"),
            Arguments.of(TD, "Mail", "0x13d89452df9512bf750f539ba3001b945576243288137ddb6c788457d4b2f79"),
            Arguments.of(TD_STRING, "String", "0x1933fe9de7e181d64298eecb44fc43b4cec344faa26968646761b7278df4ae2"),
            Arguments.of(TD_STRING, "Mail", "0x1ac6f84a5d41cee97febb378ddabbe1390d4e8036df8f89dee194e613411b09"),
            Arguments.of(TD_FELT_ARR, "Mail", "0x5b03497592c0d1fe2f3667b63099761714a895c7df96ec90a85d17bfc7a7a0"),
            Arguments.of(TD_STRUCT_ARR, "Post", "0x1d71e69bf476486b43cdcfaf5a85c00bb2d954c042b281040e513080388356d"),
            Arguments.of(TD_STRUCT_ARR, "Mail", "0x873b878e35e258fc99e3085d5aaad3a81a0c821f189c08b30def2cde55ff27"),
        )

        @JvmStatic
        fun getStructHashArguments() = listOf(
            Arguments.of(
                TD,
                "StarkNetDomain",
                "domain",
                "0x54833b121883a3e3aebff48ec08a962f5742e5f7b973469c1f8f4f55d470b07",
            ),
            Arguments.of(TD, "Mail", "message", "0x4758f1ed5e7503120c228cbcaba626f61514559e9ef5ed653b0b885e0f38aec"),
            Arguments.of(
                TD_STRING,
                "Mail",
                "message",
                "0x1d16b9b96f7cb7a55950b26cc8e01daa465f78938c47a09d5a066ca58f9936f",
            ),
            Arguments.of(
                TD_FELT_ARR,
                "Mail",
                "message",
                "0x26186b02dddb59bf12114f771971b818f48fad83c373534abebaaa39b63a7ce",
            ),
            Arguments.of(
                TD_STRUCT_ARR,
                "Mail",
                "message",
                "0x5650ec45a42c4776a182159b9d33118a46860a6e6639bb8166ff71f3c41eaef",
            ),
        )

        @JvmStatic
        fun getMessageHashArguments() = listOf(
            Arguments.of(
                TD,
                "0xcd2a3d9f938e13cd947ec05abc7fe734df8dd826",
                "0x6fcff244f63e38b9d88b9e3378d44757710d1b244282b435cb472053c8d78d0",
            ),
            Arguments.of(
                TD_STRING,
                "0xcd2a3d9f938e13cd947ec05abc7fe734df8dd826",
                "0x691b977ee0ee645647336f01d724274731f544ad0d626b078033d2541ee641d",
            ),
            Arguments.of(
                TD_FELT_ARR,
                "0xcd2a3d9f938e13cd947ec05abc7fe734df8dd826",
                "0x30ab43ef724b08c3b0a9bbe425e47c6173470be75d1d4c55fd5bf9309896bce",
            ),
            Arguments.of(
                TD_STRUCT_ARR,
                "0xcd2a3d9f938e13cd947ec05abc7fe734df8dd826",
                "0x5914ed2764eca2e6a41eb037feefd3d2e33d9af6225a9e7fe31ac943ff712c",
            ),
        )
    }

    @Test
    fun `invalid types`() {
        assertThrows<IllegalArgumentException>(
            "Types must not contain felt.",
        ) {
            TypedData(mapOf("felt" to emptyList()), "felt", "{}", "{\"felt\": 1}")
        }
        assertThrows<IllegalArgumentException>(
            "Types must not contain felt*.",
        ) {
            TypedData(mapOf("felt*" to emptyList()), "felt*", "{}", "{\"felt*\": 1}")
        }
    }

    @Test
    fun `missing dependency`() {
        assertThrows<IllegalArgumentException>(
            "Dependency [ice cream] is not defined in types.",
        ) {
            TypedData(
                mapOf(
                    "house" to listOf(TypedData.Type("fridge", "ice cream")),
                ),
                "felt",
                "{}",
                "{}",
            ).getStructHash("house", "{\"fridge\": 1}")
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
            data.domain
        } else {
            data.message
        }

        val hash = data.getStructHash(typeName, Json.encodeToString(dataStruct))

        assertEquals(Felt.fromHex(expectedResult), hash)
    }

    @ParameterizedTest
    @MethodSource("getMessageHashArguments")
    fun `message hash calculation`(data: TypedData, address: String, expectedResult: String) {
        val hash = data.getMessageHash(Felt.fromHex(address))

        assertEquals(Felt.fromHex(expectedResult), hash)
    }
}
