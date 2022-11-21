package com.swmansion.starknet.data

import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.extensions.toFelt
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigInteger

internal class ContractAddressCalculatorTest {
    private val classHash = BigInteger("951442054899045155353616354734460058868858519055082696003992725251069061570").toFelt
    private val constructorCalldata = listOf(Felt(21), Felt(37))
    private val salt = Felt(1111)

    @Test
    fun `calculateAddressFromHash without deployer address`() {
        val address = ContractAddressCalculator.calculateAddressFromHash(
            classHash = classHash,
            calldata = constructorCalldata,
            salt = salt,
        )
        val expected = BigInteger("1357105550695717639826158786311415599375114169232402161465584707209611368775").toFelt

        assertEquals(expected, address)
    }

    @Test
    fun `calculateAddressFromHash with deployer address`() {
        val deployerAddress = Felt(1234)
        val address = ContractAddressCalculator.calculateAddressFromHash(
            classHash = classHash,
            calldata = constructorCalldata,
            salt = salt,
            deployerAddress = deployerAddress,
        )
        val expected = BigInteger("3179899882984850239687045389724311807765146621017486664543269641150383510696").toFelt

        assertEquals(expected, address)
    }
}
