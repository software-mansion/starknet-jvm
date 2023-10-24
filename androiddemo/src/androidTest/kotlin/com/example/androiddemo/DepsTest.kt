package com.example.androiddemo

import androidx.test.ext.junit.runners.AndroidJUnit4
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
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class DepsTest {
    @Test
    fun okHttpClient() {
        val client = OkHttpClient()
        assertNotNull(client)
    }

    @Test
    fun jsonSerialization() {
        val json = Json { ignoreUnknownKeys = true }
        val value: Int = json.decodeFromString("451")
        assertEquals(451, value)
    }

    @Test
    fun poseidonHash() {
        val poseidonHash = Poseidon.poseidonHash(listOf(Felt(1), Felt(2)))
        Assert.assertNotEquals(Felt.ZERO, poseidonHash)
    }

    @Test
    fun keccakHash() {
        val keccakHash = starknetKeccak("123".toByteArray())
        Assert.assertNotEquals(Felt.ZERO, keccakHash)
    }

    @Test
    fun pedersenHash() {
        val pedersenHash = StarknetCurve.pedersen(Felt(1), Felt(2))
        Assert.assertNotEquals(Felt.ZERO, pedersenHash)
    }

    @Test
    fun getPublicKey() {
        // Relies on crypto_jni being built and available in the test environment.
        val privateKey = Felt(1234)
        val publicKey = StarknetCurve.getPublicKey(privateKey)
        Assert.assertNotEquals(Felt.ZERO, publicKey)
    }
}
