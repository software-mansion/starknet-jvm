package starknet.utils.data

import com.swmansion.starknet.data.types.Felt
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import starknet.utils.data.serializers.SnCastResponsePolymorphicSerializer

@OptIn(ExperimentalSerializationApi::class)
@Serializable
enum class SnCastCommand {
    @JsonNames("account create")
    ACCOUNT_CREATE,

    @JsonNames("account deploy")
    ACCOUNT_DEPLOY,

    @JsonNames("declare")
    DECLARE,

    @JsonNames("deploy")
    DEPLOY,

    @JsonNames("invoke")
    INVOKE,
}

@Serializable(with = SnCastResponsePolymorphicSerializer::class)
sealed class SnCastResponse {
    abstract val command: SnCastCommand
    abstract val error: String?
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class AccountDeploySnCastResponse(
    @JsonNames("command")
    override val command: SnCastCommand = SnCastCommand.ACCOUNT_DEPLOY,

    @JsonNames("error")
    override val error: String? = null,

    @JsonNames("transaction_hash")
    val transactionHash: Felt,
) : SnCastResponse()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class AccountCreateSnCastResponse(
    @JsonNames("command")
    override val command: SnCastCommand = SnCastCommand.ACCOUNT_CREATE,

    @JsonNames("error")
    override val error: String? = null,

    @JsonNames("address")
    val accountAddress: Felt,

    @JsonNames("max_fee")
    val maxFee: Felt,
) : SnCastResponse()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class DeclareSnCastResponse(
    @JsonNames("command")
    override val command: SnCastCommand = SnCastCommand.DECLARE,

    @JsonNames("error")
    override val error: String? = null,

    @JsonNames("class_hash")
    val classHash: Felt,

    @JsonNames("transaction_hash")
    val transactionHash: Felt,
) : SnCastResponse()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class DeploySnCastResponse(
    @JsonNames("command")
    override val command: SnCastCommand = SnCastCommand.DEPLOY,

    @JsonNames("error")
    override val error: String? = null,

    @JsonNames("contract_address")
    val contractAddress: Felt,

    @JsonNames("transaction_hash")
    val transactionHash: Felt,
) : SnCastResponse()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class InvokeSnCastResponse(
    @JsonNames("command")
    override val command: SnCastCommand = SnCastCommand.INVOKE,

    @JsonNames("error")
    override val error: String? = null,

    @JsonNames("transaction_hash")
    val transactionHash: Felt,
) : SnCastResponse()

class SnCastCommandFailed(val commandName: String, val error: String?) :
    Exception("Command $commandName failed. Error reason: $error")
