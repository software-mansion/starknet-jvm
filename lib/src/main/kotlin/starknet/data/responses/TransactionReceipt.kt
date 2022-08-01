package starknet.data.responses

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import starknet.data.types.Felt
import starknet.data.types.TransactionStatus

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
    val rejectionReason: String? = null
)
