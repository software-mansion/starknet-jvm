package starknet.data.types

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigInteger

internal class TransactionsTest {
    @Test
    fun getHash() {
        val tx1 = InvokeTransaction(
            version = Felt.ZERO,
            contractAddress = Felt.fromHex("0x2a"),
            entrypointSelector = Felt.fromHex("0x64"),
            calldata = listOf(),
            maxFee = Felt.ZERO,
            chainId = StarknetChainId.TESTNET.value,
            nonce = Felt.ZERO
        )

        assertEquals(tx1.getHash(), Felt.fromHex("0x7d260744de9d8c55e7675a34512d1951a7b262c79e685d26599edd2948de959"))

        val tx2 = InvokeTransaction(
            contractAddress = Felt(
                BigInteger("468485892896389608042320470922610020674017592380673471682128582128678525733")
            ),
            entrypointSelector = Felt(
                BigInteger("617075754465154585683856897856256838130216341506379215893724690153393808813")
            ),
            calldata = listOf(
                Felt(BigInteger("1")),
                Felt(
                    BigInteger("468485892896389608042320470922610020674017592380673471682128582128678525733")
                ),
                Felt(
                    BigInteger("1307260637166823203998179679098545329314629630090003875272134084395659334905")
                ),
                Felt(BigInteger("0")),
                Felt(BigInteger("1")),
                Felt(BigInteger("1")),
                Felt(
                    BigInteger("807694187056572246556317413263910754299517324162860342603752464651582167489")
                ),
                Felt(
                    BigInteger("2")
                )
            ),
            chainId = Felt(BigInteger("1536727068981429685321")),
            maxFee = Felt(BigInteger("100000000")),
            version = Felt(BigInteger("0")),
            nonce = Felt.ZERO,
        )

        assertEquals(tx2.getHash(), Felt.fromHex("0x77b27f044ac1402af4e44fc012655822c2da2ac231deb003d797f0359055228"))
    }
}