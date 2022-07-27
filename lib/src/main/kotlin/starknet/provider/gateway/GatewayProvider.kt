package starknet.provider.gateway

import kotlinx.serialization.json.*
import starknet.data.DECLARE_SENDER_ADDRESS
import starknet.data.NetUrls.MAINNET_URL
import starknet.data.NetUrls.TESTNET_URL
import starknet.data.responses.Transaction
import starknet.data.responses.TransactionReceipt
import starknet.data.responses.serializers.GatewayTransactionTransformingSerializer
import starknet.data.types.*
import starknet.extensions.base64Gzipped
import starknet.extensions.putFeltAsHex
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
    @Suppress("SameParameterValue")
    private fun gatewayRequestUrl(method: String): String {
        return "$gatewayUrl/$method"
    }

    private fun feederGatewayRequestUrl(method: String): String {
        return "$feederGatewayUrl/$method"
    }

    private fun callContract(payload: CallContractPayload): Request<CallContractResponse> {
        val url = feederGatewayRequestUrl("call_contract")

        val params = listOf(Pair("blockHash", payload.blockHashOrTag.string()))

        val decimalCalldata = Json.encodeToJsonElement(payload.request.calldata.toDecimal())
        val body = buildJsonObject {
            put("contract_address", payload.request.contractAddress.hexString())
            put("entry_point_selector", payload.request.entrypoint.hexString())
            put("calldata", decimalCalldata)
            put("signature", JsonArray(emptyList()))
        }

        val httpPayload = HttpService.Payload(url, "POST", params, body.toString())
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
        val params = listOf(
            Pair("contractAddress", payload.contractAddress.hexString()),
            Pair("key", payload.key.hexString()),
            Pair("blockHash", payload.blockHashOrTag.string())
        )
        val url = feederGatewayRequestUrl("get_storage_at")
        val httpPayload = HttpService.Payload(url, "GET", params)

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
        val url = feederGatewayRequestUrl("get_transaction")
        val params = listOf(Pair("transactionHash", transactionHash.hexString()))

        val httpPayload = HttpService.Payload(url, "GET", params)
        return HttpRequest(httpPayload, GatewayTransactionTransformingSerializer)
    }

    override fun getTransactionReceipt(transactionHash: Felt): Request<TransactionReceipt> {
        val url = feederGatewayRequestUrl("get_transaction_receipt")
        val params = listOf(Pair("transactionHash", transactionHash.hexString()))

        val httpPayload = HttpService.Payload(url, "GET", params)
        return HttpRequest(httpPayload, TransactionReceipt.serializer())
    }

    override fun invokeFunction(payload: InvokeFunctionPayload): Request<InvokeFunctionResponse> {
        val url = gatewayRequestUrl("add_transaction")

        val body = buildJsonObject {
            put("type", JsonPrimitive("INVOKE_FUNCTION"))
            put("contract_address", payload.invocation.contractAddress.hexString())
            put("entry_point_selector", payload.invocation.entrypoint.hexString())
            putJsonArray("calldata") {
                payload.invocation.calldata.toDecimal().forEach { add(it) }
            }
            put("max_fee", payload.maxFee?.hexString())
            putJsonArray("signature") {
                payload.signature?.toDecimal()?.forEach { add(it) }
            }
        }

        val httpPayload = HttpService.Payload(url, "POST", body.toString())
        return HttpRequest(httpPayload, InvokeFunctionResponse.serializer())
    }

    override fun deployContract(payload: DeployTransactionPayload): Request<DeployResponse> {
        val url = gatewayRequestUrl("add_transaction")
        val (program, entryPointsByType, abi) = payload.contractDefinition.parseContract()
        val compressedProgram = program.toString().base64Gzipped()

        val body = buildJsonObject {
            put("type", "DEPLOY")
            putFeltAsHex("contract_address_salt", payload.salt)
            putJsonArray("constructor_calldata") {
                payload.constructorCalldata.toDecimal().forEach { add(it) }
            }
            putJsonObject("contract_definition") {
                put("program", compressedProgram)
                put("entry_points_by_type", entryPointsByType)
                put("abi", abi)
            }
            putFeltAsHex("version", payload.version)
        }

        val httpPayload = HttpService.Payload(url, "POST", body.toString())
        return HttpRequest(httpPayload, DeployResponse.serializer())
    }

    override fun declareContract(payload: DeclareTransactionPayload): Request<DeclareResponse> {
        val url = gatewayRequestUrl("add_transaction")
        val (program, entryPointsByType, abi) = payload.contractDefinition.parseContract()
        val compressedProgram = program.toString().base64Gzipped()

        val body = buildJsonObject {
            put("type", "DECLARE")
            putFeltAsHex("sender_address", DECLARE_SENDER_ADDRESS)
            putFeltAsHex("max_fee", payload.maxFee)
            putFeltAsHex("nonce", payload.nonce)
            putFeltAsHex("version", payload.version)
            putJsonArray("signature") { payload.signature }
            putJsonObject("contract_class") {
                put("program", compressedProgram)
                put("entry_points_by_type", entryPointsByType)
                put("abi", abi)
            }
        }

        val httpPayload = HttpService.Payload(url, "POST", body.toString())
        return HttpRequest(httpPayload, DeclareResponse.serializer())
    }

    companion object Factory {
        @JvmStatic
        fun makeTestnetClient(): GatewayProvider {
            return GatewayProvider(
                "$TESTNET_URL/feeder_gateway",
                "$TESTNET_URL/gateway",
                StarknetChainId.TESTNET
            )
        }

        @JvmStatic
        fun makeMainnetClient(): GatewayProvider {
            return GatewayProvider(
                "$MAINNET_URL/feeder_gateway",
                "$MAINNET_URL/gateway",
                StarknetChainId.MAINNET
            )
        }
    }
}
