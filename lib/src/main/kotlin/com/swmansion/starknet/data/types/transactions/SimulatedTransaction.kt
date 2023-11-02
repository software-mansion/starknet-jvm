package com.swmansion.starknet.data.types.transactions

import com.swmansion.starknet.data.serializers.JsonRpcTransactionTracePolymorphicSerializer
import com.swmansion.starknet.data.types.Calldata
import com.swmansion.starknet.data.types.EstimateFeeResponse
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.OrderedEvent
import com.swmansion.starknet.data.types.OrderedMessageL2ToL1
import com.swmansion.starknet.data.types.StateDiff
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
}

@Serializable
enum class SimulationFlag(val value: String) {
    SKIP_VALIDATE("SKIP_VALIDATE"),
    SKIP_FEE_CHARGE("SKIP_FEE_CHARGE"),
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
)

@Serializable
data class RevertedFunctionInvocation(
    @SerialName("revert_reason")
    val revertReason: String,
)

@Serializable
sealed class TransactionTrace() {
    abstract val type: TransactionType
    abstract val stateDiff: StateDiff
}

@Serializable
sealed class InvokeTransactionTraceBase : TransactionTrace() {
    @SerialName("validate_invocation")
    abstract val validateInvocation: FunctionInvocation

    @SerialName("fee_transfer_invocation")
    abstract val feeTransferInvocation: FunctionInvocation?

    @SerialName("type")
    override val type: TransactionType = TransactionType.INVOKE
}

@Serializable
data class InvokeTransactionTrace(
    @SerialName("validate_invocation")
    override val validateInvocation: FunctionInvocation,

    @SerialName("execute_invocation")
    val executeInvocation: FunctionInvocation,

    @SerialName("fee_transfer_invocation")
    override val feeTransferInvocation: FunctionInvocation? = null,

    @SerialName("state_diff")
    override val stateDiff: StateDiff,
) : InvokeTransactionTraceBase()

@Serializable
data class RevertedInvokeTransactionTrace(
    @SerialName("validate_invocation")
    override val validateInvocation: FunctionInvocation,

    @SerialName("execute_invocation")
    val executeInvocation: RevertedFunctionInvocation,

    // TODO: (#344) make this field non-nullable
    @SerialName("fee_transfer_invocation")
    override val feeTransferInvocation: FunctionInvocation? = null,

    @SerialName("state_diff")
    override val stateDiff: StateDiff,
) : InvokeTransactionTraceBase()

@Serializable
data class DeclareTransactionTrace(
    @SerialName("validate_invocation")
    val validateInvocation: FunctionInvocation,

    // TODO: (#344) make this field non-nullable
    @SerialName("fee_transfer_invocation")
    val feeTransferInvocation: FunctionInvocation? = null,

    @SerialName("state_diff")
    override val stateDiff: StateDiff,

    @SerialName("type")
    override val type: TransactionType = TransactionType.DECLARE,
) : TransactionTrace()

@Serializable
data class DeployAccountTransactionTrace(
    @SerialName("validate_invocation")
    val validateInvocation: FunctionInvocation,

    @SerialName("constructor_invocation")
    val constructorInvocation: FunctionInvocation,

    // TODO: (#344) make this field non-nullable
    @SerialName("fee_transfer_invocation")
    val feeTransferInvocation: FunctionInvocation? = null,

    @SerialName("state_diff")
    override val stateDiff: StateDiff,

    @SerialName("type")
    override val type: TransactionType,
) : TransactionTrace()

@Serializable
data class L1HandlerTransactionTrace(
    @SerialName("function_invocation")
    val functionInvocation: FunctionInvocation,

    @SerialName("state_diff")
    override val stateDiff: StateDiff,

    @SerialName("type")
    override val type: TransactionType = TransactionType.L1_HANDLER,
) : TransactionTrace()

@Serializable
data class SimulatedTransaction(
    @SerialName("transaction_trace")
    @Serializable(with = JsonRpcTransactionTracePolymorphicSerializer::class)
    val transactionTrace: TransactionTrace,

    @SerialName("fee_estimation")
    val feeEstimation: EstimateFeeResponse,
)
