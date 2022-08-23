package starknet.provider.handlers

import com.swmansion.starknet.provider.exceptions.GatewayRequestFailedException
import com.swmansion.starknet.provider.exceptions.RequestFailedException
import com.swmansion.starknet.service.http.handlers.BasicHttpErrorHandler
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BasicHttpErrorHandlerTest {
    @Test
    fun `handler falls back to basic exception on unknown format`() {
        val handler = BasicHttpErrorHandler()
        val message = "{\"status_code\": 500, \"status_message\": \"error\"}"

        val exception = assertThrows(RequestFailedException::class.java) {
            handler.handle(message)
        }
        assertFalse(exception is GatewayRequestFailedException)
        assertEquals(message, exception.message)
    }

    @Test
    fun `handler parses gateway error`() {
        val handler = BasicHttpErrorHandler()
        val message = "{\"status_code\": 500, \"message\": \"error\"}"

        val exception = assertThrows(GatewayRequestFailedException::class.java) {
            handler.handle(message)
        }
        assertEquals("error", exception.message)
    }
}
