package starknet.crypto

import com.swmansion.starknet.data.types.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class FeeUtilsTest {
    companion object {
        val estimateFee = EstimateFeeResponse(
            gasConsumed = Felt(1000),
            gasPrice = Felt(100),
            dataGasConsumed = Felt(200),
            dataGasPrice = Felt(50),
            overallFee = Felt(1000 * 100 + 200 * 50), // 110000
            feeUnit = PriceUnit.WEI,
        )
    }

    @Nested
    inner class EstimateFeeToMaxFeeTest {
        @Test
        fun `estimate fee to max fee - default`() {
            val result = estimateFee.toMaxFee()

            assertEquals(result, Felt(165000))
        }

        @Test
        fun `estimate fee to max fee - specific multiplier`() {
            val result = estimateFee.toMaxFee(1.13)

            assertEquals(result, Felt(124300))
        }

        @Test
        fun `estimate fee to max fee - 1 multiplier`() {
            val result = estimateFee.toMaxFee(1.0)

            assertEquals(result, Felt(110000))
        }

        @Test
        fun `estimate fee to max fee - negative multiplier`() {
            assertThrows<java.lang.IllegalArgumentException> {
                estimateFee.toMaxFee(-1.0)
            }
        }
    }

    @Nested
    inner class EstimateFeeToResourceBoundsTest {
        @Test
        fun `estimate fee to resource bounds - default`() {
            val result = estimateFee.toResourceBounds()
            val expected = ResourceBoundsMapping(
                l1Gas = ResourceBounds(
                    maxAmount = Uint64(1650),
                    maxPricePerUnit = Uint128(150),
                ),
            )
            assertEquals(expected, result)
        }

        @Test
        fun `estimate fee to resource bounds - specific multiplier`() {
            val result = estimateFee.toResourceBounds(1.19, 1.13)
            val expected = ResourceBoundsMapping(
                l1Gas = ResourceBounds(
                    maxAmount = Uint64(1309),
                    maxPricePerUnit = Uint128(113),
                ),
            )
            assertEquals(expected, result)
        }

        @Test
        fun `estimate fee to resource bounds - 1 multiplier`() {
            val result = estimateFee.toResourceBounds(1.0, 1.0)
            val expected = ResourceBoundsMapping(
                l1Gas = ResourceBounds(
                    maxAmount = Uint64(1100),
                    maxPricePerUnit = Uint128(100),
                ),
            )
            assertEquals(expected, result)
        }

        @Test
        fun `estimate fee to resource bounds - negative multiplier`() {
            assertThrows<java.lang.IllegalArgumentException> {
                estimateFee.toResourceBounds(-1.0, 1.0)
            }
            assertThrows<java.lang.IllegalArgumentException> {
                estimateFee.toResourceBounds(1.0, -1.0)
            }
        }
    }
}
