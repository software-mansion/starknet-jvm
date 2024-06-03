package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.Cairo1ClassHashCalculator
import com.swmansion.starknet.data.TransactionHashCalculator
import com.swmansion.starknet.provider.Provider
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
enum class TransactionType(val txPrefix: Felt) {
    @SerialName("DECLARE")
    DECLARE(Felt.fromHex("0x6465636c617265")), // encodeShortString('declare'),

    @SerialName("DEPLOY")
    DEPLOY(Felt.fromHex("0x6465706c6f79")), // encodeShortString('deploy'),

    @SerialName("DEPLOY_ACCOUNT")
    DEPLOY_ACCOUNT(Felt.fromHex("0x6465706c6f795f6163636f756e74")), // encodeShortString('deploy_account'),

    @SerialName("INVOKE")
    @JsonNames("INVOKE", "INVOKE_FUNCTION")
    INVOKE(Felt.fromHex("0x696e766f6b65")), // encodeShortString('invoke'),

    @SerialName("L1_HANDLER")
    L1_HANDLER(Felt.fromHex("0x6c315f68616e646c6572")), // encodeShortString('l1_handler')
}

/**
 * The version of the transaction.
 *
 * The version is used to determine the transaction format and is used for transaction signing.
 *
 * Standard versions include [V0], [V1], [V2], and [V3]. These are utilized for regular transaction execution.
 * Query versions [V1_QUERY], [V2_QUERY], and [V3_QUERY] should only be used for creating transactions to be used
 * in queries that do not alter the chain state, as in methods like [Provider.simulateTransactions] and [Provider.getEstimateFee].
 * Sending transaction with a query version for execution will result in a failure.
 */
@Serializable
enum class TransactionVersion(val value: Felt) {
    @SerialName("0x0")
    V0(Felt.ZERO),

    @SerialName("0x1")
    V1(Felt.ONE),

    @SerialName("0x100000000000000000000000000000001")
    V1_QUERY(Felt.fromHex("0x100000000000000000000000000000001")),

    @SerialName("0x2")
    V2(Felt(2)),

    @SerialName("0x100000000000000000000000000000002")
    V2_QUERY(Felt.fromHex("0x100000000000000000000000000000002")),

    @SerialName("0x3")
    V3(Felt(3)),

    @SerialName("0x100000000000000000000000000000003")
    V3_QUERY(Felt.fromHex("0x100000000000000000000000000000003")),
}

@Serializable
enum class DAMode(val value: Int) {
    @SerialName("L1")
    L1(0),

    @SerialName("L2")
    L2(1),
}

@Serializable
sealed class Transaction : StarknetResponse {
    abstract val hash: Felt?
    abstract val version: TransactionVersion
    abstract val signature: Signature
    abstract val nonce: Felt
    abstract val type: TransactionType
}

@Serializable
sealed interface DeprecatedTransaction {
    @SerialName("version")
    val version: TransactionVersion

    @SerialName("signature")
    val signature: Signature

    @SerialName("nonce")
    val nonce: Felt

    @SerialName("type")
    val type: TransactionType

    @SerialName("max_fee")
    val maxFee: Felt
}

@Serializable
sealed interface TransactionV3 {
    @SerialName("version")
    val version: TransactionVersion

    @SerialName("signature")
    val signature: Signature

    @SerialName("nonce")
    val nonce: Felt

    @SerialName("type")
    val type: TransactionType

    @SerialName("resource_bounds")
    val resourceBounds: ResourceBoundsMapping

    @SerialName("tip")
    val tip: Uint64

    @SerialName("paymaster_data")
    val paymasterData: List<Felt>

    @SerialName("nonce_data_availability_mode")
    val nonceDataAvailabilityMode: DAMode

    @SerialName("fee_data_availability_mode")
    val feeDataAvailabilityMode: DAMode
}

