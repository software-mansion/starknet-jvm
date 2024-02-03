package starknet.provider.providers

import com.swmansion.starknet.data.types.BlockTag
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.MessageL1ToL2
import com.swmansion.starknet.data.types.PriceUnit
import com.swmansion.starknet.provider.exceptions.RequestFailedException
import com.swmansion.starknet.provider.exceptions.RpcRequestFailedException
import com.swmansion.starknet.provider.rpc.JsonRpcProvider
import com.swmansion.starknet.service.http.HttpResponse
import com.swmansion.starknet.service.http.HttpService
import kotlinx.serialization.SerializationException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class JsonRpcResponseTest {
    @Test
    fun `rpc provider parses response with unknown keys`() {
        val mockResponse = """
            {
                "id": 0,
                "jsonrpc": "2.0",
                "uknown_key": "value",
                "result": {
                    "unknown_primitive": "value",
                    "gas_consumed": "0x1234",
                    "gas_price": "0x5678",
                    "overall_fee": "0x9abc",
                    "unknown_object": {"key_1": "value_1", "key_2": "value_2"},
                    "unit": "FRI",
                    "unknown_sequence": ["0x1", "0x2"]
                }
            }
        """.trimIndent()
        val httpServiceMock = mock<HttpService> {
            on { send(any()) } doReturn HttpResponse(true, 200, mockResponse)
        }

        val provider = JsonRpcProvider(
            url = "",
            httpService = httpServiceMock,
            ignoreUnknownJsonKeys = true,
        )
        val message = MessageL1ToL2(Felt.ONE, Felt.ONE, Felt.ONE, Felt.ONE, listOf(Felt.ZERO, Felt.ONE))
        val request = provider.getEstimateMessageFee(message, BlockTag.PENDING)
        val response = request.send()

        assertEquals(Felt.fromHex("0x1234"), response.gasConsumed)
        assertEquals(Felt.fromHex("0x5678"), response.gasPrice)
        assertEquals(Felt.fromHex("0x9abc"), response.overallFee)
        assertEquals(PriceUnit.FRI, response.feeUnit)

        val provider2 = JsonRpcProvider(
            url = "",
            httpService = httpServiceMock,
            ignoreUnknownJsonKeys = false,
        )
        val request2 = provider2.getEstimateMessageFee(message, BlockTag.PENDING)
        assertThrows<SerializationException> {
            request2.send()
        }
    }
    
    @Test
    fun `rpc provider falls back to basic exception on unknown format`() {
        val message = "{\"status_code\": 500, \"status_message\": \"error\"}"
        val httpServiceMock = mock<HttpService> {
            on { send(any()) } doReturn HttpResponse(false, 500, message)
        }
        val provider = JsonRpcProvider("", httpServiceMock)
        val request = provider.getTransaction(Felt(1))

        val exception = assertThrows(RequestFailedException::class.java) {
            request.send()
        }
        assertFalse(exception is RpcRequestFailedException)
        assertEquals("Request failed", exception.message)
        assertEquals(message, exception.payload)
    }

    @Test
    fun `rpc provider parses rpc error`() {
        val message = """
            {
                "id": 0,
                "jsonrpc": "2.0",
                "error": {
                    "code": 21,
                    "message": "Invalid message selector"
                }
            }
        """.trimIndent()
        val httpServiceMock = mock<HttpService> {
            on { send(any()) } doReturn HttpResponse(true, 200, message)
        }
        val provider = JsonRpcProvider("", httpServiceMock)
        val request = provider.getTransaction(Felt(1))

        val exception = assertThrows(RpcRequestFailedException::class.java) {
            request.send()
        }
        assertEquals(21, exception.code)
        assertEquals("Invalid message selector", exception.message)
    }

    @Test
    fun `rpc provider parses rpc contract error`() {
        val message = """
            {
                "id": 0,
                "jsonrpc": "2.0",
                "error": {
                    "code": 40,
                    "message": "Contract error",
                    "data": {
                        "revert_error": "Example revert error"
                    }
                }
            }
        """.trimIndent()
        val httpServiceMock = mock<HttpService> {
            on { send(any()) } doReturn HttpResponse(true, 200, message)
        }
        val provider = JsonRpcProvider("", httpServiceMock)
        val request = provider.getTransaction(Felt(1))

        val exception = assertThrows(RpcRequestFailedException::class.java) {
            request.send()
        }
        assertEquals(40, exception.code)
        assertEquals("Contract error", exception.message)
        assertEquals("Example revert error", exception.data)
    }

    @Test
    fun `rpc provider parses rpc error with data object`() {
        val message = """
            {
                "id": 0,
                "jsonrpc": "2.0",
                "error": {
                    "code": -32603,
                    "message": "Internal error",
                    "data": {
                        "error": "Invalid message selector",
                        "details": {
                            "selector": "0x1234"
                        }
                    }
                }
            }
        """.trimIndent()
        val httpServiceMock = mock<HttpService> {
            on { send(any()) } doReturn HttpResponse(true, 200, message)
        }
        val provider = JsonRpcProvider("", httpServiceMock)
        val request = provider.getTransaction(Felt(1))

        val exception = assertThrows(RpcRequestFailedException::class.java) {
            request.send()
        }
        assertEquals(-32603, exception.code)
        assertEquals("Internal error", exception.message)
        assertEquals("{\"error\":\"Invalid message selector\",\"details\":{\"selector\":\"0x1234\"}}", exception.data)
    }

    @Test
    fun `rpc provider parses rpc error with data primive`() {
        val message = """
            {
                "id": 0,
                "jsonrpc": "2.0",
                "error": {
                    "code": -32603,
                    "message": "Internal error",
                    "data": "Invalid message selector"
                }
            }
        """.trimIndent()
        val httpServiceMock = mock<HttpService> {
            on { send(any()) } doReturn HttpResponse(true, 200, message)
        }
        val provider = JsonRpcProvider("", httpServiceMock)
        val request = provider.getTransaction(Felt(1))

        val exception = assertThrows(RpcRequestFailedException::class.java) {
            request.send()
        }
        assertEquals(-32603, exception.code)
        assertEquals("Internal error", exception.message)
        assertEquals("Invalid message selector", exception.data)
    }
}
