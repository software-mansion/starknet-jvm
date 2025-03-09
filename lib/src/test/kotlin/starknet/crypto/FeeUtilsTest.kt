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
            l1DataGasConsumed = Felt(1000),
            l1DataGasPrice = Felt(100),
            l2GasConsumed = Felt(200),
            l2GasPrice = Felt(50),
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
                    maxAmount = Uint64(1000),
                    maxPricePerUnit = Uint128(100),
                ),
                l2Gas = ResourceBounds(
                    maxAmount = Uint64(200),
                    maxPricePerUnit = Uint128(50),
                ),
                l1DataGas = ResourceBounds(
                    maxAmount = Uint64(1000),
                    maxPricePerUnit = Uint128(100),
                ),
            )
            assertEquals(expected, result)
        }

        @Test
        fun `estimate fee to resource bounds - specific multiplier`() {
            val result = estimateFee.toResourceBounds(1.19, 1.13)
            val expected = ResourceBoundsMapping(
                l1Gas = ResourceBounds(
                    maxAmount = Uint64(1190),
                    maxPricePerUnit = Uint128(113),
                ),
                l2Gas = ResourceBounds(
                    maxAmount = Uint64(238),
                    maxPricePerUnit = Uint128(56),
                ),
                l1DataGas = ResourceBounds(
                    maxAmount = Uint64(1190),
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
                    maxAmount = Uint64(1000),
                    maxPricePerUnit = Uint128(100),
                ),
                l2Gas = ResourceBounds(
                    maxAmount = Uint64(200),
                    maxPricePerUnit = Uint128(50),
                ),
                l1DataGas = ResourceBounds(
                    maxAmount = Uint64(1000),
                    maxPricePerUnit = Uint128(100)
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
