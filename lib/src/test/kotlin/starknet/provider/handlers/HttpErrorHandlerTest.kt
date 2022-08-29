package starknet.provider.handlers

import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.StarknetChainId
import com.swmansion.starknet.provider.exceptions.GatewayRequestFailedException
import com.swmansion.starknet.provider.exceptions.RequestFailedException
import com.swmansion.starknet.provider.exceptions.RpcRequestFailedException
import com.swmansion.starknet.provider.gateway.GatewayProvider
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
    fun `gateway handler falls back to basic exception on unknown format`() {
        val message = "{\"status_code\": 500, \"status_message\": \"error\"}"
        val httpServiceMock = mock<HttpService> {
            on { send(any()) } doReturn HttpResponse(false, 500, message)
        }
        val provider = GatewayProvider("", "", StarknetChainId.TESTNET, httpServiceMock)
        val request = provider.getTransaction(Felt(1))

        val exception = assertThrows(RequestFailedException::class.java) {
            request.send()
        }
        assertFalse(exception is GatewayRequestFailedException)
        assertEquals(message, exception.message)
    }

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
        assertEquals(message, exception.message)
    }

    @Test
    fun `gateway handler parses gateway error`() {
        val message = "{\"status_code\": 500, \"message\": \"error\"}"
        val httpServiceMock = mock<HttpService> {
            on { send(any()) } doReturn HttpResponse(false, 500, message)
        }
        val provider = GatewayProvider("", "", StarknetChainId.TESTNET, httpServiceMock)
        val request = provider.getTransaction(Felt(1))

        val exception = assertThrows(GatewayRequestFailedException::class.java) {
            request.send()
        }
        assertEquals("error", exception.message)
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
}
