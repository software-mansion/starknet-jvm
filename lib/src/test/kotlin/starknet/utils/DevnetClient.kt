package starknet.utils

import starknet.data.types.Felt
import java.util.concurrent.TimeUnit

const val HEX_ADDRESS_LENGTH = 66

class DevnetClient(val host: String = "localhost", val port: Int = 5050) {
    private var devnetProcess: Process? = null

    val gatewayUrl: String
    val feederGatewayUrl: String
    val rpcUrl: String

    init {
        val baseUrl = "http://${host}:${port}"

        gatewayUrl = "$baseUrl/gateway"
        feederGatewayUrl = "$baseUrl/feeder_gateway"
        rpcUrl = "$baseUrl/rpc"
    }

    fun start() {
        if (devnetProcess != null) {
            throw Error("AlreadyRunning")
        }

        devnetProcess = ProcessBuilder("starknet-devnet", "--host", host, "--port", port.toString()).start()

        // TODO: Replace with reading buffer until it prints "Listening on"
        devnetProcess!!.waitFor(1, TimeUnit.SECONDS)
    }

    fun destroy() {
        devnetProcess?.destroy()
        devnetProcess = null
    }

    fun deployContract(name: String): Felt {
        val deployProcess = ProcessBuilder(
            "starknet",
            "deploy",
            "--gateway_url",
            gatewayUrl,
            "--feeder_gateway_url",
            feederGatewayUrl,
            "--contract",
            "src/test/resources/compiled/${name}.json"
        ).start()

        deployProcess.waitFor()

        val result = String(deployProcess.inputStream.readAllBytes())

        val searchPhrase = "Contract address: "

        val addressStart = result.indexOf(searchPhrase) + searchPhrase.length
        val address = result.substring(addressStart, addressStart + HEX_ADDRESS_LENGTH)

        return Felt.fromHex(address)
    }
}
