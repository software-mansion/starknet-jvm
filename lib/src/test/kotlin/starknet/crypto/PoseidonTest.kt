package starknet.crypto

import com.swmansion.starknet.crypto.Poseidon
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.Felt.Companion.fromHex
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigInteger

internal class PoseidonTest {

    @Test
    fun `poseidon hash single zero`() {
        val zeroValue = Felt.ZERO
        val result = Poseidon.poseidonHash(zeroValue)
        assertEquals(fromHex("0x60009f680a43e6f760790f76214b26243464cdd4f31fdc460baf66d32897c1b"), result)
    }

    @Test
    fun `poseidon hash single one`() {
        val zeroValue = Felt.ONE
        val result = Poseidon.poseidonHash(zeroValue)
        assertEquals(fromHex("0x6d226d4c804cd74567f5ac59c6a4af1fe2a6eced19fb7560a9124579877da25"), result)
    }

    @Test
    fun `poseidon hash single big number`() {
        val value = Felt(BigInteger("737869762948382064636737869762948382064636737869762948382064636"))
        val result = Poseidon.poseidonHash(value)
        assertEquals(fromHex("0x1580978ed34d52bfbc78c9f21da6e9df1ed6544bf1dd561616b0aba45a40380"), result)
    }

    @Test
    fun `poseidon hash double zero`() {
        val zeroValue = Felt.ZERO
        val result = Poseidon.poseidonHash(zeroValue, zeroValue)
        assertEquals(fromHex("0x293d3e8a80f400daaaffdd5932e2bcc8814bab8f414a75dcacf87318f8b14c5"), result)
    }

    @Test
    fun `poseidon hash double big numbers`() {
        val value = Felt(BigInteger("737869762948382064636737869762948382064636737869762948382064636"))
        val result = Poseidon.poseidonHash(value, value)
        assertEquals(fromHex("0x59c0ba54a2613d811726e10be9d6f7e01cf52d6d68ced0d16829027948cdfc3"), result)
    }

    @Test
    fun `poseidon hash many all zeros`() {
        val zeroValue = Felt.ZERO
        val result = Poseidon.poseidonHash(listOf(zeroValue, zeroValue, zeroValue))
        assertEquals(fromHex("0x29aee7812642221479b7e8af204ceaa5a7b7e113349fc8fb93e6303b477eb4d"), result)
    }

    @Test
    fun `poseidon hash many zeros`() {
        val zeroValue = Felt.ZERO
        val result = Poseidon.poseidonHash(listOf(zeroValue, zeroValue, zeroValue))
        assertEquals(fromHex("0x29aee7812642221479b7e8af204ceaa5a7b7e113349fc8fb93e6303b477eb4d"), result)
    }

    @Test
    fun `poseidon hash many random values`() {
        val result = Poseidon.poseidonHash(listOf(Felt(BigInteger("10")), Felt(BigInteger("8")), Felt(BigInteger("5"))))
        assertEquals(fromHex("0x53aa661c2388b74f48a16163c38893760e26884211599194ffe264f14b5c6e7"), result)
    }

    @Test
    fun `poseidon hash many big numbers`() {
        val value1 = Felt(BigInteger("737869762948382064636737869762948382064636737869762948382064636"))
        val value2 = Felt(BigInteger("948382064636737869762948382064636737869762948382064636737869762"))
        val result = Poseidon.poseidonHash(listOf(value1, value2, value1))
        assertEquals(fromHex("0xdaa82261a460722d8deb7d3bb2cb1838084887549df141540b6d88658d34ed"), result)
    }

    @Test
    fun `poseidon hash 4 numbers zeros`() {
        val zeroValue = Felt.ZERO
        val result = Poseidon.poseidonHash(zeroValue, zeroValue, zeroValue, zeroValue)
        assertEquals(fromHex("0x5c4def9d0323f31f80e90c55fa780591ed2e2fee266491c0bd891aedac38935"), result)
    }

    @Test
    fun `poseidon hash 4 numbers`() {
        val result = Poseidon.poseidonHash(listOf(Felt(BigInteger("1")), Felt(BigInteger("10")), Felt(BigInteger("100")), Felt(BigInteger("1000"))))
        assertEquals(fromHex("0x51f923f87ee53d16c2d680c2c0c9eb0132ba255d52b6dd69f4b9918dcbe00a1"), result)
    }

    @Test
    fun `poseidon hash 10 numbers zeros`() {
        val zeroValue = Felt.ZERO
        val result = Poseidon.poseidonHash(List(10) { zeroValue })
        assertEquals(fromHex("0x7c19756199eacf9ac8c06ecab986929be144ee4a852db16f796435562e69c7c"), result)
    }
}
