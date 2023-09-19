package starknet.utils

import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.GetBlockHashAndNumberResponse
import com.swmansion.starknet.data.types.transactions.*
import com.swmansion.starknet.service.http.HttpService
import com.swmansion.starknet.service.http.OkHttpService
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.readText

class LegacyDevnetSetupFailedException(message: String) : Exception(message)

class LegacyDevnetOperationFailed(val failureReason: GatewayFailureReason?, val status: TransactionStatus) :
    Exception(failureReason?.errorMessage ?: "Devnet operation failed")

class LegacyDevnetClient(
    private val host: String = "0.0.0.0",
    private val port: Int = 5050,
    private val httpService: HttpService = OkHttpService(),
    private val accountDirectory: Path = Paths.get("src/test/resources/account"),
) : AutoCloseable {
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
            throw LegacyDevnetSetupFailedException("Devnet is already running")
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
                "--disable-rpc-request-validation",
                "--host",
                host,
                "--port",
                port.toString(),
                "--seed",
                seed.toString(),
                "--sierra-compiler-path",
                "../cairo/target/debug/starknet-sierra-compile",
            ).start()

        // TODO: Replace with reading buffer until it prints "Listening on"
        devnetProcess.waitFor(10, TimeUnit.SECONDS)

        if (!devnetProcess.isAlive) {
            throw LegacyDevnetSetupFailedException("Could not start devnet process")
        }

        isDevnetRunning = true

        if (accountDirectory.exists()) {
            accountDirectory.toFile().walkTopDown().forEach { it.delete() }
        }
        accountDetails = deployAccount("__default__").details
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
              "amount": 500000000000000000000000000000
            }
            """.trimIndent(),
        )
        val response = httpService.send(payload)
        if (!response.isSuccessful) {
            throw LegacyDevnetSetupFailedException("Prefunding account failed")
        }
    }

    data class DeployAccountResult(
        val details: AccountDetails,
        val txHash: Felt,
    )

    fun deployAccount(name: String? = null): DeployAccountResult {
        // We have to generate unique name for the account as a global namespace is used
        val accountName = name ?: UUID.randomUUID().toString()
        val params = arrayOf(
            "--account_dir",
            accountDirectory.toString(),
            "--account",
            accountName,
            "--wallet",
            "starkware.starknet.wallets.open_zeppelin.OpenZeppelinAccount",
        )

        runStarknetCli(
            "Create account config",
            "new_account",
            *params,
        )

        val details = readAccountDetails(accountName)
        prefundAccount(details.address)

        val result = runStarknetCli(
            "Account deployment",
            "deploy_account",
            *params,
        )
        val tx = getTransactionResult(result.lines(), offset = 3)

        assertTxPassed(tx.hash)

        return DeployAccountResult(details, tx.hash)
    }

    fun deployContract(contractPath: Path): TransactionResult {
        val (classHash, _) = declareContract(contractPath)
        val result = runStarknetCli(
            "Contract deployment",
            "deploy",
            "--class_hash",
            classHash.hexString(),
            "--account_dir",
            accountDirectory.toString(),
            "--wallet",
            "starkware.starknet.wallets.open_zeppelin.OpenZeppelinAccount",
            "--max_fee",
            "3115000000000000",
        )

        val lines = result.lines()
        val tx = getTransactionResult(lines)

        assertTxPassed(tx.hash)

        return tx
    }

    fun declareContract(contractPath: Path): TransactionResult {
        val result = runStarknetCli(
            "Contract declare",
            "declare",
            "--deprecated",
            "--contract",
            contractPath.absolutePathString(),
            "--account_dir",
            accountDirectory.toString(),
            "--wallet",
            "starkware.starknet.wallets.open_zeppelin.OpenZeppelinAccount",
        )

        val lines = result.lines()
        val tx = getTransactionResult(lines, offset = 2)

        assertTxPassed(tx.hash)

        return tx
    }

    fun declareV2Contract(contractPath: Path): TransactionResult {
        val result = runStarknetCli(
            "Contract declare",
            "declare",
            "--contract",
            contractPath.absolutePathString(),
            "--account_dir",
            accountDirectory.toString(),
            "--wallet",
            "starkware.starknet.wallets.open_zeppelin.OpenZeppelinAccount",
        )

        val lines = result.lines()
        val tx = getTransactionResult(lines, offset = 2)

        assertTxPassed(tx.hash)

        return tx
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
        val tx = getTransactionResult(lines, offset = 2)

        assertTxPassed(tx.hash)

        return tx
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

    private fun assertTxPassed(txHash: Felt) {
        val receipt = transactionReceipt(txHash)
        if (receipt.failureReason != null || receipt.status != TransactionStatus.ACCEPTED_ON_L2) {
            throw LegacyDevnetOperationFailed(receipt.failureReason, receipt.status)
        }
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

        val result = String(process.inputStream.readAllBytes())
        return result
    }

    private fun requireNoErrors(methodName: String, errorStream: String) {
        if (errorStream.isNotEmpty()) {
            throw LegacyDevnetSetupFailedException("Step $methodName failed. error = $errorStream")
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

    private fun readAccountDetails(accountName: String): AccountDetails {
        val accountFile = accountDirectory.resolve("starknet_open_zeppelin_accounts.json")
        val contents = accountFile.readText()
        return json.decodeFromString(AccountDetailsSerializer(accountName), contents)
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

    class AccountDetailsSerializer(val name: String) :
        JsonTransformingSerializer<AccountDetails>(AccountDetails.serializer()) {
        override fun transformDeserialize(element: JsonElement): JsonElement {
            val accounts = element.jsonObject["alpha-goerli"]!!
            return accounts.jsonObject[name]!!
        }
    }
}
