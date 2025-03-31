package com.swmansion.starknet.data

import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.data.types.DAMode
import com.swmansion.starknet.data.types.TransactionVersion
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class TransactionHashCalculatorTest {
    @Nested
    inner class DeprecatedTransactionHashTest {
        private val calldata = listOf(Felt(999), Felt(888), Felt(777))
        private val maxFee = Felt.fromHex("0xabcd987654210")

        private val chainId = StarknetChainId.fromNetworkName("SN_GOERLI")

        @Test
        fun `calculate invoke v1 transaction hash`() {
            val hash = TransactionHashCalculator.calculateInvokeTxV1Hash(
                contractAddress = Felt.fromHex("0x6352037a8acbb31095a8ed0f4aa8d8639e13b705b043a1b08f9640d2f9f0d56"),
                calldata = calldata,
                chainId = chainId,
                version = TransactionVersion.V1,
                nonce = Felt(9876),
                maxFee = maxFee,
            )
            val expected = Felt.fromHex("0x119b1a69e0c35b9035be945d3a1d551f2f78473b10311734fafb1f5df3f61d9")
            assertEquals(expected, hash)
        }
    }

    @Nested
    inner class TransactionHashV3Test {
        private val chainId = StarknetChainId.fromNetworkName("SN_GOERLI")

        @Test
        fun `prepare data availability modes`() {
            val result = TransactionHashCalculator.prepareDataAvailabilityModes(
                feeDataAvailabilityMode = DAMode.L1,
                nonceDataAvailabilityMode = DAMode.L1,
            )
            assertEquals(Felt.ZERO, result)

            val result2 = TransactionHashCalculator.prepareDataAvailabilityModes(
                feeDataAvailabilityMode = DAMode.L2,
                nonceDataAvailabilityMode = DAMode.L1,
            )
            assertEquals(Felt.ONE, result2)

            val result3 = TransactionHashCalculator.prepareDataAvailabilityModes(
                feeDataAvailabilityMode = DAMode.L1,
                nonceDataAvailabilityMode = DAMode.L2,
            )
            assertEquals(Felt.fromHex("0x100000000"), result3)

            val result4 = TransactionHashCalculator.prepareDataAvailabilityModes(
                feeDataAvailabilityMode = DAMode.L2,
                nonceDataAvailabilityMode = DAMode.L2,
            )
            assertEquals(Felt.fromHex("0x100000001"), result4)
        }

        @Test
        fun `calculate invoke v3 transaction hash`() {
            val hash = TransactionHashCalculator.calculateInvokeTxV3Hash(
                senderAddress = Felt.fromHex("0x3f6f3bc663aedc5285d6013cc3ffcbc4341d86ab488b8b68d297f8258793c41"),
                calldata = listOf(
                    Felt.fromHex("0x2"),
                    Felt.fromHex("0x4c312760dfd17a954cdd09e76aa9f149f806d88ec3e402ffaf5c4926f568a42"),
                    Felt.fromHex("0x31aafc75f498fdfa7528880ad27246b4c15af4954f96228c9a132b328de1c92"),
                    Felt.fromHex("0x0"),
                    Felt.fromHex("0x6"),
                    Felt.fromHex("0x450703c32370cf7ffff540b9352e7ee4ad583af143a361155f2b485c0c39684"),
                    Felt.fromHex("0xb17d8a2731ba7ca1816631e6be14f0fc1b8390422d649fa27f0fbb0c91eea8"),
                    Felt.fromHex("0x6"),
                    Felt.fromHex("0x0"),
                    Felt.fromHex("0x6"),
                    Felt.fromHex("0x6333f10b24ed58cc33e9bac40b0d52e067e32a175a97ca9e2ce89fe2b002d82"),
                    Felt.fromHex("0x3"),
                    Felt.fromHex("0x602e89fe5703e5b093d13d0a81c9e6d213338dc15c59f4d3ff3542d1d7dfb7d"),
                    Felt.fromHex("0x20d621301bea11ffd9108af1d65847e9049412159294d0883585d4ad43ad61b"),
                    Felt.fromHex("0x276faadb842bfcbba834f3af948386a2eb694f7006e118ad6c80305791d3247"),
                    Felt.fromHex("0x613816405e6334ab420e53d4b38a0451cb2ebca2755171315958c87d303cf6"),
                ),
                chainId = chainId,
                version = TransactionVersion.V3,
                nonce = Felt.fromHex("0x8a9"),
                accountDeploymentData = emptyList(),
                tip = Uint64.ZERO,
                resourceBounds = ResourceBoundsMapping(
                    l1Gas = ResourceBounds(
                        maxAmount = Uint64.fromHex("0x186a0"),
                        maxPricePerUnit = Uint128.fromHex("0x5af3107a4000"),
                    ),
                    l2Gas = ResourceBounds(
                        maxAmount = Uint64.fromHex("0x186a0"),
                        maxPricePerUnit = Uint128.fromHex("0x5af3107a4000"),
                    ),
                    l1DataGas = ResourceBounds(
                        maxAmount = Uint64.fromHex("0x186a0"),
                        maxPricePerUnit = Uint128.fromHex("0x5af3107a4000"),
                    ),
                ),
                paymasterData = emptyList(),
                feeDataAvailabilityMode = DAMode.L1,
                nonceDataAvailabilityMode = DAMode.L1,
            )
            val expected = Felt.fromHex("0x58bf19c9d19264cd2618fd4a9bebe75db59c0c865810986eb352723f6571648")
            assertEquals(expected, hash)
        }

        @Test
        fun `calculate deploy account v3 transaction hash`() {
            val hash = TransactionHashCalculator.calculateDeployAccountV3TxHash(
                constructorCalldata = listOf(
                    Felt.fromHex("0x5cd65f3d7daea6c63939d659b8473ea0c5cd81576035a4d34e52fb06840196c"),
                ),
                classHash = Felt.fromHex("0x2338634f11772ea342365abd5be9d9dc8a6f44f159ad782fdebd3db5d969738"),
                salt = Felt.ZERO,
                chainId = chainId,
                version = TransactionVersion.V3,
                nonce = Felt.ZERO,
                resourceBounds = ResourceBoundsMapping(
                    l1Gas = ResourceBounds(
                        maxAmount = Uint64.fromHex("0x186a0"),
                        maxPricePerUnit = Uint128.fromHex("0x5af3107a4000"),
                    ),
                    l2Gas = ResourceBounds(
                        maxAmount = Uint64.fromHex("0x186a0"),
                        maxPricePerUnit = Uint128.fromHex("0x5af3107a4000"),
                    ),
                    l1DataGas = ResourceBounds(
                        maxAmount = Uint64.fromHex("0x186a0"),
                        maxPricePerUnit = Uint128.fromHex("0x5af3107a4000"),
                    ),
                ),
                tip = Uint64.ZERO,
                paymasterData = emptyList(),
                feeDataAvailabilityMode = DAMode.L1,
                nonceDataAvailabilityMode = DAMode.L1,
            )
            val expected = Felt.fromHex("0xee682037ec979ab79b3cdb9dfca4f1088eef7cbb1aeadc9cb07b478805747e")
            assertEquals(expected, hash)
        }

        @Test
        fun `calculate declare v3 transaction hash`() {
            val hash = TransactionHashCalculator.calculateDeclareV3TxHash(
                classHash = Felt.fromHex("0x5ae9d09292a50ed48c5930904c880dab56e85b825022a7d689cfc9e65e01ee7"),
                compiledClassHash = Felt.fromHex("0x1add56d64bebf8140f3b8a38bdf102b7874437f0c861ab4ca7526ec33b4d0f8"),
                senderAddress = Felt.fromHex("0x2fab82e4aef1d8664874e1f194951856d48463c3e6bf9a8c68e234a629a6f50"),
                version = TransactionVersion.V3,
                chainId = chainId,
                nonce = Felt.ONE,
                resourceBounds = ResourceBoundsMapping(
                    l1Gas = ResourceBounds(
                        maxAmount = Uint64.fromHex("0x186a0"),
                        maxPricePerUnit = Uint128.fromHex("0x2540be400"),
                    ),
                    l2Gas = ResourceBounds(
                        maxAmount = Uint64.fromHex("0x186a0"),
                        maxPricePerUnit = Uint128.fromHex("0x2540be400"),
                    ),
                    l1DataGas = ResourceBounds(
                        maxAmount = Uint64.fromHex("0x186a0"),
                        maxPricePerUnit = Uint128.fromHex("0x2540be400"),
                    ),
                ),
                tip = Uint64.ZERO,
                paymasterData = emptyList(),
                accountDeploymentData = emptyList(),
                feeDataAvailabilityMode = DAMode.L1,
                nonceDataAvailabilityMode = DAMode.L1,
            )
            val expected = Felt.fromHex("0x15e515947b30e7a5ca52e8a25451f6993736ca933867ec41d33d473aaa46959")
            assertEquals(expected, hash)
        }
    }
}
