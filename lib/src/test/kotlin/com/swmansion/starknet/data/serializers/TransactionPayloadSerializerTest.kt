package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File

private fun loadTransactionPayloadJsonString(path: String): String {
    return File("src/test/resources/transaction_payloads/$path").readText()
}

class TransactionPayloadSerializerTest {
    @Test
    fun `deserialize invoke transaction v3 payload`() {
        val payloadJsonString = loadTransactionPayloadJsonString("v3/invoke.json")
        val payload = Json.decodeFromString(TransactionPayloadSerializer, payloadJsonString)
        assertTrue(payload is InvokeTransactionV3Payload)
    }

    @Test
    fun `deserialize invoke transaction v3 payload for fee estimate`() {
        val payloadJsonString = loadTransactionPayloadJsonString("v3/invoke_query.json")
        val payload = Json.decodeFromString(TransactionPayloadSerializer, payloadJsonString)
        assertTrue(payload is InvokeTransactionV3Payload)
    }

    @Test
    fun `deserialize invoke transaction v1 payload`() {
        val payloadJsonString = loadTransactionPayloadJsonString("v1/invoke.json")
        val payload = Json.decodeFromString(TransactionPayloadSerializer, payloadJsonString)
        assertTrue(payload is InvokeTransactionV1Payload)
    }

    @Test
    fun `deserialize invoke transaction v1 payload for fee estimate`() {
        val payloadJsonString = loadTransactionPayloadJsonString("v1/invoke_query.json")
        val payload = Json.decodeFromString(TransactionPayloadSerializer, payloadJsonString)
        assertTrue(payload is InvokeTransactionV1Payload)
    }

    @Test
    fun `deserialize declare transaction v3 payload`() {
        val payloadJsonString = loadTransactionPayloadJsonString("v3/declare.json")
        val payload = Json.decodeFromString(TransactionPayloadSerializer, payloadJsonString)
        assertTrue(payload is DeclareTransactionV3Payload)
    }

    @Test
    fun `deserialize declare transaction v3 payload for fee estimate`() {
        val payloadJsonString = loadTransactionPayloadJsonString("v3/declare_query.json")
        val payload = Json.decodeFromString(TransactionPayloadSerializer, payloadJsonString)
        assertTrue(payload is DeclareTransactionV3Payload)
    }

    @Test
    fun `deserialize declare transaction v2 payload`() {
        val payloadJsonString = loadTransactionPayloadJsonString("v2/declare.json")
        val payload = Json.decodeFromString(TransactionPayloadSerializer, payloadJsonString)
        assertTrue(payload is DeclareTransactionV2Payload)
    }

    @Test
    fun `deserialize declare transaction v2 payload for fee estimate`() {
        val payloadJsonString = loadTransactionPayloadJsonString("v2/declare_query.json")
        val payload = Json.decodeFromString(TransactionPayloadSerializer, payloadJsonString)
        assertTrue(payload is DeclareTransactionV2Payload)
    }

    @Test
    fun `deserialize deploy account transaction v3 payload`() {
        val payloadJsonString = loadTransactionPayloadJsonString("v3/deploy_account.json")
        val payload = Json.decodeFromString(TransactionPayloadSerializer, payloadJsonString)
        assertTrue(payload is DeployAccountTransactionV3Payload)
    }

    @Test
    fun `deserialize deploy account transaction v3 payload for fee estimate`() {
        val payloadJsonString = loadTransactionPayloadJsonString("v3/deploy_account_query.json")
        val payload = Json.decodeFromString(TransactionPayloadSerializer, payloadJsonString)
        assertTrue(payload is DeployAccountTransactionV3Payload)
    }

    @Test
    fun `deserialize deploy account transaction v1 payload`() {
        val payloadJsonString = loadTransactionPayloadJsonString("v1/deploy_account.json")
        val payload = Json.decodeFromString(TransactionPayloadSerializer, payloadJsonString)
        assertTrue(payload is DeployAccountTransactionV1Payload)
    }

    @Test
    fun `deserialize deploy account transaction v1 payload for fee estimate`() {
        val payloadJsonString = loadTransactionPayloadJsonString("v1/deploy_account_query.json")
        val payload = Json.decodeFromString(TransactionPayloadSerializer, payloadJsonString)
        assertTrue(payload is DeployAccountTransactionV1Payload)
    }

    @Test
    fun `deserialize transaction payload with missing transaction type`() {
        val payloadJsonString =
            """
                {
                
                }
            """.trimIndent()
        assertThrows<SerializationException> { Json.decodeFromString(TransactionPayloadSerializer, payloadJsonString) }
    }

    @Test
    fun `deserialize transaction payload with invalid transaction type`() {
        val payloadJsonString =
            """
                {
                    "type": "INVALID_TRANSACTION_TYPE"
                }
            """.trimIndent()
        assertThrows<SerializationException> { Json.decodeFromString(TransactionPayloadSerializer, payloadJsonString) }
    }

    @Test
    fun `deserialize transaction payload with deploy (deprecated) transaction type`() {
        val payloadJsonString =
            """
                {
                    "type": "DEPLOY"
                }
            """.trimIndent()
        assertThrows<IllegalArgumentException> { Json.decodeFromString(TransactionPayloadSerializer, payloadJsonString) }
    }

    @Test
    fun `deserialize invoke transaction payload with invalid version`() {
        val payloadJsonString =
            """
                {
                    "type": "INVOKE",
                    "version": "0x123"
                }
            """.trimIndent()
        assertThrows<IllegalArgumentException> { Json.decodeFromString(TransactionPayloadSerializer, payloadJsonString) }
    }

    @Test
    fun `deserialize declare transaction payload with invalid version`() {
        val payloadJsonString =
            """
                {
                    "type": "DECLARE",
                    "version": "0x123"
                }
            """.trimIndent()
        assertThrows<IllegalArgumentException> { Json.decodeFromString(TransactionPayloadSerializer, payloadJsonString) }
    }

    @Test
    fun `deserialize deploy account transaction payload with invalid version`() {
        val payloadJsonString =
            """
                {
                    "type": "DEPLOY_ACCOUNT",
                    "version": "0x123"
                }
            """.trimIndent()
        assertThrows<IllegalArgumentException> { Json.decodeFromString(TransactionPayloadSerializer, payloadJsonString) }
    }
}
