package starknet.provider.response

import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.data.types.transactions.TransactionExecutionStatus
import com.swmansion.starknet.data.types.transactions.TransactionStatus
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
import starknet.utils.DevnetClient

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
                    "data_gas_consumed": "0xabc",
                    "data_gas_price": "0x789",
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
        val message = MessageL1ToL2(Felt.ONE, Felt.ONE, Felt.ONE, Felt.ONE, listOf(Felt.ZERO, Felt.ONE))

        val provider = JsonRpcProvider("", httpServiceMock, ignoreUnknownJsonKeys = true)
        val request = provider.getEstimateMessageFee(message, BlockTag.PENDING)
        val response = request.send()

        assertEquals(Felt.fromHex("0x1234"), response.gasConsumed)
        assertEquals(Felt.fromHex("0x5678"), response.gasPrice)
        assertEquals(Felt.fromHex("0x9abc"), response.overallFee)
        assertEquals(PriceUnit.FRI, response.feeUnit)

        val provider2 = JsonRpcProvider("", httpServiceMock, ignoreUnknownJsonKeys = false)
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
    fun `rpc provider parses rpc error without data`() {
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
                            "selector": "0x1234",
                            "id": 789
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
        assertEquals("{\"error\":\"Invalid message selector\",\"details\":{\"selector\":\"0x1234\",\"id\":789}}", exception.data)
    }

    @Test
    fun `rpc provider parses rpc error with data primitive`() {
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

    @Test
    fun `rpc handler parses rpc error with data array`() {
        val message = """
            {
                "id": 0,
                "jsonrpc": "2.0",
                "error": {
                    "code": -32603,
                    "message": "Internal error",
                    "data": [
                        "Invalid message selector",
                        "0x1234"
                    ]
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
        assertEquals("[\"Invalid message selector\",\"0x1234\"]", exception.data)
    }

    @Test
    fun `rpc provider parses batch callContract`() {
        val mockResponse = """
           [
              {
                "jsonrpc": "2.0",
                "result": [
                  "0x1e2e22799540",
                  "0x0"
                ],
                "id": "0"
              },
              {
                "jsonrpc": "2.0",
                "result": [
                  "0x1e2e22799540",
                  "0x0"
                ],
                "id": "1"
              }
            ]
        """.trimIndent()

        val httpServiceMock = mock<HttpService> {
            on { send(any()) } doReturn HttpResponse(true, 200, mockResponse)
        }
        val provider = JsonRpcProvider("", httpServiceMock)

        val call = Call(
            DevnetClient.ethErc20ContractAddress,
            Felt.ZERO,
            listOf(Felt.ZERO),
        )
        val calls = listOf(provider.callContract(call), provider.callContract(call))
        val request = provider.batchRequests(calls)
        val response = request.send()

        assertEquals(response.size, calls.size)
    }

    @Test
    fun `rpc provider parses batch getTransactionStatus`() {
        val mockResponse = """
           [
              {
                "id": "0",
                "jsonrpc": "2.0",
                "result": {
                  "finality_status": "ACCEPTED_ON_L2",
                  "execution_status": "SUCCEEDED"
                }
              },
              {
                "id": "1",
                "jsonrpc": "2.0",
                "result": {
                  "finality_status": "ACCEPTED_ON_L2",
                  "execution_status": "REVERTED"
                }
              }
            ]
        """.trimIndent()

        val txHash1 = "0x06376162aed112c9ded4fad481d514decdc0cb766c765b892e368e11891eff8d"
        val txHash2 = "0x04a092caa24beca481307c1d7e4bc2fa0156e495701c4e0250367eea23352bc5"

        val httpServiceMock = mock<HttpService> {
            on { send(any()) } doReturn HttpResponse(true, 200, mockResponse)
        }
        val provider = JsonRpcProvider("", httpServiceMock)

        val request = provider.batchRequests(
            provider.getTransactionStatus(Felt.fromHex(txHash1)),
            provider.getTransactionStatus(Felt.fromHex(txHash2)),
        )
        val response = request.send()

        assertEquals(response[0].getOrThrow().finalityStatus, TransactionStatus.ACCEPTED_ON_L2)
        assertEquals(response[0].getOrThrow().executionStatus, TransactionExecutionStatus.SUCCEEDED)

        assertEquals(response[1].getOrThrow().finalityStatus, TransactionStatus.ACCEPTED_ON_L2)
        assertEquals(response[1].getOrThrow().executionStatus, TransactionExecutionStatus.REVERTED)
    }

    @Test
    fun `rpc provider parses batch response with incorrect order`() {
        val mockResponse = """
           [
              {
                "id": "1",
                "jsonrpc": "2.0",
                "result": {
                  "finality_status": "ACCEPTED_ON_L2",
                  "execution_status": "REVERTED"
                }
              },
              {
                "id": "0",
                "jsonrpc": "2.0",
                "result": {
                  "finality_status": "ACCEPTED_ON_L2",
                  "execution_status": "SUCCEEDED"
                }
              }
            ]
        """.trimIndent()

        val txHash1 = "0x06376162aed112c9ded4fad481d514decdc0cb766c765b892e368e11891eff8d"
        val txHash2 = "0x04a092caa24beca481307c1d7e4bc2fa0156e495701c4e0250367eea23352bc5"

        val httpServiceMock = mock<HttpService> {
            on { send(any()) } doReturn HttpResponse(true, 200, mockResponse)
        }
        val provider = JsonRpcProvider("", httpServiceMock)

        val request = provider.batchRequests(
            provider.getTransactionStatus(Felt.fromHex(txHash1)),
            provider.getTransactionStatus(Felt.fromHex(txHash2)),
        )
        val response = request.send()

        assertEquals(response[0].getOrThrow().finalityStatus, TransactionStatus.ACCEPTED_ON_L2)
        assertEquals(response[0].getOrThrow().executionStatus, TransactionExecutionStatus.SUCCEEDED)

        assertEquals(response[1].getOrThrow().finalityStatus, TransactionStatus.ACCEPTED_ON_L2)
        assertEquals(response[1].getOrThrow().executionStatus, TransactionExecutionStatus.REVERTED)
    }
}
