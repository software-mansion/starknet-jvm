package com.swmansion.starknet.data

import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.StarknetChainId
import com.swmansion.starknet.data.types.transactions.DAMode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class TransactionHashCalculatorTest {
    private val calldata = listOf(Felt(999), Felt(888), Felt(777))
    private val maxFee = Felt.fromHex("0xabcd987654210")
    private val chainId = StarknetChainId.GOERLI
    private val version = Felt.ONE

    @Test
    fun `calculate invoke v1 transaction hash`() {
        val hash = TransactionHashCalculator.calculateInvokeTxV1Hash(
            contractAddress = Felt.fromHex("0x6352037a8acbb31095a8ed0f4aa8d8639e13b705b043a1b08f9640d2f9f0d56"),
            calldata = calldata,
            chainId = chainId,
            version = version,
            nonce = Felt(9876),
            maxFee = maxFee,
        )
        val expected = Felt.fromHex("0x119b1a69e0c35b9035be945d3a1d551f2f78473b10311734fafb1f5df3f61d9")

        assertEquals(expected, hash)
    }

    @Test
    fun `calculate deploy account v1 transaction hash`() {
        val hash = TransactionHashCalculator.calculateDeployAccountV1TxHash(
            classHash = Felt.fromHex("0x21a7f43387573b68666669a0ed764252ce5367708e696e31967764a90b429c2"),
            calldata = calldata,
            salt = Felt(1234),
            chainId = chainId,
            version = version,
            maxFee = maxFee,
            nonce = Felt.ZERO,
        )
        val expected = Felt.fromHex("0x68beaf15e356928a1850cf343be85032efad964324b0abca4a9a57ff2057ef7")

        assertEquals(expected, hash)
    }

    @Test
    fun `calculate data availability modes`(){
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
}
