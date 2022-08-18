package com.swmansion.starknet.provider.rpc

/**
 * Enum with JSON-RPC methods.
 *
 * @param methodName method name to be converted to enum value
 */
internal enum class JsonRpcMethod(val methodName: String) {
    CALL("starknet_call"),
    INVOKE_TRANSACTION("starknet_addInvokeTransaction"),
    GET_STORAGE_AT("starknet_getStorageAt"),
    GET_CLASS("starknet_getClass"),
    GET_CLASS_AT("starknet_getClassAt"),
    GET_CLASS_HASH_AT("starknet_getClassHashAt"),
    GET_TRANSACTION_BY_HASH("starknet_getTransactionByHash"),
    GET_TRANSACTION_RECEIPT("starknet_getTransactionReceipt"),
    DECLARE("starknet_addDeclareTransaction"),
    DEPLOY("starknet_addDeployTransaction"),
    ESTIMATE_FEE("starknet_estimateFee"),
}
