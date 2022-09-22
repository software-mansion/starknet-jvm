package starknet.crypto

import com.swmansion.starknet.crypto.estimatedFeeToMaxFee
import com.swmansion.starknet.data.types.Felt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FeeUtilsTest {
    @Test
    fun `estimate fee to max fee - default`() {
        val result = estimatedFeeToMaxFee(Felt(100))

        assertEquals(result, Felt(110))
    }

    @Test
    fun `estimate fee to max fee - 10 percent overhead`() {
        val result = estimatedFeeToMaxFee(Felt(100), 0.50)

        assertEquals(result, Felt(150))
    }

    @Test
    fun `estimate fee to max fee - 0 overhead`() {
        val result = estimatedFeeToMaxFee(Felt(100), 0.0)

        assertEquals(result, Felt(100))
    }
}
