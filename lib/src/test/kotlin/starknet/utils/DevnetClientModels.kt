package starknet.utils

import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.transactions.GatewayFailureReason
import com.swmansion.starknet.data.types.transactions.TransactionStatus
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.jsonObject

data class DeployAccountResult(
    val details: AccountDetails,
    val transactionHash: Felt,
)
data class CreateAccountResult(
    val details: AccountDetails,
    val maxFee: Felt,
)

data class DeclareContractResult(
    val classHash: Felt,
    val transactionHash: Felt,
)

data class DeployContractResult(
    val contractAddress: Felt,
    val transactionHash: Felt,
)

data class InvokeContractResult(
    val transactionHash: Felt,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class AccountDetails(
    @JsonNames("private_key")
    val privateKey: Felt,

    @JsonNames("public_key")
    val publicKey: Felt,

    @JsonNames("address")
    val address: Felt,

    @JsonNames("salt")
    val salt: Felt,
)

// // Simplified receipt that is intended to support any JSON-RPC version starting 0.3,
// // to avoid DevnetClient relying on TransactionReceipt dataclasses.
// // Only use it for checking whether a transaction was successful.
// @Serializable
// data class DevnetReceipt (
//        @SerialName("status")
//        val status: TransactionStatus? = null,
//
//        @SerialName("execution_status")
//        val executionStatus: TransactionExecutionStatus? = null,
//
//        @SerialName("finality_status")
//        val finalityStatus: TransactionFinalityStatus? = null
// ) {
//    val isSuccessful: Boolean get() = when (status) {
//            null -> executionStatus == TransactionExecutionStatus.SUCCEEDED &&
//                    (finalityStatus == TransactionFinalityStatus.ACCEPTED_ON_L1 ||
//                            finalityStatus == TransactionFinalityStatus.ACCEPTED_ON_L2)
//            else -> status == TransactionStatus.ACCEPTED_ON_L1 ||
//                    status == TransactionStatus.ACCEPTED_ON_L2
//    }
// }

class AccountDetailsSerializer(val name: String) :
    JsonTransformingSerializer<AccountDetails>(AccountDetails.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        val accounts = element.jsonObject.getOrElse("alpha-goerli") { throw DevnetSetupFailedException("Invalid account file") }
        return accounts.jsonObject.getOrElse(name) { throw DevnetSetupFailedException("Details for account \"$name\" not found") }
    }
}

class DevnetSetupFailedException(message: String) : Exception(message)

class LegacyDevnetOperationFailed(val failureReason: GatewayFailureReason?, val status: TransactionStatus) :
    Exception(failureReason?.errorMessage ?: "Devnet operation failed")
