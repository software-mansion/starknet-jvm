package starknet.data.responses

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import starknet.data.types.Felt
import starknet.data.types.TransactionStatus

@Serializable
data class TransactionReceipt @OptIn(ExperimentalSerializationApi::class) constructor(
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
