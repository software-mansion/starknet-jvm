package starknet.utils

import starknet.data.types.Felt
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.absolutePathString

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
        devnetProcess!!.waitFor(2, TimeUnit.SECONDS)

        if (!devnetProcess!!.isAlive) {
            throw Error("Could not start devnet process")
        }
    }

    fun destroy() {
        devnetProcess?.destroyForcibly()

        // Wait for the process to be destroyed
        devnetProcess?.waitFor()
        devnetProcess = null
    }

    data class TransactionResult(val address: Felt, val hash: Felt)

    fun deployContract(contractPath: Path): TransactionResult {
        val deployProcess = ProcessBuilder(
            "starknet",
            "deploy",
            "--gateway_url",
            gatewayUrl,
            "--feeder_gateway_url",
            feederGatewayUrl,
            "--contract",
            contractPath.absolutePathString(),
            "--no_wallet"
        ).start()

        deployProcess.waitFor()

        val result = String(deployProcess.inputStream.readAllBytes())
        val lines = result.lines()
        return getTransactionResult(lines)
    }

    fun declareContract(contractPath: Path): TransactionResult {
        val declareProcess = ProcessBuilder(
            "starknet",
            "declare",
            "--gateway_url",
            gatewayUrl,
            "--feeder_gateway_url",
            feederGatewayUrl,
            "--contract",
            contractPath.absolutePathString(),
            "--no_wallet"
        ).start()

        declareProcess.waitFor()

        val result = String(declareProcess.inputStream.readAllBytes())
        val lines = result.lines()
        return getTransactionResult(lines)
    }

    fun invokeTransaction(
        functionName: String,
        contractAddress: Felt,
        abiPath: Path,
        vararg inputs: Int
    ): TransactionResult {
        val invokeProcess = ProcessBuilder(
            "starknet",
            "invoke",
            "--gateway_url",
            gatewayUrl,
            "--feeder_gateway_url",
            feederGatewayUrl,
            "--address",
            contractAddress.hexString(),
            "--abi",
            abiPath.absolutePathString(),
            "--function",
            functionName,
            "--inputs",
            inputs.joinToString(separator = " "),
            "--no_wallet"
        ).start()

        invokeProcess.waitFor()

        val result = String(invokeProcess.inputStream.readAllBytes())
        val lines = result.lines()
        return getTransactionResult(lines)
    }

    private fun getValueFromLine(line: String, index: Int = 1): String {
        val split = line.split(": ")
        return split[index]
    }

    private fun getTransactionResult(lines: List<String>): TransactionResult {
        val address = Felt.fromHex(getValueFromLine(lines[1]))
        val hash = Felt.fromHex(getValueFromLine(lines[2]))
        return TransactionResult(address, hash)
    }
}
