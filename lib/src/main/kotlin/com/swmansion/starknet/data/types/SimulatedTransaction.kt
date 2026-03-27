package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.serializers.TransactionTracePolymorphicSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.put

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
    @SerialName("SKIP_VALIDATE")
    SKIP_VALIDATE("SKIP_VALIDATE"),

    @SerialName("SKIP_FEE_CHARGE")
    SKIP_FEE_CHARGE("SKIP_FEE_CHARGE"),

    @SerialName("RETURN_INITIAL_READS")
    RETURN_INITIAL_READS("RETURN_INITIAL_READS"),
}

@Serializable
enum class SimulationFlagForEstimateFee(val value: String) {
    SKIP_VALIDATE("SKIP_VALIDATE"),
}

@Serializable
enum class TraceFlag(val value: String) {
    @SerialName("RETURN_INITIAL_READS")
    RETURN_INITIAL_READS("RETURN_INITIAL_READS"),
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
    val executionResources: InnerCallExecutionResources,

    @SerialName("is_reverted")
    val isReverted: Boolean,
)

@Serializable
data class RevertedFunctionInvocation(
    @SerialName("revert_reason")
    val revertReason: String,
)

@Serializable
sealed class TransactionTrace : StarknetResponse {
    abstract val type: TransactionType
    abstract val stateDiff: StateDiff?
}

@Serializable
sealed class L1HandlerTransactionTraceBase : TransactionTrace() {
    @SerialName("type")
    override val type: TransactionType = TransactionType.L1_HANDLER
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

@Serializable
data class L1HandlerTransactionTrace(
    @SerialName("function_invocation")
    val functionInvocation: FunctionInvocation,

    @SerialName("state_diff")
    override val stateDiff: StateDiff? = null,

    @SerialName("execution_resources")
    val executionResources: ExecutionResources,
) : L1HandlerTransactionTraceBase()

@Serializable
data class RevertedL1HandlerTransactionTrace(
    @SerialName("function_invocation")
    val functionInvocation: RevertedFunctionInvocation,

    @SerialName("state_diff")
    override val stateDiff: StateDiff? = null,

    @SerialName("execution_resources")
    val executionResources: ExecutionResources,
) : L1HandlerTransactionTraceBase()

@Serializable
data class SimulatedTransaction(
    @SerialName("transaction_trace")
    @Serializable(with = TransactionTracePolymorphicSerializer::class)
    val transactionTrace: TransactionTrace,

    @SerialName("fee_estimation")
    val feeEstimation: EstimateFeeResponse,
)

@Serializable
data class InitialReadsStorageEntry(
    @SerialName("contract_address")
    val contractAddress: Felt,

    @SerialName("key")
    val key: Felt,

    @SerialName("value")
    val value: Felt,
)

@Serializable
data class InitialReadsNonceEntry(
    @SerialName("contract_address")
    val contractAddress: Felt,

    @SerialName("nonce")
    val nonce: Felt,
)

@Serializable
data class InitialReadsClassHashEntry(
    @SerialName("contract_address")
    val contractAddress: Felt,

    @SerialName("class_hash")
    val classHash: Felt,
)

@Serializable
data class InitialReadsDeclaredContractEntry(
    @SerialName("class_hash")
    val classHash: Felt,

    @SerialName("is_declared")
    val isDeclared: Boolean,
)

@Serializable
data class InitialReads(
    @SerialName("storage")
    val storage: List<InitialReadsStorageEntry>? = null,

    @SerialName("nonces")
    val nonces: List<InitialReadsNonceEntry>? = null,

    @SerialName("class_hashes")
    val classHashes: List<InitialReadsClassHashEntry>? = null,

    @SerialName("declared_contracts")
    val declaredContracts: List<InitialReadsDeclaredContractEntry>? = null,
)

@Serializable
data class SimulatedTransactionWithInitialReads(
    @SerialName("simulated_transactions")
    val simulatedTransactions: List<SimulatedTransaction>,

    @SerialName("initial_reads")
    val initialReads: InitialReads,
) : StarknetResponse

@Serializable
data class BlockTransactionTrace(
    @SerialName("transaction_hash")
    val transactionHash: Felt,

    @Serializable(with = TransactionTracePolymorphicSerializer::class)
    @SerialName("trace_root")
    val traceRoot: TransactionTrace,
)

@Serializable(with = BlockTransactionTracesResultSerializer::class)
sealed class BlockTransactionTracesResult : StarknetResponse {
    abstract val traces: List<BlockTransactionTrace>

    data class Traces(
        override val traces: List<BlockTransactionTrace>,
    ) : BlockTransactionTracesResult()

    data class TracesWithInitialReads(
        override val traces: List<BlockTransactionTrace>,
        val initialReads: InitialReads,
    ) : BlockTransactionTracesResult()
}

object BlockTransactionTracesResultSerializer : KSerializer<BlockTransactionTracesResult> {
    private val listSerializer = ListSerializer(BlockTransactionTrace.serializer())

    override val descriptor = buildClassSerialDescriptor("BlockTransactionTracesResult")

    override fun serialize(encoder: Encoder, value: BlockTransactionTracesResult) {
        val output = encoder as? JsonEncoder
            ?: throw SerializationException("This serializer can only be used with JSON")

        when (value) {
            is BlockTransactionTracesResult.TracesWithInitialReads -> {
                val jsonObject = buildJsonObject {
                    put("traces", output.json.encodeToJsonElement(listSerializer, value.traces))
                    put("initial_reads", output.json.encodeToJsonElement(InitialReads.serializer(), value.initialReads))
                }
                output.encodeJsonElement(jsonObject)
            }
            is BlockTransactionTracesResult.Traces -> {
                val jsonArray = output.json.encodeToJsonElement(listSerializer, value.traces)
                output.encodeJsonElement(jsonArray)
            }
        }
    }

    override fun deserialize(decoder: Decoder): BlockTransactionTracesResult {
        val input = decoder as? JsonDecoder
            ?: throw SerializationException("This serializer can only be used with JSON")

        val element = input.decodeJsonElement()

        return when {
            element is JsonArray -> {
                // Format without RETURN_INITIAL_READS: [{"transaction_hash": ..., "trace_root": ...}, ...]
                BlockTransactionTracesResult.Traces(
                    input.json.decodeFromJsonElement(listSerializer, element),
                )
            }
            element is JsonObject && element.containsKey("traces") -> {
                // Format with RETURN_INITIAL_READS: {"traces": [...], "initial_reads": {...}}
                val traces = input.json.decodeFromJsonElement(listSerializer, element.getValue("traces"))
                val initialReads = input.json.decodeFromJsonElement(InitialReads.serializer(), element.getValue("initial_reads"))
                BlockTransactionTracesResult.TracesWithInitialReads(traces, initialReads)
            }
            else -> throw SerializationException("Unexpected JSON format for BlockTransactionTracesResult")
        }
    }
}
