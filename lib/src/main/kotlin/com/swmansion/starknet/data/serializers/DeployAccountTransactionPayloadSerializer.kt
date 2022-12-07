package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.transactions.DeployAccountTransactionPayload
import com.swmansion.starknet.extensions.put
import com.swmansion.starknet.extensions.toDecimal
import kotlinx.serialization.json.*

internal fun serializeDeployAccountTransactionPayload(
        payload: DeployAccountTransactionPayload,
): JsonObject =
        buildJsonObject {
            put("type", "DEPLOY_ACCOUNT")
            put("class_hash", payload.classHash)
            put("contract_address_salt", payload.salt)
            putJsonArray("constructor_calldata") {
                payload.constructorCalldata.toDecimal().forEach { add(it) }
            }
            put("version", payload.version)
            put("nonce", payload.nonce)
            put("max_fee", payload.maxFee)
            putJsonArray("signature") {
                payload.signature.toDecimal().forEach { add(it) }
            }
        }