@Serializable
@SerialName("DEPLOY")
data class DeployTransaction @JvmOverloads internal constructor(
    @SerialName("contract_address_salt")
    val contractAddressSalt: Felt,

    @SerialName("constructor_calldata")
    val constructorCalldata: Calldata,

    @SerialName("class_hash")
    val classHash: Felt,

    // not in RPC spec, but returned alongside the transaction
    @SerialName("transaction_hash")
    override val hash: Felt? = null,

    // not in RPC spec
    @SerialName("max_fee")
    override val maxFee: Felt = Felt.ZERO,

    @SerialName("version")
    override val version: TransactionVersion,

    // not in RPC spec
    @SerialName("signature")
    override val signature: Signature = emptyList(),

    @SerialName("nonce")
    override val nonce: Felt = Felt.ZERO,

    @SerialName("type")
    override val type: TransactionType = TransactionType.DEPLOY,
) : Transaction(), DeprecatedTransaction

@Serializable
@SerialName("INVOKE_FUNCTION")
sealed class InvokeTransaction : Transaction() {
    abstract val calldata: Calldata
    override val type: TransactionType = TransactionType.INVOKE
}

@Serializable
data class InvokeTransactionV1 internal constructor(
    @SerialName("calldata")
    override val calldata: Calldata,

    @SerialName("sender_address")
    val senderAddress: Felt,

    // not in RPC spec, but returned alongside the transaction
    @SerialName("transaction_hash")
    override val hash: Felt? = null,

    @SerialName("max_fee")
    override val maxFee: Felt,

    @SerialName("version")
    override val version: TransactionVersion,

    @SerialName("signature")
    override val signature: Signature,

    @SerialName("nonce")
    override val nonce: Felt,

) : InvokeTransaction(), DeprecatedTransaction {
    @JvmOverloads
    constructor(
        senderAddress: Felt,
        calldata: Calldata,
        chainId: StarknetChainId,
        nonce: Felt,
        maxFee: Felt,
        signature: Signature = emptyList(),
        forFeeEstimate: Boolean = false,
    ) : this(
        hash = TransactionHashCalculator.calculateInvokeTxV1Hash(
            contractAddress = senderAddress,
            calldata = calldata,
            chainId = chainId,
            version = if (forFeeEstimate) TransactionVersion.V1_QUERY else TransactionVersion.V1,
            nonce = nonce,
            maxFee = maxFee,
        ),
        senderAddress = senderAddress,
        calldata = calldata,
        maxFee = maxFee,
        version = if (forFeeEstimate) TransactionVersion.V1_QUERY else TransactionVersion.V1,
        signature = signature,
        nonce = nonce,
    )

    fun toPayload(): InvokeTransactionV1Payload {
        return InvokeTransactionV1Payload(
            calldata = calldata,
            signature = signature,
            maxFee = maxFee,
            nonce = nonce,
            senderAddress = senderAddress,
            version = version,
        )
    }
}

@Serializable
data class InvokeTransactionV3 internal constructor(
    @SerialName("calldata")
    override val calldata: Calldata,

    @SerialName("sender_address")
    val senderAddress: Felt,

    // not in RPC spec, but returned alongside the transaction
    @SerialName("transaction_hash")
    override val hash: Felt? = null,

    @SerialName("version")
    override val version: TransactionVersion,

    @SerialName("signature")
    override val signature: Signature,

    @SerialName("nonce")
    override val nonce: Felt,

    @SerialName("resource_bounds")
    override val resourceBounds: ResourceBoundsMapping,

    @SerialName("tip")
    override val tip: Uint64,

    @SerialName("paymaster_data")
    override val paymasterData: List<Felt>,

    @SerialName("account_deployment_data")
    val accountDeploymentData: List<Felt>,

    @SerialName("nonce_data_availability_mode")
    override val nonceDataAvailabilityMode: DAMode,

    @SerialName("fee_data_availability_mode")
    override val feeDataAvailabilityMode: DAMode,
) : InvokeTransaction(), TransactionV3 {
    @JvmOverloads
    constructor(
        senderAddress: Felt,
        calldata: Calldata,
        chainId: StarknetChainId,
        nonce: Felt,
        signature: Signature = emptyList(),
        forFeeEstimate: Boolean = false,
        resourceBounds: ResourceBoundsMapping,
    ) : this(
        hash = TransactionHashCalculator.calculateInvokeTxV3Hash(
            senderAddress = senderAddress,
            calldata = calldata,
            chainId = chainId,
            version = if (forFeeEstimate) TransactionVersion.V3_QUERY else TransactionVersion.V3,
            nonce = nonce,
            resourceBounds = resourceBounds,
            tip = Uint64.ZERO,
            paymasterData = emptyList(),
            accountDeploymentData = emptyList(),
            nonceDataAvailabilityMode = DAMode.L1,
            feeDataAvailabilityMode = DAMode.L1,
        ),
        senderAddress = senderAddress,
        calldata = calldata,
        version = if (forFeeEstimate) TransactionVersion.V3_QUERY else TransactionVersion.V3,
        signature = signature,
        nonce = nonce,
        resourceBounds = resourceBounds,
        tip = Uint64.ZERO,
        paymasterData = emptyList(),
        accountDeploymentData = emptyList(),
        nonceDataAvailabilityMode = DAMode.L1,
        feeDataAvailabilityMode = DAMode.L1,
    )

    fun toPayload(): InvokeTransactionV3Payload {
        return InvokeTransactionV3Payload(
            calldata = calldata,
            signature = signature,
            nonce = nonce,
            senderAddress = senderAddress,
            version = version,
            resourceBounds = resourceBounds,
        )
    }
}

