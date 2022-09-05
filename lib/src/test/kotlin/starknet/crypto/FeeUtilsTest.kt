package starknet.crypto

import com.swmansion.starknet.crypto.FeeUtils
import com.swmansion.starknet.data.types.Felt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FeeUtilsTest {
    @Test
    fun `estimate fee to max fee - default`() {
        val result = FeeUtils.estimatedFeeToMaxFee(Felt(100))

        assertEquals(result, Felt(150))
    }

    @Test
    fun `estimate fee to max fee - 10 percent overhead`() {
        val result = FeeUtils.estimatedFeeToMaxFee(Felt(100), 0.10)

        assertEquals(result, Felt(110))
    }

    @Test
    fun `estimate fee to max fee - 0 overhead`() {
        val result = FeeUtils.estimatedFeeToMaxFee(Felt(100), 0.0)

        assertEquals(result, Felt(100))
    }
}
