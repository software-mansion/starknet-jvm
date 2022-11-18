package com.swmansion.starknet.data

import com.swmansion.starknet.crypto.keccak
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.extensions.toFelt
import org.bouncycastle.jcajce.provider.digest.Keccak
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigInteger
import java.util.*

data class ChecksumTestCase(
    val incorrect: String,
    val correct: String,
)

internal class ContractAddressCalculatorTest {
    private val classHash =
        BigInteger("951442054899045155353616354734460058868858519055082696003992725251069061570").toFelt
    private val constructorCalldata = listOf(Felt(21), Felt(37))
    private val salt = Felt(1111)

    companion object {
        // List of (address, valid checksum address)
        @JvmStatic
        fun checksumAddresses() = listOf(
            ChecksumTestCase(
                "0x2fd23d9182193775423497fc0c472e156c57c69e4089a1967fb288a2d84e914",
                "0x02Fd23d9182193775423497fc0c472E156C57C69E4089A1967fb288A2d84e914",
            ),
            ChecksumTestCase(
                "0x00abcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefab",
                "0x00AbcDefaBcdefabCDEfAbCDEfAbcdEFAbCDEfabCDefaBCdEFaBcDeFaBcDefAb",
            ),
            ChecksumTestCase(
                "0xfedcbafedcbafedcbafedcbafedcbafedcbafedcbafedcbafedcbafedcbafe",
                "0x00fEdCBafEdcbafEDCbAFedCBAFeDCbafEdCBAfeDcbaFeDCbAfEDCbAfeDcbAFE",
            ),
        )
    }

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

    @ParameterizedTest
    @MethodSource("checksumAddresses")
    fun `calculateChecksumAddress returns proper addresses`(case: ChecksumTestCase) {
        assertEquals(
            case.correct,
            ContractAddressCalculator.calculateChecksumAddress(Felt.fromHex(case.incorrect)),
        )
        assertEquals(
            case.correct,
            ContractAddressCalculator.calculateChecksumAddress(Felt.fromHex(case.correct)),
        )
    }

    @ParameterizedTest
    @MethodSource("checksumAddresses")
    fun `isChecksumAddressValid works`(case: ChecksumTestCase) {
        assertTrue(ContractAddressCalculator.isChecksumAddressValid(case.correct))
        assertFalse(ContractAddressCalculator.isChecksumAddressValid(case.incorrect))
    }

    fun byteArrayToHex(a: ByteArray): String {
        val sb = StringBuilder(a.size * 2)
        for (b in a) sb.append(String.format("%02x", b))
        return sb.toString()
    }

    @Test
    fun kck() {
//        assertEquals(
//            "0x5fe7f977e71dba2ea1a68e21057beebb9be2ac30c6410aa38d4f3fbe41dcffd2",
//            keccak(byteArrayOf(1)).toHex(),
//        )
//        val list = mutableListOf<Byte>()
//        list.addAll(
//            HexFormat.of().parseHex("0x00abcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefab")
//        )
        val byteArray = BigInteger("abcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefab", 16).toByteArray()
        println("HEX STRING")
        println(Felt.fromHex("0xabcdefabcdefab").hexString())
//        val byteArray = HexFormat.of().parseHex("abcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefab")

//        list.add(0.toByte())
        assertEquals(32, byteArray.size)
        println(byteArray[0])
        println(BigInteger("abcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefab", 16))
//        assertEquals(
//            "0xd5b179671f1663119ae493ffd1c621cba08fb301ae370dd5c828192d293a35f3",
//            keccak(
//                list.toByteArray(),
//            ).toHex(),
//        )

        val keccak = Keccak.Digest256().apply {
            update(byteArray)
        }
        val keccakResult = keccak.digest()
        assertEquals(
            "d5b179671f1663119ae493ffd1c621cba08fb301ae370dd5c828192d293a35f3",
            byteArrayToHex(keccakResult),
        )
    }
}
