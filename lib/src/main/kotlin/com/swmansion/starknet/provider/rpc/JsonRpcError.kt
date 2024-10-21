package com.swmansion.starknet.provider.rpc

import kotlinx.serialization.Serializable

@Serializable
sealed class JsonRpcError {
    abstract val code: Int
    abstract val message: String
    abstract val data: String?

    companion object {
        fun fromCode(code: Int, data: String?): JsonRpcError {
            return when (code) {
                1 -> FailedToReceiveTransactionError(data = data)
                20 -> ContractNotFoundError(data = data)
                24 -> BlockNotFoundError(data = data)
                27 -> InvalidTransactionIndexError(data = data)
                28 -> ClassHashNotFoundError(data = data)
                29 -> TransactionHashNotFoundError(data = data)
                31 -> PageSizeTooBigError(data = data)
                32 -> NoBlocksError(data = data)
                33 -> InvalidContinuationTokenError(data = data)
                34 -> TooManyKeysInFilterError(data = data)
                40 -> ContractError(data = data)
                41 -> TransactionExecutionError(data = data)
                else -> throw IllegalArgumentException("Unknown JSON RPC error code: $code")
            }
        }
    }
}

@Suppress("DataClassPrivateConstructor")
@Serializable
data class FailedToReceiveTransactionError private constructor(
    override val code: Int,
    override val message: String,
    override val data: String? = null,
) : JsonRpcError() {
    @JvmOverloads
    constructor(data: String? = null) : this(
        code = 1,
        message = "Failed to write transaction",
        data = data,
    )
}

@Suppress("DataClassPrivateConstructor")
@Serializable
data class ContractNotFoundError private constructor(
    override val code: Int,
    override val message: String,
    override val data: String? = null,
) : JsonRpcError() {
    @JvmOverloads
    constructor(data: String? = null) : this(
        code = 20,
        message = "Contract not found",
        data = data,
    )
}

@Suppress("DataClassPrivateConstructor")
@Serializable
data class BlockNotFoundError private constructor(
    override val code: Int,
    override val message: String,
    override val data: String? = null,
) : JsonRpcError() {
    @JvmOverloads
    constructor(data: String? = null) : this(
        code = 24,
        message = "Block not found",
        data = data,
    )
}

@Suppress("DataClassPrivateConstructor")
@Serializable
data class InvalidTransactionIndexError private constructor(
    override val code: Int,
    override val message: String,
    override val data: String? = null,
) : JsonRpcError() {
    @JvmOverloads
    constructor(data: String? = null) : this(
        code = 27,
        message = "Invalid transaction index in a block",
        data = data,
    )
}

@Suppress("DataClassPrivateConstructor")
@Serializable
data class ClassHashNotFoundError private constructor(
    override val code: Int,
    override val message: String,
    override val data: String? = null,
) : JsonRpcError() {
    @JvmOverloads
    constructor(data: String? = null) : this(
        code = 28,
        message = "Class hash not found",
        data = data,
    )
}

@Suppress("DataClassPrivateConstructor")
@Serializable
data class TransactionHashNotFoundError private constructor(
    override val code: Int,
    override val message: String,
    override val data: String? = null,
) : JsonRpcError() {
    @JvmOverloads
    constructor(data: String? = null) : this(
        code = 29,
        message = "Transaction hash not found",
        data = data,
    )
}

@Suppress("DataClassPrivateConstructor")
@Serializable
data class PageSizeTooBigError private constructor(
    override val code: Int,
    override val message: String,
    override val data: String? = null,
) : JsonRpcError() {
    @JvmOverloads
    constructor(data: String? = null) : this(
        code = 31,
        message = "Requested page size is too big",
        data = data,
    )
}

@Suppress("DataClassPrivateConstructor")
@Serializable
data class NoBlocksError private constructor(
    override val code: Int,
    override val message: String,
    override val data: String? = null,
) : JsonRpcError() {
    @JvmOverloads
    constructor(data: String? = null) : this(
        code = 32,
        message = "There are no blocks",
        data = data,
    )
}

@Suppress("DataClassPrivateConstructor")
@Serializable
data class InvalidContinuationTokenError private constructor(
    override val code: Int,
    override val message: String,
    override val data: String? = null,
) : JsonRpcError() {
    @JvmOverloads
    constructor(data: String? = null) : this(
        code = 33,
        message = "The supplied continuation token is invalid or unknown",
        data = data,
    )
}

@Suppress("DataClassPrivateConstructor")
@Serializable
data class TooManyKeysInFilterError private constructor(
    override val code: Int,
    override val message: String,
    override val data: String? = null,
) : JsonRpcError() {
    @JvmOverloads
    constructor(data: String? = null) : this(
        code = 34,
        message = "Too many keys provided in a filter",
        data = data,
    )
}

@Suppress("DataClassPrivateConstructor")
@Serializable
data class ContractError private constructor(
    override val code: Int,
    override val message: String,
    override val data: String? = null,
) : JsonRpcError() {
    @JvmOverloads
    constructor(data: String? = null) : this(
        code = 40,
        message = "Contract error",
        data = data,
    )
}

@Suppress("DataClassPrivateConstructor")
@Serializable
data class TransactionExecutionError private constructor(
    override val code: Int,
    override val message: String,
    override val data: String? = null,
) : JsonRpcError() {
    @JvmOverloads
    constructor(data: String? = null) : this(
        code = 41,
        message = "Transaction execution error",
        data = data,
    )
}

