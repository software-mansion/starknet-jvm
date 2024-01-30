package starknet.crypto

import com.swmansion.starknet.data.types.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FeeUtilsTest {
    companion object {
        val estimateFee = EstimateFeeResponse(
            gasConsumed = Felt(1000),
            gasPrice = Felt(100),
            overallFee = Felt(1000 * 100),
            feeUnit = PriceUnit.WEI,
        )
    }

    @Nested
    inner class EstimateFeeToMaxFeeTest {
        @Test
        fun `estimate fee to max fee - default`() {
            val result = estimateFee.toMaxFee()

            assertEquals(result, Felt(150000))
        }

        @Test
        fun `estimate fee to max fee - specific overhead`() {
            val result = estimateFee.toMaxFee(0.13)

            assertEquals(result, Felt(113000))
        }

        @Test
        fun `estimate fee to max fee - 0 overhead`() {
            val result = estimateFee.toMaxFee(0.0)

            assertEquals(result, Felt(100000))
        }
    }

    @Nested
    inner class EstimateFeeToResourceBoundsTest {
        @Test
        fun `estimate fee to resource bounds - default`() {
            val result = estimateFee.toResourceBounds()
            val expected = ResourceBoundsMapping(
                l1Gas = ResourceBounds(maxAmount = Uint64(1100), maxPricePerUnit = Uint128(150)),
            )
            assertEquals(expected, result)
        }

        @Test
        fun `estimate fee to resource bounds - specific overhead`() {
            val result = estimateFee.toResourceBounds(0.19, 0.13)
            val expected = ResourceBoundsMapping(
                l1Gas = ResourceBounds(maxAmount = Uint64(1190), maxPricePerUnit = Uint128(113)),
            )
            assertEquals(expected, result)
        }

        @Test
        fun `estimate fee to resource bounds - 0 overhead`() {
            val result = estimateFee.toResourceBounds(0.0, 0.0)
            val expected = ResourceBoundsMapping(
                l1Gas = ResourceBounds(maxAmount = Uint64(1000), maxPricePerUnit = Uint128(100)),
            )
            assertEquals(expected, result)
        }
    }
}
