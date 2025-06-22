package starknet.data

import com.swmansion.starknet.data.types.TransactionStatus
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TransactionStatusMappingTest {
    @Test
    fun `unknown transaction status throws exception`() {
        val statusString = "\"RANDOM_STATUS\""

        assertThrows<SerializationException> {
            Json.decodeFromString(TransactionStatus.serializer(), statusString)
        }
    }
}
