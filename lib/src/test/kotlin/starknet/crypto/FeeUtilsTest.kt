package starknet.crypto

import com.swmansion.starknet.crypto.FeeUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FeeUtilsTest {
    @Test
    fun `estimate fee to max fee - default`() {
        val result = FeeUtils.estimatedFeeToMaxFee(100)

        assertEquals(result, 150)
    }

    @Test
    fun `estimate fee to max fee - 10 percent overhead`() {
        val result = FeeUtils.estimatedFeeToMaxFee(100, 0.10)

        assertEquals(result, 110)
    }

    @Test
    fun `estimate fee to max fee - 0 overhead`() {
        val result = FeeUtils.estimatedFeeToMaxFee(100, 0.0)

        assertEquals(result, 100)
    }
}
