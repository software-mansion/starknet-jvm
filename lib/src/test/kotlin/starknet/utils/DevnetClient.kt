package starknet.utils

import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.extensions.toUint128
import com.swmansion.starknet.extensions.toUint64
import com.swmansion.starknet.provider.Provider
import com.swmansion.starknet.provider.rpc.JsonRpcProvider
import com.swmansion.starknet.service.http.HttpService
import com.swmansion.starknet.service.http.OkHttpService
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import starknet.utils.data.*
import starknet.utils.data.serializers.AccountDetailsSerializer
import starknet.utils.data.serializers.SnCastResponsePolymorphicSerializer
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.readText

class DevnetClient(
    val host: String = "0.0.0.0",
    val port: Int = 5000,
    val seed: Int = 1053545547,
    private val httpService: HttpService = OkHttpService(),
    private val accountDirectory: Path = Paths.get("src/test/resources/account"),
    private val contractsDirectory: Path = Paths.get("src/test/resources/contracts"),
) : AutoCloseable {

    private val baseUrl: String = "http://$host:$port"
    val rpcUrl: String = "$baseUrl/rpc"

    private val json = Json { ignoreUnknownKeys = true }

    private lateinit var devnetPath: Path
    private lateinit var devnetProcess: Process
    private var isDevnetRunning = false

    private val accountFilePath = accountDirectory.resolve("starknet_open_zeppelin_accounts.json")
    private val scarbTomlPath = contractsDirectory.resolve("Scarb.toml")

    lateinit var defaultAccountDetails: AccountDetails

    val provider: Provider by lazy { JsonRpcProvider(rpcUrl) }

    private enum class TransactionVerificiationMode { RECEIPT, STATUS, DISABLED }

    private val transactionVerificiationMode = TransactionVerificiationMode.STATUS

    private enum class StateArchiveCapacity(val value: String) { FULL("full"), NONE("none") }

    private val stateArchiveCapacity = StateArchiveCapacity.FULL

    private val defaultResourceBounds = ResourceBoundsMapping(
        l1Gas = ResourceBounds(
            maxAmount = 100_000.toUint64,
            maxPricePerUnit = 10_000_000_000_000.toUint128,
        ),
        l2Gas = ResourceBounds(
            maxAmount = 1_000_000_000.toUint64,
            maxPricePerUnit = 100_000_000_000_000_000.toUint128,
        ),
        l1DataGas = ResourceBounds(
            maxAmount = 100_000.toUint64,
            maxPricePerUnit = 10_000_000_000_000.toUint128,
        ),
    )

    companion object {
        // Source: https://github.com/0xSpaceShard/starknet-devnet/blob/430b3370e60b28b8de430143b26e52bf36380b9a/crates/starknet-devnet-core/src/constants.rs#L25
        val accountContractClassHash =
            Felt.fromHex("0x05b4b537eaa2399e3aa99c4e2e0208ebd6c71bc1467938cd52c798c601e43564")
        val legacyAccountContractClassHash =
            Felt.fromHex("0x4d07e40e93398ed3c76981e72dd1fd22557a78ce36c0515f679e27f0bb5bc5f")
        val ethErc20ContractAddress = Felt.fromHex("0x49d36570d4e46f48e99674bd3fcc84644ddd6b96f7c741b1562b82f9e004dc7")
    }

    fun start() {
        if (isDevnetRunning) {
            throw DevnetSetupFailedException("Devnet is already running")
        }

        devnetPath = resolveDevnetPath()

        // This kills any zombie devnet processes left over from previous test runs, if any.
        ProcessBuilder(
            "pkill",
            "-f",
            "starknet-devnet.*$port.*$seed",
        ).start().waitFor()

        val devnetProcessBuilder = ProcessBuilder(
            "starknet-devnet",
            "--host",
            host,
            "--port",
            port.toString(),
            "--seed",
            seed.toString(),
            // This is currently needed for devnet to support requests with specified block_id (not latest or pre-confirmed)
            "--state-archive-capacity",
            stateArchiveCapacity.value,
        )
        devnetProcess = devnetProcessBuilder.start()
        devnetProcess.waitFor(8, TimeUnit.SECONDS)

        if (!devnetProcess.isAlive) {
            throw DevnetSetupFailedException("Could not start devnet process")
        }
        isDevnetRunning = true

        if (accountDirectory.exists()) {
            accountDirectory.toFile().walkTopDown().forEach { it.delete() }
        }
    }

    override fun close() {
        if (!isDevnetRunning) {
            return
        }
        devnetProcess.destroyForcibly()

        // Wait for the process to be destroyed
        devnetProcess.waitFor()
        isDevnetRunning = false
    }

    fun prefundAccountEth(accountAddress: Felt) {
        prefundAccount(accountAddress, PriceUnit.WEI)
    }

    fun prefundAccountStrk(accountAddress: Felt) {
        prefundAccount(accountAddress, PriceUnit.FRI)
    }

    private fun prefundAccount(accountAddress: Felt, priceUnit: PriceUnit) {
        val unit = Json.encodeToString(PriceUnit.serializer(), priceUnit)
        val payload = HttpService.Payload(
            url = rpcUrl,
            body =
            """
            {
              "jsonrpc": "2.0",
              "id": 0,
              "method": "devnet_mint",
              "params": {
                "address": "${accountAddress.hexString()}",
                "amount": 100000000000000000000000000000000000,
                "unit": $unit
              }
            }
            """.trimIndent(),
            method = "POST",
            params = emptyList(),
        )
        val response = httpService.send(payload)
        if (!response.isSuccessful) {
            throw DevnetSetupFailedException("Prefunding account failed")
        }
    }

    fun createAccount(
        accountName: String,
        classHash: Felt = accountContractClassHash,
        salt: Felt? = null,
        type: String,
    ): CreateAccountResult {
        val params = mutableListOf(
            "create",
            "--name",
            accountName,
            "--class-hash",
            classHash.hexString(),
            "--type",
            type,
            "--url",
            rpcUrl,
        )
        salt?.let {
            params.add("--salt")
            params.add(salt.hexString())
        }

        val response = runSnCast(
            command = "account",
            args = params,
        ) as AccountCreateSnCastResponse

        return CreateAccountResult(
            details = readAccountDetails(accountName),
            estimatedFee = response.estimatedFee,
        )
    }

    fun deployAccount(
        resourceBounds: ResourceBoundsMapping = defaultResourceBounds,
        prefund: Boolean = false,
        accountName: String = "__default__",
    ): DeployAccountResult {
        if (prefund) {
            prefundAccountEth(readAccountDetails(accountName).address)
            prefundAccountStrk(readAccountDetails(accountName).address)
        }

        val params = listOf(
            "deploy",
            "--name",
            accountName,
            *createFeeArgs(resourceBounds),
            "--url",
            rpcUrl,
        )
        val response = runSnCast(
            command = "account",
            args = params,
            accountName = accountName,
        ) as AccountDeploySnCastResponse

        requireTransactionSuccessful(response.transactionHash, "Deploy Account")

        return DeployAccountResult(
            details = readAccountDetails(accountName),
            transactionHash = response.transactionHash,
        )
    }

    fun createDeployAccount(
        classHash: Felt = accountContractClassHash,
        salt: Felt? = null,
        resourceBounds: ResourceBoundsMapping = defaultResourceBounds,
        accountName: String = "__default__",
        type: String = "oz",
    ): DeployAccountResult {
        val createResult = createAccount(accountName, classHash, salt, type)
        val details = createResult.details
        val deployResult = deployAccount(resourceBounds, prefund = true, accountName)

        requireTransactionSuccessful(deployResult.transactionHash, "Deploy Account")

        return DeployAccountResult(
            details = details,
            transactionHash = deployResult.transactionHash,
        )
    }

    fun declareContract(
        contractName: String,
        resourceBounds: ResourceBoundsMapping = defaultResourceBounds,
        accountName: String = "__default__",
    ): DeclareContractResult {
        val params = listOf(
            "--contract-name",
            contractName,
            *createFeeArgs(resourceBounds),
            "--url",
            rpcUrl,
        )
        val response = runSnCast(
            command = "declare",
            args = params,
            accountName = accountName,
        ) as DeclareSnCastResponse

        requireTransactionSuccessful(response.transactionHash, "Declare")

        return DeclareContractResult(
            classHash = response.classHash,
            transactionHash = response.transactionHash,
        )
    }

    fun deployContract(
        classHash: Felt,
        constructorCalldata: List<Felt> = emptyList(),
        salt: Felt? = null,
        unique: Boolean = false,
        resourceBounds: ResourceBoundsMapping = defaultResourceBounds,
        accountName: String = "__default__",
    ): DeployContractResult {
        val params = mutableListOf(
            "--class-hash",
            classHash.hexString(),
            *createFeeArgs(resourceBounds),
            "--url",
            rpcUrl,
        )
        if (constructorCalldata.isNotEmpty()) {
            params.add("--constructor-calldata")
            constructorCalldata.forEach { params.add(it.hexString()) }
        }
        if (unique) {
            params.add("--unique")
        }
        salt?.let {
            params.add("--salt")
            params.add(salt.hexString())
        }
        val response = runSnCast(
            command = "deploy",
            args = params,
            accountName = accountName,
        ) as DeploySnCastResponse

        requireTransactionSuccessful(response.transactionHash, "Deploy Contract")

        return DeployContractResult(
            transactionHash = response.transactionHash,
            contractAddress = response.contractAddress,
        )
    }

    fun declareDeployContract(
        contractName: String,
        constructorCalldata: List<Felt> = emptyList(),
        salt: Felt? = null,
        unique: Boolean = false,
        resourceBounds: ResourceBoundsMapping = defaultResourceBounds,
        accountName: String = "__default__",
    ): DeployContractResult {
        val declareResponse = declareContract(contractName, resourceBounds, accountName)

        val classHash = declareResponse.classHash
        val deployResponse = deployContract(classHash, constructorCalldata, salt, unique, resourceBounds, accountName)

        return DeployContractResult(
            transactionHash = deployResponse.transactionHash,
            contractAddress = deployResponse.contractAddress,
        )
    }

    fun invokeContract(
        contractAddress: Felt,
        function: String,
        calldata: List<Felt>,
        resourceBounds: ResourceBoundsMapping = defaultResourceBounds,
        accountName: String = "__default__",
    ): InvokeContractResult {
        val params = mutableListOf(
            "--contract-address",
            contractAddress.hexString(),
            "--function",
            function,
            *createFeeArgs(resourceBounds),
            "--url",
            rpcUrl,
        )
        if (calldata.isNotEmpty()) {
            params.add("--calldata")
            calldata.forEach { params.add(it.hexString()) }
        }
        val response = runSnCast(
            command = "invoke",
            args = params,
            accountName = accountName,
        ) as InvokeSnCastResponse

        requireTransactionSuccessful(response.transactionHash, "Invoke")

        return InvokeContractResult(
            transactionHash = response.transactionHash,
        )
    }

    private fun createFeeArgs(resourceBounds: ResourceBoundsMapping): Array<String> {
        return arrayOf(
            "--l1-gas",
            resourceBounds.l1Gas.maxAmount.value.toString(),
            "--l1-gas-price",
            resourceBounds.l1Gas.maxPricePerUnit.value.toString(),
            "--l2-gas",
            resourceBounds.l2Gas.maxAmount.value.toString(),
            "--l2-gas-price",
            resourceBounds.l2Gas.maxPricePerUnit.value.toString(),
            "--l1-data-gas",
            resourceBounds.l1DataGas.maxAmount.value.toString(),
            "--l1-data-gas-price",
            resourceBounds.l1DataGas.maxPricePerUnit.value.toString(),
        )
    }

    private fun runSnCast(
        command: String,
        args: List<String>,
        accountName: String = "__default__",
    ): SnCastResponse {
        val processBuilder = ProcessBuilder(
            "sncast",
            "--json",
            "--accounts-file",
            accountFilePath.absolutePathString(),
            "--profile",
            "tests_profile",
            "--account",
            accountName,
            command,
            *(args.toTypedArray()),
        )

        processBuilder.directory(File(contractsDirectory.absolutePathString()))

        val process = processBuilder.start()
        process.waitFor()

        val error = String(process.errorStream.readAllBytes())
        requireNoErrors(command, error)

        // As of sncast 0.40.0, declare command returns three response objects
        // First two of them come from "scarb build" output, and don't have "command" field
        // Last object is the actual one we want to return
        // Retrieving the last object works in both cases - with one and with few response objects
        val lines = String(process.inputStream.readAllBytes()).trim().split("\n").filter { it != "null" }
        val result = lines.last()

        // TODO(#596)
        // Workaround: "command" field is not present in sncast response anymore, hence
        // we need to put it to JSON synthetically
        val jsonElement = json.parseToJsonElement(result).jsonObject.toMutableMap()
        val commandStr = if (command == "account") {
            command + " " + (
                args.getOrNull(0)
                    ?: throw IllegalArgumentException("Missing subcommand for account command")
                )
        } else {
            command
        }
        jsonElement["command"] = JsonPrimitive(commandStr)

        // Remove fields with null values
        jsonElement.entries.removeIf { it.value is JsonNull }

        return json.decodeFromString(
            SnCastResponsePolymorphicSerializer,
            json.encodeToString(JsonObject(jsonElement)),
        )
    }

    private fun requireNoErrors(command: String, errorStream: String) {
        if (errorStream.isNotEmpty()) {
            throw SnCastCommandFailed(command, errorStream)
        }
    }

    private fun readAccountDetails(accountName: String = "__default__"): AccountDetails {
        val contents = accountFilePath.readText()
        return json.decodeFromString(AccountDetailsSerializer(accountName), contents)
    }

    private fun requireTransactionSuccessful(transactionHash: Felt, type: String) {
        // Receipt provides a more detailed error message than status, but is less reliable than getTransactionStatus
        // Use status by default, use receipt for debugging purposes if needed
        when (transactionVerificiationMode) {
            TransactionVerificiationMode.RECEIPT -> requireTransactionReceiptSuccessful(transactionHash, type)
            TransactionVerificiationMode.STATUS -> requireTransactionStatusSuccessful(transactionHash, type)
            TransactionVerificiationMode.DISABLED -> {}
        }
    }

    private fun requireTransactionReceiptSuccessful(transactionHash: Felt, type: String) {
        val request = provider.getTransactionReceipt(transactionHash)
        val receipt = request.send()
        if (!receipt.isAccepted) {
            throw DevnetTransactionFailedException("$type transaction failed. Reason: ${receipt.revertReason}")
        }
    }

    private fun requireTransactionStatusSuccessful(transactionHash: Felt, type: String) {
        val request = provider.getTransactionStatus(transactionHash)
        val status = request.send()
        if (status.executionStatus != TransactionExecutionStatus.SUCCEEDED &&
            (status.finalityStatus == TransactionStatus.ACCEPTED_ON_L1 || status.finalityStatus == TransactionStatus.ACCEPTED_ON_L2)
        ) {
            throw DevnetTransactionFailedException("$type transaction failed.")
        }
    }

    private fun resolveDevnetPath(): Path {
        val binaryName = "starknet-devnet"

        return System.getenv("DEVNET_PATH")?.takeIf { it.isNotBlank() }?.let { Paths.get(it) }
            ?: runAsdfWhich(binaryName)
            ?: throw DevnetSetupFailedException(
                "Could not find '$binaryName'. Set DEVNET_PATH env var or ensure it was installed through asdf for the project",
            )
    }

    private fun runAsdfWhich(binaryName: String): Path? =
        ProcessBuilder("asdf", "which", binaryName)
            .redirectErrorStream(true)
            .start()
            .inputStream
            .readAllBytes()
            .toString(Charsets.UTF_8)
            .trim()
            .takeIf { it.isNotEmpty() }
            ?.let { Paths.get(it) }
            ?.takeIf { Files.isExecutable(it) }
}
