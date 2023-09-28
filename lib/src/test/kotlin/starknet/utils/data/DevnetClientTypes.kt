package starknet.utils.data

import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.transactions.GatewayFailureReason
import com.swmansion.starknet.data.types.transactions.TransactionStatus
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

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

class DevnetSetupFailedException(message: String) : Exception(message)

class LegacyDevnetOperationFailed(val failureReason: GatewayFailureReason?, val status: TransactionStatus) :
    Exception(failureReason?.errorMessage ?: "Devnet operation failed")
