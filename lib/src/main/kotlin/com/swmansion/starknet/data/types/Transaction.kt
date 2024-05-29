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
data class DeployTransaction(
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
data class InvokeTransactionV1(
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
    override val version: TransactionVersion = TransactionVersion.V1,

    @SerialName("signature")
    override val signature: Signature,

    @SerialName("nonce")
    override val nonce: Felt,

) : InvokeTransaction(), DeprecatedTransaction {
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
data class InvokeTransactionV3(
    @SerialName("calldata")
    override val calldata: Calldata,

    @SerialName("sender_address")
    val senderAddress: Felt,

    // not in RPC spec, but returned alongside the transaction
    @SerialName("transaction_hash")
    override val hash: Felt? = null,

    @SerialName("version")
    override val version: TransactionVersion = TransactionVersion.V3,

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
data class InvokeTransactionV0(
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
data class DeclareTransactionV0(
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
data class DeclareTransactionV1(
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
data class DeclareTransactionV2(
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
    override val version: TransactionVersion = TransactionVersion.V2,

    @SerialName("signature")
    override val signature: Signature,

    @SerialName("nonce")
    override val nonce: Felt,

    @SerialName("compiled_class_hash")
    val compiledClassHash: Felt,

    @SerialName("contract_class")
    val contractDefinition: Cairo1ContractDefinition? = null,
) : DeclareTransaction(), DeprecatedTransaction {
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
data class DeclareTransactionV3(
    @SerialName("class_hash")
    override val classHash: Felt,

    @SerialName("sender_address")
    override val senderAddress: Felt,

    // not in RPC spec
    @SerialName("transaction_hash")
    override val hash: Felt? = null,

    @SerialName("version")
    override val version: TransactionVersion = TransactionVersion.V3,

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
    override val version: TransactionVersion = TransactionVersion.V1,

    @SerialName("signature")
    override val signature: Signature,

    @SerialName("nonce")
    override val nonce: Felt,
) : DeployAccountTransaction(), DeprecatedTransaction {
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
data class DeployAccountTransactionV3(
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
    override val version: TransactionVersion = TransactionVersion.V3,

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
    internal fun toPayload(): DeployAccountTransactionV3Payload {
        return DeployAccountTransactionV3Payload(
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

object TransactionFactory {
    private val tip = Uint64.ZERO
    private val paymasterData: PaymasterData = emptyList()
    private val accountDeploymentData: AccountDeploymentData = emptyList()
    private val nonceDataAvailabilityMode = DAMode.L1
    private val feeDataAvailabilityMode = DAMode.L1

    @JvmStatic
    @JvmOverloads
    fun makeInvokeV1Transaction(
        senderAddress: Felt,
        calldata: Calldata,
        chainId: StarknetChainId,
        nonce: Felt,
        maxFee: Felt,
        signature: Signature = emptyList(),
        version: TransactionVersion,
    ): InvokeTransactionV1 {
        val hash = TransactionHashCalculator.calculateInvokeTxV1Hash(
            contractAddress = senderAddress,
            calldata = calldata,
            chainId = chainId,
            version = version,
            nonce = nonce,
            maxFee = maxFee,
        )
        return InvokeTransactionV1(
            hash = hash,
            senderAddress = senderAddress,
            calldata = calldata,
            maxFee = maxFee,
            version = version,
            signature = signature,
            nonce = nonce,
        )
    }

    @JvmStatic
    @JvmOverloads
    fun makeInvokeV3Transaction(
        senderAddress: Felt,
        calldata: Calldata,
        chainId: StarknetChainId,
        nonce: Felt,
        signature: Signature = emptyList(),
        version: TransactionVersion,
        resourceBounds: ResourceBoundsMapping,
    ): InvokeTransactionV3 {
        val hash = TransactionHashCalculator.calculateInvokeTxV3Hash(
            senderAddress = senderAddress,
            calldata = calldata,
            chainId = chainId,
            version = version,
            nonce = nonce,
            resourceBounds = resourceBounds,
            tip = tip,
            paymasterData = paymasterData,
            accountDeploymentData = accountDeploymentData,
            nonceDataAvailabilityMode = nonceDataAvailabilityMode,
            feeDataAvailabilityMode = feeDataAvailabilityMode,
        )
        return InvokeTransactionV3(
            hash = hash,
            senderAddress = senderAddress,
            calldata = calldata,
            version = version,
            signature = signature,
            nonce = nonce,
            resourceBounds = resourceBounds,
            tip = tip,
            paymasterData = paymasterData,
            accountDeploymentData = accountDeploymentData,
            nonceDataAvailabilityMode = nonceDataAvailabilityMode,
            feeDataAvailabilityMode = feeDataAvailabilityMode,
        )
    }

    @JvmStatic
    @JvmOverloads
    fun makeDeployAccountV1Transaction(
        classHash: Felt,
        contractAddress: Felt,
        salt: Felt,
        calldata: Calldata,
        chainId: StarknetChainId,
        version: TransactionVersion,
        maxFee: Felt,
        signature: Signature = emptyList(),
        nonce: Felt = Felt.ZERO,
    ): DeployAccountTransactionV1 {
        val hash = TransactionHashCalculator.calculateDeployAccountV1TxHash(
            classHash = classHash,
            calldata = calldata,
            salt = salt,
            chainId = chainId,
            version = version,
            maxFee = maxFee,
            nonce = nonce,
        )
        return DeployAccountTransactionV1(
            classHash = classHash,
            contractAddress = contractAddress,
            contractAddressSalt = salt,
            constructorCalldata = calldata,
            version = version,
            nonce = nonce,
            maxFee = maxFee,
            hash = hash,
            signature = signature,
        )
    }

    @JvmStatic
    @JvmOverloads
    fun makeDeployAccountV3Transaction(
        classHash: Felt,
        senderAddress: Felt,
        salt: Felt,
        calldata: Calldata,
        chainId: StarknetChainId,
        version: TransactionVersion,
        signature: Signature = emptyList(),
        nonce: Felt = Felt.ZERO,
        resourceBounds: ResourceBoundsMapping,
    ): DeployAccountTransactionV3 {
        val hash = TransactionHashCalculator.calculateDeployAccountV3TxHash(
            classHash = classHash,
            salt = salt,
            constructorCalldata = calldata,
            chainId = chainId,
            version = version,
            nonce = nonce,
            resourceBounds = resourceBounds,
            tip = tip,
            paymasterData = paymasterData,
            nonceDataAvailabilityMode = nonceDataAvailabilityMode,
            feeDataAvailabilityMode = feeDataAvailabilityMode,
        )
        return DeployAccountTransactionV3(
            classHash = classHash,
            contractAddress = senderAddress,
            contractAddressSalt = salt,
            constructorCalldata = calldata,
            version = version,
            nonce = nonce,
            hash = hash,
            signature = signature,
            resourceBounds = resourceBounds,
            tip = tip,
            paymasterData = paymasterData,
            nonceDataAvailabilityMode = nonceDataAvailabilityMode,
            feeDataAvailabilityMode = feeDataAvailabilityMode,
        )
    }

    @JvmStatic
    @JvmOverloads
    fun makeDeclareV2Transaction(
        senderAddress: Felt,
        contractDefinition: Cairo1ContractDefinition,
        chainId: StarknetChainId,
        maxFee: Felt,
        version: TransactionVersion,
        nonce: Felt,
        casmContractDefinition: CasmContractDefinition,
        signature: Signature = emptyList(),
    ): DeclareTransactionV2 {
        val classHash = Cairo1ClassHashCalculator.computeSierraClassHash(contractDefinition)
        val compiledClassHash = Cairo1ClassHashCalculator.computeCasmClassHash(casmContractDefinition)
        val hash = TransactionHashCalculator.calculateDeclareV2TxHash(
            classHash = classHash,
            chainId = chainId,
            senderAddress = senderAddress,
            maxFee = maxFee,
            version = version,
            nonce = nonce,
            compiledClassHash = compiledClassHash,
        )
        return DeclareTransactionV2(
            hash = hash,
            classHash = classHash,
            senderAddress = senderAddress,
            contractDefinition = contractDefinition,
            maxFee = maxFee,
            version = version,
            signature = signature,
            nonce = nonce,
            compiledClassHash = compiledClassHash,
        )
    }

    @JvmStatic
    @JvmOverloads
    fun makeDeclareV3Transaction(
        senderAddress: Felt,
        contractDefinition: Cairo1ContractDefinition,
        chainId: StarknetChainId,
        version: TransactionVersion,
        nonce: Felt,
        casmContractDefinition: CasmContractDefinition,
        signature: Signature = emptyList(),
        resourceBounds: ResourceBoundsMapping,
    ): DeclareTransactionV3 {
        val classHash = Cairo1ClassHashCalculator.computeSierraClassHash(contractDefinition)
        val compiledClassHash = Cairo1ClassHashCalculator.computeCasmClassHash(casmContractDefinition)
        val hash = TransactionHashCalculator.calculateDeclareV3TxHash(
            classHash = classHash,
            chainId = chainId,
            senderAddress = senderAddress,
            version = version,
            nonce = nonce,
            compiledClassHash = compiledClassHash,
            resourceBounds = resourceBounds,
            tip = tip,
            paymasterData = paymasterData,
            accountDeploymentData = accountDeploymentData,
            nonceDataAvailabilityMode = nonceDataAvailabilityMode,
            feeDataAvailabilityMode = feeDataAvailabilityMode,
        )
        return DeclareTransactionV3(
            hash = hash,
            classHash = classHash,
            senderAddress = senderAddress,
            contractDefinition = contractDefinition,
            version = version,
            signature = signature,
            nonce = nonce,
            compiledClassHash = compiledClassHash,
            resourceBounds = resourceBounds,
            tip = tip,
            paymasterData = paymasterData,
            accountDeploymentData = accountDeploymentData,
            nonceDataAvailabilityMode = nonceDataAvailabilityMode,
            feeDataAvailabilityMode = feeDataAvailabilityMode,
        )
    }
}
