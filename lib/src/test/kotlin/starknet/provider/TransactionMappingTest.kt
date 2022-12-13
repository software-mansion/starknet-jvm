package starknet.provider

import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.StarknetChainId
import com.swmansion.starknet.data.types.transactions.GatewayTransactionReceipt
import com.swmansion.starknet.data.types.transactions.TransactionStatus
import com.swmansion.starknet.provider.gateway.GatewayProvider
import com.swmansion.starknet.service.http.HttpResponse
import com.swmansion.starknet.service.http.HttpService
import kotlinx.serialization.SerializationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class TransactionMappingTest {
    @Test
    fun `received transaction is mapped to pending`() {
        val txr = """
            {
                "status": "RECEIVED",
                "block_hash": "0x00001",
                "block_number": 1,
                "transaction_index": 1,
                "transaction_hash": "0x000001",
                "l2_to_l1_messages": [],
                "events": [],
                "execution_resources": {
                    "n_steps": 0,
                    "builtin_instance_counter": {},
                    "n_memory_holes": 0
                },
                "actual_fee": "0x0"
            }
        """.trimIndent()
        val httpServiceMock = mock<HttpService> {
            on { send(any()) } doReturn HttpResponse(true, 200, txr)
        }
        val provider = GatewayProvider("", "", StarknetChainId.TESTNET, httpServiceMock)
        val result = provider.getTransactionReceipt(Felt(1)).send()

        require(result is GatewayTransactionReceipt)
        assertEquals(TransactionStatus.PENDING, result.status)
    }

    @Test
    fun `not received transaction is mapped to unknown`() {
        val txr = """
            {
                "status": "NOT_RECEIVED",
                "block_hash": "0x00001",
                "block_number": 1,
                "transaction_index": 1,
                "transaction_hash": "0x000001",
                "l2_to_l1_messages": [],
                "events": [],
                "execution_resources": {
                    "n_steps": 0,
                    "builtin_instance_counter": {},
                    "n_memory_holes": 0
                },
                "actual_fee": "0x0"
            }
        """.trimIndent()
        val httpServiceMock = mock<HttpService> {
            on { send(any()) } doReturn HttpResponse(true, 200, txr)
        }
        val provider = GatewayProvider("", "", StarknetChainId.TESTNET, httpServiceMock)
        val result = provider.getTransactionReceipt(Felt(1)).send()

        require(result is GatewayTransactionReceipt)
        assertEquals(TransactionStatus.UNKNOWN, result.status)
    }

    @Test
    fun `unknown transaction type throws exception`() {
        val txr = """
            {
                "status": "RANDOM_STATUS",
                "block_hash": "0x00001",
                "block_number": 1,
                "transaction_index": 1,
                "transaction_hash": "0x000001",
                "l2_to_l1_messages": [],
                "events": [],
                "execution_resources": {
                    "n_steps": 0,
                    "builtin_instance_counter": {},
                    "n_memory_holes": 0
                },
                "actual_fee": "0x0"
            }
        """.trimIndent()
        val httpServiceMock = mock<HttpService> {
            on { send(any()) } doReturn HttpResponse(true, 200, txr)
        }
        val provider = GatewayProvider("", "", StarknetChainId.TESTNET, httpServiceMock)
        val request = provider.getTransactionReceipt(Felt(1))

        assertThrows<SerializationException> { request.send() }
    }
}
