package starknet.data

import com.swmansion.starknet.data.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Test

val mockedTypedData = TypedData(
    mapOf(
        "StarkNetDomain" to listOf(
            StarkNetType("name", "felt"),
            StarkNetType("version", "felt"),
            StarkNetType("chainId", "felt"),
        ),
        "Person" to listOf(
            StarkNetType("name", "felt"),
            StarkNetType("wallet", "felt"),
        ),
        "Mail" to listOf(
            StarkNetType("from", "Person"),
            StarkNetType("to", "Person"),
            StarkNetType("contents", "felt"),
        ),
    ),
    "Mail",
    StarkNetDomain(
        "StarkNet Mail",
        "1",
        1,
    ),
    buildJsonObject {
        put(
            "from",
            buildJsonObject {
                put("name", JsonPrimitive("Cow"))
                put("wallet", JsonPrimitive("0xCD2a3d9F938E13CD947Ec05AbC7FE734Df8DD826"))
            },
        )
        put(
            "to",
            buildJsonObject {
                put("name", JsonPrimitive("Bob"))
                put("wallet", JsonPrimitive("0xbBbBBBBbbBBBbbbBbbBbbbbBBbBbbbbBbBbbBBbB"))
            },
        )
        put("contents", JsonPrimitive("Hello Bob!"))
    },
)

val mockedTypedDataStruct = TypedData(
    mapOf(
        "StarkNetDomain" to listOf(
            StarkNetType("name", "felt"),
            StarkNetType("version", "felt"),
            StarkNetType("chainId", "felt"),
        ),
        "Person" to listOf(
            StarkNetType("name", "felt"),
            StarkNetType("wallet", "felt"),
        ),
        "Post" to listOf(
            StarkNetType("title", "felt"),
            StarkNetType("content", "felt"),
        ),
        "Mail" to listOf(
            StarkNetType("from", "Person"),
            StarkNetType("to", "Person"),
            StarkNetType("posts_len", "felt"),
            StarkNetType("posts", "Post*"),
        ),
    ),
    "Mail",
    StarkNetDomain(
        "StarkNet Mail",
        "1",
        1,
    ),
    buildJsonObject {
        put(
            "from",
            buildJsonObject {
                put("name", JsonPrimitive("Cow"))
                put("wallet", JsonPrimitive("0xCD2a3d9F938E13CD947Ec05AbC7FE734Df8DD826"))
            },
        )
        put(
            "to",
            buildJsonObject {
                put("name", JsonPrimitive("Bob"))
                put("wallet", JsonPrimitive("0xbBbBBBBbbBBBbbbBbbBbbbbBBbBbbbbBbBbbBBbB"))
            },
        )
        put("posts_len", JsonPrimitive(2))
        put(
            "posts",
            JsonArray(
                listOf(
                    buildJsonObject {
                        put("title", JsonPrimitive("Greeting"))
                        put("content", JsonPrimitive("Hello, Bob!"))
                    },
                    buildJsonObject {
                        put("title", JsonPrimitive("Farewell"))
                        put("content", JsonPrimitive("Goodbye, Bob!"))
                    },
                ),
            ),
        )
    },
)

class TypedDataTest {

    @Test
    fun `type encoding`() {
//        val encoding = mockedTypedData.encodeType(mockedTypedData.types, "Mail")
//
//        assertEquals("Mail(from:Person,to:Person,contents:felt)Person(name:felt,wallet:felt)", encoding)
    }

    @Test
    fun `type encoding - struct`() {
//        val encoding = encodeType(mockedTypedDataStruct.types, "Mail")
//
//        assertEquals("Mail(from:Person,to:Person,posts_len:felt,posts:Post*)Person(name:felt,wallet:felt)Post(title:felt,content:felt)", encoding)
    }

    @Test
    fun `type hash calculation`() {
//        val mailTypeHash = getTypeHash(mockedTypedData.types, "Mail")
//        assertEquals(Felt.fromHex("0x13d89452df9512bf750f539ba3001b945576243288137ddb6c788457d4b2f79"), mailTypeHash)
//
//        val personTypeHash = getTypeHash(mockedTypedData.types, "Person")
//        assertEquals(Felt.fromHex("0x2896dbe4b96a67110f454c01e5336edc5bbc3635537efd690f122f4809cc855"), personTypeHash)
//
//        val domainTypeHash = getTypeHash(mockedTypedData.types, "StarkNetDomain")
//        assertEquals(Felt.fromHex("0x1bfc207425a47a5dfa1a50a4f5241203f50624ca5fdf5e18755765416b8e288"), domainTypeHash)
    }

    @Test
    fun `type hash calculation - struct`() {
//        val postTypeHash = getTypeHash(mockedTypedDataStruct.types, "Post")
//        assertEquals(Felt.fromHex("0x1d71e69bf476486b43cdcfaf5a85c00bb2d954c042b281040e513080388356d"), postTypeHash)
//
//        val mailWithStructArrayTypeHash = getTypeHash(mockedTypedDataStruct.types, "Mail")
//        assertEquals(Felt.fromHex("0x873b878e35e258fc99e3085d5aaad3a81a0c821f189c08b30def2cde55ff27"), mailWithStructArrayTypeHash)
//
//        val selectorTypeHash = getTypeHash(emptyMap(), "selector")
//        assertEquals(Felt.fromHex("0x1d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470"), selectorTypeHash)
    }

    @Test
    fun `selector from name`() {
//        println(encodeType(mockedTypedData.types, "Mail"))
//        println(selectorFromName(encodeType(mockedTypedData.types, "Mail")))
//        println(selectorFromName("\"Mail(from:Person,to:Person,contents:felt)Person(name:felt,wallet:felt)\""))
//        println(selectorFromName("Mail(from:Person,to:Person,contents:felt)Person(name:felt,wallet:felt)"))
//        val test = "test_short_message"
//
//        print(selectorFromName(test))
//
//        val test2 = "test_short_message_test_short_message_test_short_message_test_short_message_test_short_message_test_short_message_test_short_message_"
//
//        print(selectorFromName(test2))
    }
}
