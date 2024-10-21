package com.swmansion.starknet.provider.rpc

import com.swmansion.starknet.data.serializers.ContractExecutionErrorPolymorphicSerializer
import com.swmansion.starknet.data.types.Felt
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

@Serializable
sealed interface JsonRpcError {
    @SerialName("code")
    val code: Int

    @SerialName("message")
    val message: String
}

@Suppress("DataClassPrivateConstructor")
@Serializable
data class FailedToReceiveTransactionError private constructor(
    override val code: Int,
    override val message: String,
) : JsonRpcError {
    constructor() : this(
        code = 1,
        message = "Failed to write transaction",
    )
}

@Suppress("DataClassPrivateConstructor")
@Serializable
data class ContractNotFoundError private constructor(
    override val code: Int,
    override val message: String,

    ) : JsonRpcError {
    constructor() : this(
        code = 20,
        message = "Contract not found",
    )
}

@Suppress("DataClassPrivateConstructor")
@Serializable
data class BlockNotFoundError private constructor(
    override val code: Int,
    override val message: String,

    ) : JsonRpcError {
    constructor() : this(
        code = 24,
        message = "Block not found",
    )
}

@Suppress("DataClassPrivateConstructor")
@Serializable
data class InvalidTransactionIndexError private constructor(
    override val code: Int,
    override val message: String,

    ) : JsonRpcError {
    constructor() : this(
        code = 27,
        message = "Invalid transaction index in a block",
    )
}

@Suppress("DataClassPrivateConstructor")
@Serializable
data class ClassHashNotFoundError private constructor(
    override val code: Int,
    override val message: String,

    ) : JsonRpcError {
    constructor() : this(
        code = 28,
        message = "Class hash not found",
    )
}

@Suppress("DataClassPrivateConstructor")
@Serializable
data class TransactionHashNotFoundError private constructor(
    override val code: Int,
    override val message: String,

    ) : JsonRpcError {
    constructor() : this(
        code = 29,
        message = "Transaction hash not found",
    )
}

@Suppress("DataClassPrivateConstructor")
@Serializable
data class PageSizeTooBigError private constructor(
    override val code: Int,
    override val message: String,

    ) : JsonRpcError {
    constructor() : this(
        code = 31,
        message = "Requested page size is too big",
    )
}

@Suppress("DataClassPrivateConstructor")
@Serializable
data class NoBlocksError private constructor(
    override val code: Int,
    override val message: String,

    ) : JsonRpcError {
    constructor() : this(
        code = 32,
        message = "There are no blocks",
    )
}

@Suppress("DataClassPrivateConstructor")
@Serializable
data class InvalidContinuationTokenError private constructor(
    override val code: Int,
    override val message: String,

    ) : JsonRpcError {
    constructor() : this(
        code = 33,
        message = "The supplied continuation token is invalid or unknown",
    )
}

@Suppress("DataClassPrivateConstructor")
@Serializable
data class TooManyKeysInFilterError private constructor(
    override val code: Int,
    override val message: String,
) : JsonRpcError {
    constructor() : this(
        code = 34,
        message = "Too many keys provided in a filter",
    )
}

@Suppress("DataClassPrivateConstructor")
@Serializable
data class ContractError private constructor(
    override val code: Int,
    override val message: String,
    val data: ContractExecutionErrorData,
) : JsonRpcError {
    constructor(data: String) : this(
        code = 40,
        message = "Contract error",
        data = Json.decodeFromString(data),
    )
}

@Serializable
data class ContractExecutionErrorData(
    @SerialName("revert_error")
    val error: ContractExecutionError,
)

@Suppress("DataClassPrivateConstructor")
@Serializable
data class TransactionExecutionError private constructor(
    override val code: Int,
    override val message: String,
    val data: TransactionExecutionErrorData,
) : JsonRpcError {
    constructor(data: String) : this(
        code = 41,
        message = "Transaction execution error",
        data = Json.decodeFromString(data),
    )
}

@Serializable
data class TransactionExecutionErrorData(
    @SerialName("transaction_index")
    val transactionIndex: Int,

    @SerialName("execution_error")
    val error: ContractExecutionError,
)


@Serializable(with = ContractExecutionErrorPolymorphicSerializer::class)
sealed class ContractExecutionError {
    @Serializable
    data class InnerCall(
        @SerialName("contract_address")
        val contractAddress: Felt,

        @SerialName("class_hash")
        val classHash: Felt,

        @SerialName("selector")
        val selector: Felt,

        @SerialName("error")
        val error: ContractExecutionError,
    ) : ContractExecutionError()

    @Serializable
    data class ErrorMessage(
        val value: String,
    ) : ContractExecutionError()
}


fun main() {
    val json = Json {
        prettyPrint = true
    }

    val jsonString = """
    {
      "contract_address": "0x123",
      "class_hash": "0xabc",
      "selector": "0xdef",
      "error": {
        "contract_address": "0x456",
        "class_hash": "0xdef",
        "selector": "0xabc",
        "error": "some basic error text"
      }
    }
    """.trimIndent()

    // Deserialize JSON string to InnerCallError
    val result = json.decodeFromString<ContractExecutionError>(jsonString)

    // Print the deserialized object
    println(result)

    // Serialize it back to JSON
    val serialized = json.encodeToString(result)
    println(serialized)
}