@Serializable
data class InvokeTransactionV0 @JvmOverloads internal constructor(
    @SerialName("calldata")
    override val calldata: Calldata,

    // not in RPC spec, but returned alongside the transaction
    @SerialName("transaction_hash")
    override val hash: Felt? = null,

    @SerialName("max_fee")
    val maxFee: Felt,

    @SerialName("version")
    override val version: TransactionVersion = TransactionVersion.V0,

    @SerialName("signature")
    override val signature: Signature,

    // not in RPC spec
    @SerialName("nonce")
    override val nonce: Felt = Felt.ZERO,

    @SerialName("contract_address")
    val contractAddress: Felt,

    @SerialName("entry_point_selector")
    val entryPointSelector: Felt,
) : InvokeTransaction()

@Serializable
@SerialName("DECLARE")
sealed class DeclareTransaction : Transaction() {
    abstract val classHash: Felt
    abstract val senderAddress: Felt
    override val type: TransactionType = TransactionType.DECLARE
}

@Serializable
data class DeclareTransactionV0 @JvmOverloads internal constructor(
    @SerialName("class_hash")
    override val classHash: Felt,

    @SerialName("sender_address")
    override val senderAddress: Felt,

    // not in RPC spec, but returned alongside the transaction
    @SerialName("transaction_hash")
    override val hash: Felt? = null,

    @SerialName("max_fee")
    override val maxFee: Felt,

    @SerialName("version")
    override val version: TransactionVersion = TransactionVersion.V0,

    @SerialName("signature")
    override val signature: Signature,

    @SerialName("nonce")
    override val nonce: Felt = Felt.ZERO,

    @SerialName("contract_class")
    val contractDefinition: Cairo0ContractDefinition? = null,
) : DeclareTransaction(), DeprecatedTransaction

@Serializable
data class DeclareTransactionV1 @JvmOverloads internal constructor(
    @SerialName("class_hash")
    override val classHash: Felt,

    @SerialName("sender_address")
    override val senderAddress: Felt,

    // not in RPC spec, but returned alongside the transaction
    @SerialName("transaction_hash")
    override val hash: Felt? = null,

    @SerialName("max_fee")
    override val maxFee: Felt,

    @SerialName("version")
    override val version: TransactionVersion = TransactionVersion.V1,

    @SerialName("signature")
    override val signature: Signature,

    @SerialName("nonce")
    override val nonce: Felt,

    @SerialName("contract_class")
    val contractDefinition: Cairo0ContractDefinition? = null,
) : DeclareTransaction(), DeprecatedTransaction

