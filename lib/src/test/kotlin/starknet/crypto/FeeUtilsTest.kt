package starknet.crypto

import com.swmansion.starknet.data.types.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class FeeUtilsTest {
    companion object {
        val estimateFee = EstimateFeeResponse(
            l1GasConsumed = Felt(1000),
            l1GasPrice = Felt(100),
            l1DataGasConsumed = Felt(200),
            l1DataGasPrice = Felt(50),
            l2GasConsumed = Felt(1000),
            l2GasPrice = Felt(100),
            overallFee = Felt(1000 * 100 + 200 * 50 + 1000 * 100), // 210000
            feeUnit = PriceUnit.WEI,
        )
    }

    @Nested
    inner class EstimateFeeToMaxFeeTest {
        @Test
        fun `estimate fee to max fee - default`() {
            val result = estimateFee.toMaxFee()

            assertEquals(result, Felt(315000))
        }

        @Test
        fun `estimate fee to max fee - specific multiplier`() {
            val result = estimateFee.toMaxFee(1.13)

            assertEquals(result, Felt(237300))
        }

        @Test
        fun `estimate fee to max fee - 1 multiplier`() {
            val result = estimateFee.toMaxFee(1.0)

            assertEquals(result, Felt(210000))
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
                    maxAmount = Uint64(3150),
                    maxPricePerUnit = Uint128(150),
                ),
                l2Gas = ResourceBounds(
                    maxAmount = Uint64(3150),
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
                    maxAmount = Uint64(2499),
                    maxPricePerUnit = Uint128(113),
                ),
                // TODO: Check if these l2 resources bounds are adequate
                l2Gas = ResourceBounds(
                    maxAmount = Uint64(2499),
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
                    maxAmount = Uint64(2499),
                    maxPricePerUnit = Uint128(113),
                ),
                // TODO: Check if these l2 resources bounds are adequate
                l2Gas = ResourceBounds(
                    maxAmount = Uint64(2499),
                    maxPricePerUnit = Uint128(113),
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
