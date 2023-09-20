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

class AccountDetailsSerializer(val name: String) :
    JsonTransformingSerializer<AccountDetails>(AccountDetails.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        val accounts = element.jsonObject["alpha-goerli"]!!
        return accounts.jsonObject[name]!!
    }
}

class DevnetSetupFailedException(message: String) : Exception(message)

class LegacyDevnetOperationFailed(val failureReason: GatewayFailureReason?, val status: TransactionStatus) :
    Exception(failureReason?.errorMessage ?: "Devnet operation failed")