@Serializable
data class DeclareTransactionV2 @JvmOverloads internal constructor(
    @SerialName("class_hash")
    override val classHash: Felt,

    @SerialName("sender_address")
    override val senderAddress: Felt,

    // not in RPC spec, but returned alongside the transaction
    @SerialName("transaction_hash")
    override val hash: Felt? = null,

    @SerialName("max_fee")
    override val maxFee: Felt,

    @SerialName("version")
    override val version: TransactionVersion,

    @SerialName("signature")
    override val signature: Signature,

    @SerialName("nonce")
    override val nonce: Felt,

    @SerialName("compiled_class_hash")
    val compiledClassHash: Felt,

    @SerialName("contract_class")
    val contractDefinition: Cairo1ContractDefinition? = null,
) : DeclareTransaction(), DeprecatedTransaction {
    @JvmOverloads
    constructor(
        senderAddress: Felt,
        contractDefinition: Cairo1ContractDefinition,
        chainId: StarknetChainId,
        maxFee: Felt,
        nonce: Felt,
        casmContractDefinition: CasmContractDefinition,
        signature: Signature = emptyList(),
        forFeeEstimate: Boolean = false,
    ) : this(
        hash = TransactionHashCalculator.calculateDeclareV2TxHash(
            classHash = Cairo1ClassHashCalculator.computeSierraClassHash(contractDefinition),
            chainId = chainId,
            senderAddress = senderAddress,
            maxFee = maxFee,
            version = if (forFeeEstimate) TransactionVersion.V2_QUERY else TransactionVersion.V2,
            nonce = nonce,
            compiledClassHash = Cairo1ClassHashCalculator.computeCasmClassHash(casmContractDefinition),
        ),
        classHash = Cairo1ClassHashCalculator.computeSierraClassHash(contractDefinition),
        senderAddress = senderAddress,
        contractDefinition = contractDefinition,
        maxFee = maxFee,
        version = if (forFeeEstimate) TransactionVersion.V2_QUERY else TransactionVersion.V2,
        signature = signature,
        nonce = nonce,
        compiledClassHash = Cairo1ClassHashCalculator.computeCasmClassHash(casmContractDefinition),
    )

    @Throws(ConvertingToPayloadFailedException::class)
    internal fun toPayload(): DeclareTransactionV2Payload {
        contractDefinition ?: throw ConvertingToPayloadFailedException()
        return DeclareTransactionV2Payload(
            contractDefinition = contractDefinition,
            senderAddress = senderAddress,
            maxFee = maxFee,
            nonce = nonce,
            signature = signature,
            compiledClassHash = compiledClassHash,
            version = version,
        )
    }

    internal class ConvertingToPayloadFailedException : RuntimeException()
}

@Serializable
data class DeclareTransactionV3 @JvmOverloads constructor(
    @SerialName("class_hash")
    override val classHash: Felt,

    @SerialName("sender_address")
    override val senderAddress: Felt,

    // not in RPC spec
    @SerialName("transaction_hash")
    override val hash: Felt,

    @SerialName("version")
    override val version: TransactionVersion,

    @SerialName("signature")
    override val signature: Signature,

    @SerialName("nonce")
    override val nonce: Felt,

    @SerialName("resource_bounds")
    override val resourceBounds: ResourceBoundsMapping,

    @SerialName("tip")
    override val tip: Uint64,

    @SerialName("paymaster_data")
    override val paymasterData: List<Felt>,

    @SerialName("account_deployment_data")
    val accountDeploymentData: List<Felt>,

    @SerialName("nonce_data_availability_mode")
    override val nonceDataAvailabilityMode: DAMode,

    @SerialName("fee_data_availability_mode")
    override val feeDataAvailabilityMode: DAMode,

    @SerialName("compiled_class_hash")
    val compiledClassHash: Felt,

    @SerialName("contract_class")
    val contractDefinition: Cairo1ContractDefinition? = null,
) : DeclareTransaction(), TransactionV3 {
    constructor(
        senderAddress: Felt,
        contractDefinition: Cairo1ContractDefinition,
        chainId: StarknetChainId,
        forFeeEstimate: Boolean = false,
        nonce: Felt,
        casmContractDefinition: CasmContractDefinition,
        signature: Signature = emptyList(),
        resourceBounds: ResourceBoundsMapping,
    ) : this(
        hash = TransactionHashCalculator.calculateDeclareV3TxHash(
            classHash = Cairo1ClassHashCalculator.computeSierraClassHash(contractDefinition),
            chainId = chainId,
            senderAddress = senderAddress,
            version = if (forFeeEstimate) TransactionVersion.V3_QUERY else TransactionVersion.V3,
            nonce = nonce,
            compiledClassHash = Cairo1ClassHashCalculator.computeCasmClassHash(casmContractDefinition),
            resourceBounds = resourceBounds,
            tip = Uint64.ZERO,
            paymasterData = emptyList(),
            accountDeploymentData = emptyList(),
            nonceDataAvailabilityMode = DAMode.L1,
            feeDataAvailabilityMode = DAMode.L1,
        ),
        classHash = Cairo1ClassHashCalculator.computeSierraClassHash(contractDefinition),
        senderAddress = senderAddress,
        contractDefinition = contractDefinition,
        version = if (forFeeEstimate) TransactionVersion.V3_QUERY else TransactionVersion.V3,
        signature = signature,
        nonce = nonce,
        compiledClassHash = Cairo1ClassHashCalculator.computeCasmClassHash(casmContractDefinition),
        resourceBounds = resourceBounds,
        tip = Uint64.ZERO,
        paymasterData = emptyList(),
        accountDeploymentData = emptyList(),
        nonceDataAvailabilityMode = DAMode.L1,
        feeDataAvailabilityMode = DAMode.L1,
    )

    @Throws(ConvertingToPayloadFailedException::class)
    internal fun toPayload(): DeclareTransactionV3Payload {
        contractDefinition ?: throw ConvertingToPayloadFailedException()
        return DeclareTransactionV3Payload(
            contractDefinition = contractDefinition,
            senderAddress = senderAddress,
            nonce = nonce,
            resourceBounds = resourceBounds,
            signature = signature,
            compiledClassHash = compiledClassHash,
            version = version,
        )
    }

    internal class ConvertingToPayloadFailedException : RuntimeException()
}

