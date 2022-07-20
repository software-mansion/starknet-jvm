package starknet.provider.gateway

import kotlinx.serialization.json.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import starknet.data.responses.Transaction
import starknet.data.responses.TransactionReceipt
import starknet.data.responses.serializers.GatewayTransactionTransformingSerializer
import starknet.data.types.*
import starknet.provider.Provider
import starknet.provider.Request
import starknet.service.http.HttpRequest
import starknet.service.http.HttpService

/**
 * A provider for interacting with StarkNet gateway.
 *
 * @param feederGatewayUrl url of the feeder gateway
 * @param gatewayUrl url of the gateway
 * @param chainId an id of the network
 */
class GatewayProvider(
    private val feederGatewayUrl: String,
    private val gatewayUrl: String,
    override val chainId: StarknetChainId
) : Provider {
    private fun buildRequestUrl(
        baseUrl: String,
        endpoint: String,
        vararg params: Pair<String, String>
    ): String {
        val urlBuilder = baseUrl.toHttpUrl().newBuilder()

        urlBuilder.addPathSegment(endpoint)

        for (param in params) {
            urlBuilder.addQueryParameter(param.first, param.second)
        }

        return urlBuilder.build().toString()
    }

    private fun callContract(payload: CallContractPayload): Request<CallContractResponse> {
        val url = buildRequestUrl(
            feederGatewayUrl,
            "call_contract",
            Pair("blockHash", payload.blockHashOrTag.string())
        )

        val decimalCalldata = Json.encodeToJsonElement(payload.request.calldata.toDecimal())

        val jsonPayload = buildJsonObject {
            put("contract_address", payload.request.contractAddress.hexString())
            put("entry_point_selector", payload.request.entrypoint.hexString())
            put("calldata", decimalCalldata)
            put("signature", JsonArray(emptyList()))
        }

        val httpPayload = HttpService.Payload(url, "POST", jsonPayload.toString())
        return HttpRequest(httpPayload, CallContractResponse.serializer())
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
        val url = buildRequestUrl(
            feederGatewayUrl,
            "get_storage_at",
            Pair("contractAddress", payload.contractAddress.hexString()),
            Pair("key", payload.key.hexString()),
            Pair("blockHash", payload.blockHashOrTag.string())
        )

        val httpPayload = HttpService.Payload(url, "GET")
        return HttpRequest(httpPayload, Felt.serializer())
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
        val url = buildRequestUrl(
            feederGatewayUrl,
            "get_transaction",
            Pair("transactionHash", transactionHash.hexString())
        )

        val httpPayload = HttpService.Payload(url, "GET")
        return HttpRequest(httpPayload, GatewayTransactionTransformingSerializer)
    }

    override fun getTransactionReceipt(transactionHash: Felt): Request<TransactionReceipt> {
        val url = buildRequestUrl(
            feederGatewayUrl,
            "get_transaction_receipt",
            Pair("transactionHash", transactionHash.hexString())
        )

        val httpPayload = HttpService.Payload(url, "GET")
        return HttpRequest(httpPayload, TransactionReceipt.serializer())
    }

    override fun invokeFunction(payload: InvokeFunctionPayload): Request<InvokeFunctionResponse> {
        val url = buildRequestUrl(gatewayUrl, "add_transaction")

        val decimalCalldata = Json.encodeToJsonElement(payload.invocation.calldata.toDecimal())
        val decimalSignature = Json.encodeToJsonElement(payload.signature?.toDecimal() ?: emptyList())

        val jsonPayload = buildJsonObject {
            put("type", JsonPrimitive("INVOKE_FUNCTION"))
            put("contract_address", payload.invocation.contractAddress.hexString())
            put("entry_point_selector", payload.invocation.entrypoint.hexString())
            put("calldata", decimalCalldata)
            put("max_fee", payload.maxFee?.hexString())
            put("signature", decimalSignature)
        }

        val httpPayload = HttpService.Payload(url, "POST", jsonPayload.toString())
        return HttpRequest(httpPayload, InvokeFunctionResponse.serializer())
    }
}
