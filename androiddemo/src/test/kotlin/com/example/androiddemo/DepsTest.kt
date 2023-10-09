package com.example.androiddemo

import com.swmansion.starknet.crypto.Poseidon
import com.swmansion.starknet.crypto.StarknetCurve
import com.swmansion.starknet.crypto.starknetKeccak
import com.swmansion.starknet.data.types.Felt
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

internal class DepsTest {
    @Test
    fun `okhttp client`() {
        val client = OkHttpClient()
        assertNotNull(client)
    }

    @Test
    fun `json serialization`() {
        val json = Json { ignoreUnknownKeys = true }
        val value: Int = json.decodeFromString("451")
        assertEquals(451, value)
    }

    @Test
    fun `poseidon hash`() {
        val poseidonHash = Poseidon.poseidonHash(listOf(Felt(1), Felt(2)))
        Assert.assertNotEquals(Felt.ZERO, poseidonHash)
    }

    @Test
    fun `keccak hash`() {
        val keccakHash = starknetKeccak("123".toByteArray())
        Assert.assertNotEquals(Felt.ZERO, keccakHash)
    }

    @Test
    fun `pedersen hash`() {
        val pedersenHash = StarknetCurve.pedersen(Felt(1), Felt(2))
        Assert.assertNotEquals(Felt.ZERO, pedersenHash)
    }

    @Test
    fun `get public key`() {
        // Relies on crypto_jni being built and available in the test environment.
        val privateKey = Felt(1234)
        val publicKey = StarknetCurve.getPublicKey(privateKey)
        Assert.assertNotEquals(Felt.ZERO, publicKey)
    }

}