@Serializable
@SerialName("L1_HANDLER")
data class L1HandlerTransaction(
    @SerialName("contract_address")
    val contractAddress: Felt,

    @SerialName("calldata")
    val calldata: Calldata,

    @SerialName("entry_point_selector")
    val entryPointSelector: Felt,

    // not in RPC spec, but returned alongside the transaction
    @SerialName("transaction_hash")
    override val hash: Felt? = null,

    @SerialName("max_fee")
    override val maxFee: Felt = Felt.ZERO,

    @SerialName("version")
    override val version: TransactionVersion = TransactionVersion.V0,

    @SerialName("signature")
    override val signature: Signature = emptyList(),

    @SerialName("nonce")
    override val nonce: Felt,

    @SerialName("type")
    override val type: TransactionType = TransactionType.L1_HANDLER,
) : Transaction(), DeprecatedTransaction

@Serializable
@SerialName("DEPLOY_ACCOUNT")
sealed class DeployAccountTransaction : Transaction() {
    @SerialName("class_hash")
    abstract val classHash: Felt

    @SerialName("contract_address")
    abstract val contractAddress: Felt

    @SerialName("contract_address_salt")
    abstract val contractAddressSalt: Felt

    @SerialName("constructor_calldata")
    abstract val constructorCalldata: Calldata

    @SerialName("type")
    override val type: TransactionType = TransactionType.DEPLOY_ACCOUNT
}

