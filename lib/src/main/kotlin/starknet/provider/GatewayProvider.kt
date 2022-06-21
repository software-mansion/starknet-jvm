package starknet.provider

import starknet.data.types.*

class GatewayProvider: Provider  {
    val feederGatewayUrl: String
    val gatewayUrl: String

    constructor(feederGatewayUrl: String, gatewayUrl: String) {
        this.feederGatewayUrl = feederGatewayUrl
        this.gatewayUrl = gatewayUrl
    }

    constructor() {
        this.feederGatewayUrl = "https://alpha4.starknet.io/feeder_gateway/"
        this.gatewayUrl = "https://alpha4.starknet.io/gateway/"
    }

    override fun callContract(invokeTransaction: Invocation): Request<CallContractResponse> {
        val service = HttpService(feederGatewayUrl + "call_contract", "get")

        return Request(service, "test")
    }
}