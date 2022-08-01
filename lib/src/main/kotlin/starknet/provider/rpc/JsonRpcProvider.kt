package starknet.provider.rpc

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.*
import starknet.data.responses.Transaction
import starknet.data.responses.TransactionReceipt
import starknet.data.responses.serializers.JsonRpcTransactionPolymorphicSerializer
import starknet.data.types.*
import starknet.provider.Provider
import starknet.provider.Request
import starknet.service.http.HttpRequest
import starknet.service.http.HttpService

/**
 * A provider for interacting with StarkNet JSON-RPC
 *
 * @param url url of the service providing a rpc interface
 * @param chainId an id of the network
 */
class JsonRpcProvider(
    private val url: String,
    override val chainId: StarknetChainId,
) : Provider {

    private fun buildRequestJson(method: String, paramsJson: JsonElement): Map<String, JsonElement> {
        val map = mapOf(
            "jsonrpc" to JsonPrimitive("2.0"),
            "method" to JsonPrimitive(method),
            "id" to JsonPrimitive(0), // It is not used anywhere
            "params" to paramsJson,
        )

        return JsonObject(map)
    }

    private fun <T> buildRequest(
        method: JsonRpcMethod,
        paramsJson: JsonElement,
        responseSerializer: KSerializer<T>,
    ): HttpRequest<T> {
        val requestJson = buildRequestJson(method.methodName, paramsJson)

        val payload = HttpService.Payload(url, "POST", emptyList(), requestJson.toString())

        val jsonRpcDeserializer = JsonRpcResponseDeserializer(responseSerializer)

        return HttpRequest(payload, jsonRpcDeserializer)
    }

    private fun callContract(payload: CallContractPayload): Request<CallContractResponse> {
        val params = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.CALL, params, CallContractResponse.serializer())
    }

    override fun callContract(call: Call, blockTag: BlockTag): Request<CallContractResponse> {
        val payload = CallContractPayload(call, BlockHashOrTag.Tag(blockTag))

        return callContract(payload)
    }

    override fun callContract(call: Call, blockHash: Felt): Request<CallContractResponse> {
        val payload = CallContractPayload(call, BlockHashOrTag.Hash(blockHash))

        return callContract(payload)
    }

    private fun getStorageAt(payload: GetStorageAtPayload): Request<Felt> {
        val params = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_STORAGE_AT, params, Felt.serializer())
    }

    override fun getStorageAt(contractAddress: Felt, key: Felt, blockTag: BlockTag): Request<Felt> {
        val payload = GetStorageAtPayload(contractAddress, key, BlockHashOrTag.Tag(blockTag))

        return getStorageAt(payload)
    }

    override fun getStorageAt(contractAddress: Felt, key: Felt, blockHash: Felt): Request<Felt> {
        val payload = GetStorageAtPayload(contractAddress, key, BlockHashOrTag.Hash(blockHash))

        return getStorageAt(payload)
    }

    override fun getTransaction(transactionHash: Felt): Request<Transaction> {
        val payload = GetTransactionByHashPayload(transactionHash)
        val params = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_TRANSACTION_BY_HASH, params, JsonRpcTransactionPolymorphicSerializer)
    }

    override fun getTransactionReceipt(transactionHash: Felt): Request<TransactionReceipt> {
        val payload = GetTransactionReceiptPayload(transactionHash)
        val params = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_TRANSACTION_RECEIPT, params, TransactionReceipt.serializer())
    }

    override fun invokeFunction(
        payload: InvokeFunctionPayload,
    ): Request<InvokeFunctionResponse> {
        val params = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.INVOKE_TRANSACTION, params, InvokeFunctionResponse.serializer())
    }

    override fun deployContract(payload: DeployTransactionPayload): Request<DeployResponse> {
        val params = buildJsonObject {
            put("contract_definition", payload.contractDefinition.toRpcJson())
            putJsonArray("constructor_calldata") {
//                FIXME(restore this once devnet accepts our PR
//                payload.constructorCalldata.forEach { addFeltAsHex(it) }
                payload.constructorCalldata.forEach { add(it.value.intValueExact()) }
            }
//            putFeltAsHex("contract_address_salt", payload.salt)
            put("contract_address_salt", payload.salt.value.intValueExact())
        }

        return buildRequest(JsonRpcMethod.DEPLOY, params, DeployResponse.serializer())
    }

    override fun declareContract(payload: DeclareTransactionPayload): Request<DeclareResponse> {
        val params = buildJsonObject {
            put("contract_class", payload.contractDefinition.toRpcJson())
//            FIXME(restore this once devnet accepts our PR
//            putFeltAsHex("version", payload.version)
            put("version", 0)
        }

        return buildRequest(JsonRpcMethod.DECLARE, params, DeclareResponse.serializer())
    }
}
