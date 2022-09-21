package starknet.utils

import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.service.http.HttpService
import com.swmansion.starknet.service.http.OkHttpService
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.lang.Exception
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists

class DevnetSetupFailedException(message: String) : Exception(message)

class DevnetClient(
    private val host: String = "0.0.0.0",
    private val port: Int = 5050,
    private val httpService: HttpService = OkHttpService(),
) : AutoCloseable {
    private val accountDirectory = Paths.get("src/test/resources/account")
    private val baseUrl: String = "http://$host:$port"
    private val seed: Int = 1053545547

    private lateinit var accountAddress: Felt
    private lateinit var devnetProcess: Process

    private var isDevnetRunning = false

    val gatewayUrl: String = "$baseUrl/gateway"
    val feederGatewayUrl: String = "$baseUrl/feeder_gateway"
    val rpcUrl: String = "$baseUrl/rpc"

    @Serializable
    data class Block(
        @SerialName("block_hash") val hash: Felt,
        @SerialName("block_number") val number: Int,
    )

    data class TransactionResult(val address: Felt, val hash: Felt)

    fun start() {
        if (isDevnetRunning) {
            throw DevnetSetupFailedException("Devnet is already running")
        }

        // This kills any zombie devnet processes left over from previous
        // test runs, if any.
        ProcessBuilder(
            "pkill",
            "-f",
            "starknet-devnet.*$port.*$seed",
        ).start().waitFor()

        devnetProcess =
            ProcessBuilder(
                "starknet-devnet",
                "--host",
                host,
                "--port",
                port.toString(),
                "--seed",
                seed.toString(),
            ).start()

        // TODO: Replace with reading buffer until it prints "Listening on"
        devnetProcess.waitFor(10, TimeUnit.SECONDS)

        if (!devnetProcess.isAlive) {
            throw DevnetSetupFailedException("Could not start devnet process")
        }

        isDevnetRunning = true

        if (accountDirectory.exists()) {
            accountDirectory.toFile().walkTopDown().forEach { it.delete() }
        }
        deployAccount()
        prefundAccount()
    }

    override fun close() {
        if (!isDevnetRunning) return

        devnetProcess.destroyForcibly()

        // Wait for the process to be destroyed
        devnetProcess.waitFor()
        isDevnetRunning = false
    }

    private fun prefundAccount() {
        val payload = HttpService.Payload(
            "$baseUrl/mint",
            "POST",
            emptyList(),
            """
            {
              "address": "${accountAddress.hexString()}",
              "amount": 5000000000000000
            }
            """.trimIndent(),
        )
        val response = httpService.send(payload)
        if (!response.isSuccessful) {
            throw DevnetSetupFailedException("Prefunding account failed")
        }
    }

    private fun deployAccount() {
        val deployProcess = ProcessBuilder(
            "starknet",
            "deploy_account",
            "--gateway_url",
            gatewayUrl,
            "--feeder_gateway_url",
            feederGatewayUrl,
            "--account_dir",
            accountDirectory.toString(),
            "--wallet",
            "starkware.starknet.wallets.open_zeppelin.OpenZeppelinAccount",
            "--network",
            "alpha-goerli",
        ).start()

        deployProcess.waitFor()

        val error = String(deployProcess.errorStream.readAllBytes())
        requireNoErrors("Account setup", error)

        val output = String(deployProcess.inputStream.readAllBytes())
        val lines = output.lines()

        val result = getTransactionResult(lines, offset = 5)
        accountAddress = result.address
    }

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
            "--no_wallet",
        ).start()

        deployProcess.waitFor()

        val error = String(deployProcess.errorStream.readAllBytes())
        requireNoErrors("Contract deployment", error)

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
            "--account_dir",
            accountDirectory.toString(),
            "--wallet",
            "starkware.starknet.wallets.open_zeppelin.OpenZeppelinAccount",
            "--network",
            "alpha-goerli",
        ).start()

        declareProcess.waitFor()

        val error = String(declareProcess.errorStream.readAllBytes())
        requireNoErrors("Contract declare", error)

        val result = String(declareProcess.inputStream.readAllBytes())
        val lines = result.lines()
        return getTransactionResult(lines, offset = 2)
    }

    fun invokeTransaction(
        functionName: String,
        contractAddress: Felt,
        abiPath: Path,
        vararg inputs: Int,
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
            "--account_dir",
            accountDirectory.toString(),
            "--wallet",
            "starkware.starknet.wallets.open_zeppelin.OpenZeppelinAccount",
            "--network",
            "alpha-goerli",
        ).start()

        invokeProcess.waitFor()

        val error = String(invokeProcess.errorStream.readAllBytes())
        requireNoErrors("Invoke contract", error)

        val result = String(invokeProcess.inputStream.readAllBytes())
        val lines = result.lines()
        return getTransactionResult(lines, offset = 2)
    }

    fun getLatestBlock(): Block {
        val getBlockProcess = ProcessBuilder(
            "starknet",
            "get_block",
            "--gateway_url",
            gatewayUrl,
            "--feeder_gateway_url",
            feederGatewayUrl,
        ).start()

        getBlockProcess.waitFor()

        val result = String(getBlockProcess.inputStream.readAllBytes())

        val json = Json {
            ignoreUnknownKeys = true
        }

        return json.decodeFromString(Block.serializer(), result)
    }

    fun getStorageAt(contractAddress: Felt, storageKey: Felt): Felt {
        val getStorageAtProcess = ProcessBuilder(
            "starknet",
            "get_storage_at",
            "--contract_address",
            contractAddress.hexString(),
            "--key",
            storageKey.decString(),
            "--gateway_url",
            gatewayUrl,
            "--feeder_gateway_url",
            feederGatewayUrl,
        ).start()

        val error = String(getStorageAtProcess.errorStream.readAllBytes())
        requireNoErrors("Get storage", error)

        val result = String(getStorageAtProcess.inputStream.readAllBytes())
        return Felt.fromHex(result.trim())
    }

    private fun requireNoErrors(methodName: String, errorStream: String) {
        if (errorStream.isNotEmpty()) {
            throw DevnetSetupFailedException("Step $methodName failed. error = $errorStream")
        }
    }

    private fun getValueFromLine(line: String, index: Int = 1): String {
        val split = line.split(": ")
        return split[index]
    }

    private fun getTransactionResult(lines: List<String>, offset: Int = 1): TransactionResult {
        val address = Felt.fromHex(getValueFromLine(lines[offset]))
        val hash = Felt.fromHex(getValueFromLine(lines[offset + 1]))
        return TransactionResult(address, hash)
    }
}
