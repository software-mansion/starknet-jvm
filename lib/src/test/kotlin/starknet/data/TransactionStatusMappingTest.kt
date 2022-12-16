package starknet.data

import com.swmansion.starknet.data.types.transactions.TransactionStatus
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TransactionStatusMappingTest {

    @Test
    fun `received status is mapped to pending`() {
        val statusString = "\"RECEIVED\""
        val status = Json.decodeFromString(TransactionStatus.serializer(), statusString)

        assertEquals(TransactionStatus.PENDING, status)
    }

    @Test
    fun `not received transaction is mapped to unknown`() {
        val statusString = "\"NOT_RECEIVED\""
        val status = Json.decodeFromString(TransactionStatus.serializer(), statusString)

        assertEquals(TransactionStatus.UNKNOWN, status)
    }

    @Test
    fun `unknown transaction type throws exception`() {
        val statusString = "\"RANDOM_STATUS\""

        assertThrows<SerializationException> {
            Json.decodeFromString(TransactionStatus.serializer(), statusString)
        }
    }
}
