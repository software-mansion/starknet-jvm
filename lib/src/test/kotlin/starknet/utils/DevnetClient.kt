package starknet.utils

import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.GetBlockHashAndNumberResponse
import com.swmansion.starknet.data.types.transactions.GatewayTransactionReceipt
import com.swmansion.starknet.service.http.HttpService
import com.swmansion.starknet.service.http.OkHttpService
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import java.lang.Exception
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.readText

class DevnetSetupFailedException(message: String) : Exception(message)

class DevnetClient(
    private val host: String = "0.0.0.0",
    private val port: Int = 5050,
    private val httpService: HttpService = OkHttpService(),
) : AutoCloseable {
    private val accountDirectory = Paths.get("src/test/resources/account")
    private val baseUrl: String = "http://$host:$port"
    private val seed: Int = 1053545547
    private val json = Json { ignoreUnknownKeys = true }

    private lateinit var devnetProcess: Process

    private var isDevnetRunning = false

    lateinit var accountDetails: AccountDetails

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

    fun prefundAccount(accountAddress: Felt) {
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

    private fun prefundAccount() = prefundAccount(accountDetails.address)

    private fun deployAccount() {
        val params = arrayOf(
            "--account_dir",
            accountDirectory.toString(),
            "--wallet",
            "starkware.starknet.wallets.open_zeppelin.OpenZeppelinAccount",
        )

        runStarknetCli(
            "Create account config",
            "new_account",
            *params,
        )

        runStarknetCli(
            "Account deployment",
            "deploy_account",
            *params,
        )

        accountDetails = readAccountDetails()
    }

    fun deployContract(contractPath: Path): TransactionResult {
        val (classHash, _) = declareContract(contractPath)
        println("CLASS HASH")
        println(classHash)
        val result = runStarknetCli(
            "Contract deployment",
            "deploy",
            "--class_hash",
            classHash.hexString(),
            "--wallet",
            "starkware.starknet.wallets.open_zeppelin.OpenZeppelinAccount",
        )

        val lines = result.lines()
        return getTransactionResult(lines)
    }

    fun declareContract(contractPath: Path): TransactionResult {
        val result = runStarknetCli(
            "Contract declare",
            "declare",
            "--no_wallet",
            "--contract",
            contractPath.absolutePathString(),
            "--account_dir",
            accountDirectory.toString(),
            // Any address is accepted as the sender here
            "--sender", "0x7754f1c8af9126d6fbf38b1ca2ccf8f92e0b267c8a96704e4964060b119b78d",
//            "--wallet",
//            "starkware.starknet.wallets.open_zeppelin.OpenZeppelinAccount",
        )

        val lines = result.lines()
        println(lines)
        return getTransactionResult(lines, offset = 1)
    }

    fun invokeTransaction(
        functionName: String,
        contractAddress: Felt,
        abiPath: Path,
        inputs: List<Felt>,
    ): TransactionResult {
        val result = runStarknetCli(
            "Invoke contract",
            "invoke",
            "--address",
            contractAddress.hexString(),
            "--abi",
            abiPath.absolutePathString(),
            "--function",
            functionName,
            "--inputs",
            *inputs.map { it.decString() }.toTypedArray(),
            "--account_dir",
            accountDirectory.toString(),
            "--wallet",
            "starkware.starknet.wallets.open_zeppelin.OpenZeppelinAccount",
        )

        val lines = result.lines()
        return getTransactionResult(lines, offset = 2)
    }

    fun getLatestBlock(): Block {
        val result = runStarknetCli(
            "Get latest block",
            "get_block",
        )

        val json = Json {
            ignoreUnknownKeys = true
        }

        return json.decodeFromString(Block.serializer(), result)
    }

    fun getStorageAt(contractAddress: Felt, storageKey: Felt): Felt {
        val result = runStarknetCli(
            "Get storage",
            "get_storage_at",
            "--contract_address",
            contractAddress.hexString(),
            "--key",
            storageKey.decString(),
        )

        return Felt.fromHex(result.trim())
    }

    fun transactionReceipt(transactionHash: Felt): GatewayTransactionReceipt {
        val result = runStarknetCli(
            "Get receipt",
            "get_transaction_receipt",
            "--hash",
            transactionHash.hexString(),
        )

        return json.decodeFromString(result)
    }

    fun latestBlock(): GetBlockHashAndNumberResponse {
        val result = runStarknetCli(
            "Get receipt",
            "get_block",
            "--number",
            "latest",
        )

        return json.decodeFromString(result)
    }

    private fun runStarknetCli(name: String, command: String, vararg args: String): String {
        val process = ProcessBuilder(
            "starknet",
            command,
            *args,
            "--gateway_url",
            gatewayUrl,
            "--feeder_gateway_url",
            feederGatewayUrl,
            "--network",
            "alpha-goerli",
        ).start()
        process.waitFor()

        val error = String(process.errorStream.readAllBytes())
        requireNoErrors(name, error)

        return String(process.inputStream.readAllBytes())
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

    private fun readAccountDetails(): AccountDetails {
        val accountFile = accountDirectory.resolve("starknet_open_zeppelin_accounts.json")
        val contents = accountFile.readText()
        return json.decodeFromString(AccountDetailsSerializer(), contents)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    data class AccountDetails(
        @JsonNames("private_key")
        val privateKey: Felt,

        @JsonNames("public_key")
        val publicKey: Felt,

        @JsonNames("address")
        val address: Felt,

        @JsonNames("salt")
        val salt: Felt,
    )

    class AccountDetailsSerializer : JsonTransformingSerializer<AccountDetails>(AccountDetails.serializer()) {
        override fun transformDeserialize(element: JsonElement): JsonElement {
            val accounts = element.jsonObject["alpha-goerli"]!!
            return accounts.jsonObject["__default__"]!!
        }
    }
}