@Serializable
data class DeployAccountTransactionV1(
    @SerialName("class_hash")
    override val classHash: Felt,

    // not in RPC spec, can be removed in the future
    @SerialName("contract_address")
    override val contractAddress: Felt = Felt.ZERO,

    @SerialName("contract_address_salt")
    override val contractAddressSalt: Felt,

    @SerialName("constructor_calldata")
    override val constructorCalldata: Calldata,

    // not in RPC spec, but returned alongside the transaction
    @SerialName("transaction_hash")
    override val hash: Felt? = null,

    @SerialName("max_fee")
    override val maxFee: Felt,

    @SerialName("version")
    override val version: TransactionVersion,

    @SerialName("signature")
    override val signature: Signature,

    @SerialName("nonce")
    override val nonce: Felt,
) : DeployAccountTransaction(), DeprecatedTransaction {
    @JvmOverloads
    constructor(
        classHash: Felt,
        contractAddress: Felt,
        salt: Felt,
        calldata: Calldata,
        chainId: StarknetChainId,
        forFeeEstimate: Boolean = false,
        maxFee: Felt,
        signature: Signature = emptyList(),
        nonce: Felt = Felt.ZERO,
    ) : this(
        classHash = classHash,
        contractAddress = contractAddress,
        contractAddressSalt = salt,
        constructorCalldata = calldata,
        version = if (forFeeEstimate) TransactionVersion.V1_QUERY else TransactionVersion.V1,
        nonce = nonce,
        maxFee = maxFee,
        hash = TransactionHashCalculator.calculateDeployAccountV1TxHash(
            classHash = classHash,
            calldata = calldata,
            salt = salt,
            chainId = chainId,
            version = if (forFeeEstimate) TransactionVersion.V1_QUERY else TransactionVersion.V1,
            maxFee = maxFee,
            nonce = nonce,
        ),
        signature = signature,
    )

    internal fun toPayload(): DeployAccountTransactionV1Payload {
        return DeployAccountTransactionV1Payload(
            classHash = classHash,
            salt = contractAddressSalt,
            constructorCalldata = constructorCalldata,
            version = version,
            nonce = nonce,
            maxFee = maxFee,
            signature = signature,
        )
    }
}

@Serializable
data class DeployAccountTransactionV3 @JvmOverloads internal constructor(
    @SerialName("class_hash")
    override val classHash: Felt,

    // not in RPC spec, can be removed in the future
    @SerialName("contract_address")
    override val contractAddress: Felt = Felt.ZERO,

    @SerialName("contract_address_salt")
    override val contractAddressSalt: Felt,

    @SerialName("constructor_calldata")
    override val constructorCalldata: Calldata,

    // not in RPC spec, but returned alongside the transaction
    @SerialName("transaction_hash")
    override val hash: Felt? = null,

    @SerialName("version")
    override val version: TransactionVersion,

    @SerialName("signature")
    override val signature: Signature,

    @SerialName("nonce")
    override val nonce: Felt,

    @SerialName("resource_bounds")
    override val resourceBounds: ResourceBoundsMapping,

    @SerialName("tip")
    override val tip: Uint64,

    @SerialName("paymaster_data")
    override val paymasterData: List<Felt>,

    @SerialName("nonce_data_availability_mode")
    override val nonceDataAvailabilityMode: DAMode,

    @SerialName("fee_data_availability_mode")
    override val feeDataAvailabilityMode: DAMode,
) : DeployAccountTransaction(), TransactionV3 {
    @JvmOverloads
    constructor(
        classHash: Felt,
        senderAddress: Felt,
        salt: Felt,
        calldata: Calldata,
        chainId: StarknetChainId,
        forFeeEstimate: Boolean = false,
        signature: Signature = emptyList(),
        nonce: Felt = Felt.ZERO,
        resourceBounds: ResourceBoundsMapping,
    ) : this(
        classHash = classHash,
        contractAddress = senderAddress,
        contractAddressSalt = salt,
        constructorCalldata = calldata,
        version = if (forFeeEstimate) TransactionVersion.V3_QUERY else TransactionVersion.V3,
        nonce = nonce,
        hash = TransactionHashCalculator.calculateDeployAccountV3TxHash(
            classHash = classHash,
            salt = salt,
            constructorCalldata = calldata,
            chainId = chainId,
            version = if (forFeeEstimate) TransactionVersion.V3_QUERY else TransactionVersion.V3,
            nonce = nonce,
            resourceBounds = resourceBounds,
            tip = Uint64.ZERO,
            paymasterData = emptyList(),
            nonceDataAvailabilityMode = DAMode.L1,
            feeDataAvailabilityMode = DAMode.L1,
        ),
        signature = signature,
        resourceBounds = resourceBounds,
        tip = Uint64.ZERO,
        paymasterData = emptyList(),
        nonceDataAvailabilityMode = DAMode.L1,
        feeDataAvailabilityMode = DAMode.L1,
    )

    internal fun toPayload() = DeployAccountTransactionV3Payload(
            classHash = classHash,
            salt = contractAddressSalt,
            constructorCalldata = constructorCalldata,
            version = version,
            nonce = nonce,
            signature = signature,
            resourceBounds = resourceBounds,
        )
    }
}
