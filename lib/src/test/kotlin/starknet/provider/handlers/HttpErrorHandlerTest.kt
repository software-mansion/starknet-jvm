package starknet.provider.handlers

import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.StarknetChainId
import com.swmansion.starknet.provider.exceptions.RequestFailedException
import com.swmansion.starknet.provider.exceptions.RpcRequestFailedException
import com.swmansion.starknet.provider.rpc.JsonRpcProvider
import com.swmansion.starknet.service.http.HttpResponse
import com.swmansion.starknet.service.http.HttpService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class HttpErrorHandlerTest {

    @Test
    fun `rpc handler falls back to basic exception on unknown format`() {
        val message = "{\"status_code\": 500, \"status_message\": \"error\"}"
        val httpServiceMock = mock<HttpService> {
            on { send(any()) } doReturn HttpResponse(false, 500, message)
        }
        val provider = JsonRpcProvider("", StarknetChainId.TESTNET, httpServiceMock)
        val request = provider.getTransaction(Felt(1))

        val exception = assertThrows(RequestFailedException::class.java) {
            request.send()
        }
        assertFalse(exception is RpcRequestFailedException)
        assertEquals("Request failed", exception.message)
        assertEquals(message, exception.payload)
    }

    @Test
    fun `rpc handler parses rpc error`() {
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
        val provider = JsonRpcProvider("", StarknetChainId.TESTNET, httpServiceMock)
        val request = provider.getTransaction(Felt(1))

        val exception = assertThrows(RpcRequestFailedException::class.java) {
            request.send()
        }
        assertEquals(21, exception.code)
        assertEquals("Invalid message selector", exception.message)
    }

    @Test
    fun `rpc handler parses rpc contract error`() {
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
        val provider = JsonRpcProvider("", StarknetChainId.TESTNET, httpServiceMock)
        val request = provider.getTransaction(Felt(1))

        val exception = assertThrows(RpcRequestFailedException::class.java) {
            request.send()
        }
        assertEquals(40, exception.code)
        assertEquals("Contract error", exception.message)
        assertEquals("Example revert error", exception.data)
    }

    @Test
    fun `rpc handler parses rpc error with data object`() {
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
        val provider = JsonRpcProvider("", StarknetChainId.TESTNET, httpServiceMock)
        val request = provider.getTransaction(Felt(1))

        val exception = assertThrows(RpcRequestFailedException::class.java) {
            request.send()
        }
        assertEquals(-32603, exception.code)
        assertEquals("Internal error", exception.message)
        assertEquals("{\"error\":\"Invalid message selector\",\"details\":{\"selector\":\"0x1234\"}}", exception.data)
    }

    @Test
    fun `rpc handler parses rpc error with data primive`() {
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
        val provider = JsonRpcProvider("", StarknetChainId.TESTNET, httpServiceMock)
        val request = provider.getTransaction(Felt(1))

        val exception = assertThrows(RpcRequestFailedException::class.java) {
            request.send()
        }
        assertEquals(-32603, exception.code)
        assertEquals("Internal error", exception.message)
        assertEquals("Invalid message selector", exception.data)
    }
}
