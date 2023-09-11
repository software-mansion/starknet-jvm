package com.swmansion.starknet.data.types.transactions

import com.swmansion.starknet.data.serializers.JsonRpcTransactionTracePolymorphicSerializer
import com.swmansion.starknet.data.types.Calldata
import com.swmansion.starknet.data.types.EstimateFeeResponse
import com.swmansion.starknet.data.types.EventContent
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.MessageL2ToL1
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
    SKIP_EXECUTE("SKIP_EXECUTE"),
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
    val callerAddress: Felt?,

    @SerialName("class_hash")
    val classHash: Felt?,

    @SerialName("entry_point_type")
    val entryPointType: EntryPointType?,

    @SerialName("call_type")
    val callType: CallType?,

    @SerialName("result")
    val result: List<Felt>?,

    @SerialName("calls")
    val calls: List<FunctionInvocation>?,

    @SerialName("events")
    val events: List<EventContent>?,

    @SerialName("messages")
    val messages: List<MessageL2ToL1>?,
)

@Serializable
sealed class TransactionTrace()

@Serializable
data class InvokeTransactionTrace(
    @SerialName("validate_invocation")
    val validateInvocation: FunctionInvocation?,

    @SerialName("execute_invocation")
    val executeInvocation: FunctionInvocation?,

    @SerialName("fee_transfer_invocation")
    val feeTransferInvocation: FunctionInvocation?,
) : TransactionTrace()

@Serializable
data class DeclareTransactionTrace(
    @SerialName("validate_invocation")
    val validateInvocation: FunctionInvocation?,

    @SerialName("fee_transfer_invocation")
    val feeTransferInvocation: FunctionInvocation?,
) : TransactionTrace()

@Serializable
data class DeployAccountTransactionTrace(
    @SerialName("validate_invocation")
    val validateInvocation: FunctionInvocation?,

    @SerialName("constructor_invocation")
    val constructorInvocation: FunctionInvocation?,

    @SerialName("fee_transfer_invocation")
    val feeTransferInvocation: FunctionInvocation?,
) : TransactionTrace()

@Serializable
data class L1HandlerTransactionTrace(
    @SerialName("function_invocation")
    val functionInvocation: FunctionInvocation?,
) : TransactionTrace()

@Serializable
data class SimulatedTransaction(
    @SerialName("transaction_trace")
    @Serializable(with = JsonRpcTransactionTracePolymorphicSerializer::class)
    val transactionTrace: TransactionTrace,

    @SerialName("fee_estimation")
    val feeEstimation: EstimateFeeResponse,
)
