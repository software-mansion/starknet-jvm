package starknet.utils

import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.PriceUnit
import com.swmansion.starknet.data.types.TransactionExecutionStatus
import com.swmansion.starknet.data.types.TransactionStatus
import com.swmansion.starknet.provider.Provider
import com.swmansion.starknet.provider.rpc.JsonRpcProvider
import com.swmansion.starknet.service.http.HttpService
import com.swmansion.starknet.service.http.OkHttpService
import kotlinx.serialization.json.*
import starknet.utils.data.*
import starknet.utils.data.serializers.AccountDetailsSerializer
import starknet.utils.data.serializers.SnCastResponsePolymorphicSerializer
import java.io.File
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
    val mintUrl: String = "$baseUrl/mint"

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

    companion object {
        // Source: https://github.com/0xSpaceShard/starknet-devnet-rs/blob/47ee2a73c227ee356f344ce94e5f61871299be80/crates/starknet-devnet-core/src/constants.rs
        val accountContractClassHash = Felt.fromHex("0x044cab2e6a3a7bc516425d06d76c6ffd56ae308864dbc66f8e75028e3784aa29")
        val legacyAccountContractClassHash = Felt.fromHex("0x4d07e40e93398ed3c76981e72dd1fd22557a78ce36c0515f679e27f0bb5bc5f")
        val ethErc20ContractClassHash = Felt.fromHex("0x6a22bf63c7bc07effa39a25dfbd21523d211db0100a0afd054d172b81840eaf")
        val ethErc20ContractAddress = Felt.fromHex("0x49d36570d4e46f48e99674bd3fcc84644ddd6b96f7c741b1562b82f9e004dc7")
        val strkErc20ContractAddress = Felt.fromHex("0x04718f5a0fc34cc1af16a1cdee98ffb20c31f5cd61d6ab07201858f4287c938d")
        val udcContractClassHash = Felt.fromHex("0x7b3e05f48f0c69e4a65ce5e076a66271a527aff2c34ce1083ec6e1526997a69")
        val udcContractAddress = Felt.fromHex("0x41a78e741e5af2fec34b695679bc6891742439f7afb8484ecd7766661ad02bf")

        // For seed 1053545547
        val predeployedAccount1 = AccountDetails(
            privateKey = Felt.fromHex("0xa2ed22bb0cb0b49c69f6d6a8d24bc5ea"),
            publicKey = Felt.fromHex("0x198e98e771ebb5da7f4f05658a80a3d6be2213dc5096d055cbbefa62901ab06"),
            address = Felt.fromHex("0x1323cacbc02b4aaed9bb6b24d121fb712d8946376040990f2f2fa0dcf17bb5b"),
            salt = Felt(20),
        )
        val predeployedAccount2 = AccountDetails(
            privateKey = Felt.fromHex("0xc1c7db92d22ef773de96f8bde8e56c85"),
            publicKey = Felt.fromHex("0x26df62f8e61920575f9c9391ed5f08397cfcfd2ade02d47781a4a8836c091fd"),
            address = Felt.fromHex("0x34864aab9f693157f88f2213ffdaa7303a46bbea92b702416a648c3d0e42f35"),
            salt = Felt(20),
        )
    }

    fun start() {
        if (isDevnetRunning) {
            throw DevnetSetupFailedException("Devnet is already running")
        }
        devnetPath = Paths.get(System.getenv("DEVNET_PATH")) ?: throw DevnetSetupFailedException(
            "DEVNET_PATH environment variable is not set. Make sure you have devnet installed https://github.com/0xSpaceShard/starknet-devnet-rs and DEVNET_PATH points to a devnet binary.",
        )

        // This kills any zombie devnet processes left over from previous test runs, if any.
        ProcessBuilder(
            "pkill",
            "-f",
            "starknet-devnet.*$port.*$seed",
        ).start().waitFor()

        val devnetProcessBuilder = ProcessBuilder(
            devnetPath.absolutePathString(),
//            "starknet-devnet",
            "--host",
            host,
            "--port",
            port.toString(),
            "--seed",
            seed.toString(),
            // This is currently needed for devnet to support requests with specified block_id (not latest or pending)
            "--state-archive-capacity",
            stateArchiveCapacity.value,
        )
        devnetProcess = devnetProcessBuilder.start()
        devnetProcess.waitFor(10, TimeUnit.SECONDS)

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
            url = mintUrl,
            body =
            """
            {
              "address": "${accountAddress.hexString()}",
              "amount": 500000000000000000000000000000,
              "unit": $unit
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
            "--silent",
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
            maxFee = response.maxFee,
        )
    }

    fun deployAccount(
        classHash: Felt = accountContractClassHash,
        maxFee: Felt = Felt(1000000000000000),
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
            "--l1-gas",
            "100000",
            "--l2-gas",
            "1000000000",
            "--l1-data-gas",
            "100000",
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
        maxFee: Felt = Felt(1000000000000000),
        accountName: String = "__default__",
        type: String = "oz",
    ): DeployAccountResult {
        val createResult = createAccount(accountName, classHash, salt, type)
        val details = createResult.details
        val deployResult = deployAccount(classHash, maxFee, prefund = true, accountName)

        requireTransactionSuccessful(deployResult.transactionHash, "Deploy Account")

        return DeployAccountResult(
            details = details,
            transactionHash = deployResult.transactionHash,
        )
    }

    fun declareContract(
        contractName: String,
        maxFee: Felt = Felt(5000000000000000),
        accountName: String = "__default__",
    ): DeclareContractResult {
        val params = listOf(
            "--contract-name",
            contractName,
            "--l1-gas",
            "100000",
            "--l2-gas",
            "1000000000",
            "--l1-data-gas",
            "100000",
        )
        try {
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
        } catch (e: SnCastCommandFailed) {
            println("AAA: "+e)
            throw e
        }


    }

    fun deployContract(
        classHash: Felt,
        constructorCalldata: List<Felt> = emptyList(),
        salt: Felt? = null,
        unique: Boolean = false,
        maxFee: Felt = Felt(1000000000000000),
        accountName: String = "__default__",
    ): DeployContractResult {
        val params = mutableListOf(
            "--class-hash",
            classHash.hexString(),
            "--l1-gas",
            "100000",
            "--l2-gas",
            "1000000000",
            "--l1-data-gas",
            "100000",
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
        maxFeeDeclare: Felt = Felt(5000000000000000),
        maxFeeDeploy: Felt = Felt(1000000000000000),
        accountName: String = "__default__",
    ): DeployContractResult {
        val declareResponse = declareContract(contractName, maxFeeDeclare, accountName)

        val classHash = declareResponse.classHash
        val deployResponse = deployContract(classHash, constructorCalldata, salt, unique, maxFeeDeploy, accountName)

        return DeployContractResult(
            transactionHash = deployResponse.transactionHash,
            contractAddress = deployResponse.contractAddress,
        )
    }

    fun invokeContract(
        contractAddress: Felt,
        function: String,
        calldata: List<Felt>,
        maxFee: Felt = Felt(1000000000000000),
        accountName: String = "__default__",
    ): InvokeContractResult {
        val params = mutableListOf(
            "--contract-address",
            contractAddress.hexString(),
            "--function",
            function,
            "--l1-gas",
            "100000",
            "--l2-gas",
            "1000000000",
            "--l1-data-gas",
            "100000",
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

//    /Users/franciszekjob/Projects/SWM/starknet-foundry/target/release/sncast --hex-format --json --accounts-file /Users/franciszekjob/Projects/starknet/starknet-jvm/lib/src/test/resources/accounts/standard_deployer_test/starknet_open_zeppelin_accounts.json --account __default__ account create --name __default__ --class-hash 0x44cab2e6a3a7bc516425d06d76c6ffd56ae308864dbc66f8e75028e3784aa29 --type oz --url http://0.0.0.0:5050/rpc
    private fun runSnCast(
        command: String,
        args: List<String>,
        accountName: String = "__default__",
    ): SnCastResponse {
        val processBuilder = ProcessBuilder(
            "/Users/franciszekjob/Projects/SWM/starknet-foundry/target/release/sncast",
//            "sncast",
            "--hex-format",
            "--json",
            "--accounts-file",
            accountFilePath.absolutePathString(),
            "--account",
            accountName,
            command,
            *(args.toTypedArray()),
            "--url",
            rpcUrl,
        )

        println(processBuilder.command().joinToString(" "))
        processBuilder.directory(File(contractsDirectory.absolutePathString()))

        val process = processBuilder.start()
        process.waitFor()

        val error = String(process.errorStream.readAllBytes())
        requireNoErrors(command, error)

        // As of sncast 0.24.0, declare command returns three response objects
        // First two of them come from "scarb build" output, and don't have "command" field
        // Last object is the actual one we want to return
        // Retrieving the last object works in both cases - with one and with few response objects
        val lines = String(process.inputStream.readAllBytes()).trim().split("\n")
        val result = lines.last()
        println("result: $result")
        return json.decodeFromString(SnCastResponsePolymorphicSerializer, result)
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
}
