package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.serializers.TransactionTracePolymorphicSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class EntryPointType(val value: String) {
    EXTERNAL("EXTERNAL"),
    L1_HANDLER("L1_HANDLER"),
    CONSTRUCTOR("CONSTRUCTOR"),
}

@Serializable
enum class CallType(val value: String) {
    CALL("CALL"),
    LIBRARY_CALL("LIBRARY_CALL"),
    DELEGATE("DELEGATE"),
}

@Serializable
enum class SimulationFlag(val value: String) {
    SKIP_VALIDATE("SKIP_VALIDATE"),
    SKIP_FEE_CHARGE("SKIP_FEE_CHARGE"),
}

@Serializable
enum class SimulationFlagForEstimateFee(val value: String) {
    SKIP_VALIDATE("SKIP_VALIDATE"),
}

@Serializable
data class FunctionInvocation(
    @SerialName("contract_address")
    val contractAddress: Felt,

    @SerialName("entry_point_selector")
    val entrypoint: Felt,

    @SerialName("calldata")
    val calldata: Calldata,

    @SerialName("caller_address")
    val callerAddress: Felt,

    @SerialName("class_hash")
    val classHash: Felt,

    @SerialName("entry_point_type")
    val entryPointType: EntryPointType,

    @SerialName("call_type")
    val callType: CallType,

    @SerialName("result")
    val result: List<Felt>,

    @SerialName("calls")
    val calls: List<FunctionInvocation>,

    @SerialName("events")
    val events: List<OrderedEvent>,

    @SerialName("messages")
    val messages: List<OrderedMessageL2ToL1>,

    @SerialName("execution_resources")
    val computationResources: ComputationResources,
)

@Serializable
data class RevertedFunctionInvocation(
    @SerialName("revert_reason")
    val revertReason: String,
)

@Serializable
sealed class TransactionTrace {
    abstract val type: TransactionType
    abstract val stateDiff: StateDiff?
}

@Serializable
sealed class InvokeTransactionTraceBase : TransactionTrace() {
    @SerialName("validate_invocation")
    abstract val validateInvocation: FunctionInvocation?

    @SerialName("fee_transfer_invocation")
    abstract val feeTransferInvocation: FunctionInvocation?

    @SerialName("type")
    override val type: TransactionType = TransactionType.INVOKE
}

@Serializable
data class InvokeTransactionTrace(
    @SerialName("validate_invocation")
    override val validateInvocation: FunctionInvocation? = null,

    @SerialName("execute_invocation")
    val executeInvocation: FunctionInvocation,

    @SerialName("fee_transfer_invocation")
    override val feeTransferInvocation: FunctionInvocation? = null,

    @SerialName("state_diff")
    override val stateDiff: StateDiff? = null,

    @SerialName("execution_resources")
    val executionResources: ExecutionResources,
) : InvokeTransactionTraceBase()

@Serializable
data class RevertedInvokeTransactionTrace(
    @SerialName("validate_invocation")
    override val validateInvocation: FunctionInvocation? = null,

    @SerialName("execute_invocation")
    val executeInvocation: RevertedFunctionInvocation,

    @SerialName("fee_transfer_invocation")
    override val feeTransferInvocation: FunctionInvocation? = null,

    @SerialName("state_diff")
    override val stateDiff: StateDiff? = null,

    @SerialName("execution_resources")
    val executionResources: ExecutionResources,
) : InvokeTransactionTraceBase()

@Suppress("DataClassPrivateConstructor")
@Serializable
data class DeclareTransactionTrace private constructor(
    @SerialName("validate_invocation")
    val validateInvocation: FunctionInvocation? = null,

    @SerialName("fee_transfer_invocation")
    val feeTransferInvocation: FunctionInvocation? = null,

    @SerialName("state_diff")
    override val stateDiff: StateDiff? = null,

    @SerialName("execution_resources")
    val executionResources: ExecutionResources,

    @SerialName("type")
    override val type: TransactionType,
) : TransactionTrace() {
    @JvmOverloads
    constructor(
        validateInvocation: FunctionInvocation? = null,
        feeTransferInvocation: FunctionInvocation? = null,
        stateDiff: StateDiff? = null,
        executionResources: ExecutionResources,
    ) : this(
        validateInvocation = validateInvocation,
        feeTransferInvocation = feeTransferInvocation,
        stateDiff = stateDiff,
        executionResources = executionResources,
        type = TransactionType.DECLARE,
    )
}

@Suppress("DataClassPrivateConstructor")
@Serializable
data class DeployAccountTransactionTrace private constructor(
    @SerialName("validate_invocation")
    val validateInvocation: FunctionInvocation? = null,

    @SerialName("constructor_invocation")
    val constructorInvocation: FunctionInvocation,

    @SerialName("fee_transfer_invocation")
    val feeTransferInvocation: FunctionInvocation? = null,

    @SerialName("state_diff")
    override val stateDiff: StateDiff? = null,

    @SerialName("execution_resources")
    val executionResources: ExecutionResources,

    @SerialName("type")
    override val type: TransactionType,
) : TransactionTrace() {
    @JvmOverloads
    constructor(
        validateInvocation: FunctionInvocation? = null,
        constructorInvocation: FunctionInvocation,
        feeTransferInvocation: FunctionInvocation? = null,
        stateDiff: StateDiff? = null,
        executionResources: ExecutionResources,
    ) : this(
        validateInvocation = validateInvocation,
        constructorInvocation = constructorInvocation,
        feeTransferInvocation = feeTransferInvocation,
        stateDiff = stateDiff,
        executionResources = executionResources,
        type = TransactionType.DEPLOY_ACCOUNT,
    )
}

@Suppress("DataClassPrivateConstructor")
@Serializable
data class L1HandlerTransactionTrace private constructor(
    @SerialName("function_invocation")
    val functionInvocation: FunctionInvocation,

    @SerialName("state_diff")
    override val stateDiff: StateDiff? = null,

    @SerialName("type")
    override val type: TransactionType,
) : TransactionTrace() {
    @JvmOverloads
    constructor(
        functionInvocation: FunctionInvocation,
        stateDiff: StateDiff? = null,
    ) : this(
        functionInvocation = functionInvocation,
        stateDiff = stateDiff,
        type = TransactionType.L1_HANDLER,
    )
}

@Serializable
data class SimulatedTransaction(
    @SerialName("transaction_trace")
    @Serializable(with = TransactionTracePolymorphicSerializer::class)
    val transactionTrace: TransactionTrace,

    @SerialName("fee_estimation")
    val feeEstimation: EstimateFeeResponse,
)
