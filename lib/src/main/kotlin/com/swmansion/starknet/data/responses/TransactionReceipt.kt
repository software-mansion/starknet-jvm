@file:JvmName("TransactionReceipt")

package com.swmansion.starknet.data.responses

import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.TransactionStatus
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
// OptIn needed because @JsonNames is part of the experimental serialization api
data class TransactionReceipt(
    @JsonNames("transaction_hash", "txn_hash")
    val hash: Felt,

    @JsonNames("status")
    val status: TransactionStatus,

    @JsonNames("version")
    val version: Felt = Felt(0),

    @JsonNames("actual_fee")
    val actualFee: Felt,

    @JsonNames("statusData")
    val rejectionReason: String? = null,
)
