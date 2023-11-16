package starknet.deployercontract

import com.swmansion.starknet.account.StandardAccount
import com.swmansion.starknet.data.types.BlockTag
import com.swmansion.starknet.data.types.Call
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.StarknetChainId
import com.swmansion.starknet.deployercontract.StandardDeployer
import com.swmansion.starknet.provider.Provider
import com.swmansion.starknet.provider.gateway.GatewayProvider
import com.swmansion.starknet.provider.rpc.JsonRpcProvider
import com.swmansion.starknet.signer.Signer
import com.swmansion.starknet.signer.StarkCurveSigner
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import starknet.utils.DevnetClient
import starknet.utils.LegacyDevnetClient
import java.nio.file.Path
import java.nio.file.Paths
import starknet.utils.LegacyContractDeployer as TestContractDeployer

@Execution(ExecutionMode.SAME_THREAD)
object StandardDeployerTest {
    @JvmStatic
    private val legacyDevnetClient = LegacyDevnetClient(
        port = 5053,
        accountDirectory = Paths.get("src/test/resources/accounts_legacy/standard_deployer_test"),
    )
    private val devnetClient = DevnetClient(
        port = 5063,
        accountDirectory = Paths.get("src/test/resources/accounts/standard_deployer_test"),
        contractsDirectory = Paths.get("src/test/resources/contracts"),
    )
    private val rpcProvider = JsonRpcProvider(
        devnetClient.rpcUrl,
        StarknetChainId.TESTNET,
    )
    private val legacyGatewayProvider = GatewayProvider(
        legacyDevnetClient.feederGatewayUrl,
        legacyDevnetClient.gatewayUrl,
        StarknetChainId.TESTNET,
    )

    private lateinit var legacySigner: Signer
    private lateinit var signer: Signer

    private lateinit var legacyTestContractDeployer: TestContractDeployer
    private lateinit var legacyDevnetAddressBook: AddressBook
    private lateinit var devnetAddressBook: AddressBook

    @JvmStatic
    @BeforeAll
    fun before() {
        try {
            devnetClient.start()
            legacyDevnetClient.start()
            val balanceContractClassHash = devnetClient.declareContract("Balance").classHash
            val legacyBalanceContractClassHash = legacyDevnetClient.declareContract(Path.of("src/test/resources/contracts_v0/target/release/balance.json")).address

            // Prepare devnet address book
            val accountDetails = devnetClient.createDeployAccount("standard_account_test").details
            signer = StarkCurveSigner(accountDetails.privateKey)
            devnetAddressBook = AddressBook(
                deployerAddress = DevnetClient.udcContractAddress,
                accountAddress = accountDetails.address,
                balanceContractClassHash = balanceContractClassHash,
            )

            // Prepare legacy devnet address book
            legacyTestContractDeployer = TestContractDeployer.deployInstance(legacyDevnetClient)
            val legacyDeployerClassHash = legacyDevnetClient.declareContract(Path.of("src/test/resources/contracts_v0/target/release/deployer.json")).address
            val legacyGatewayDeployerAddress = legacyTestContractDeployer.deployContract(legacyDeployerClassHash)

            legacySigner = StarkCurveSigner(Felt(1234))
            val legacyAccountClassHash = legacyDevnetClient.declareContract(Path.of("src/test/resources/contracts_v0/target/release/account.json")).address
            val legacyAccountAddress = legacyTestContractDeployer.deployContract(legacyAccountClassHash, calldata = listOf(legacySigner.publicKey))
            legacyDevnetClient.prefundAccount(legacyAccountAddress)

            legacyDevnetAddressBook = AddressBook(
                deployerAddress = legacyGatewayDeployerAddress,
                accountAddress = legacyAccountAddress,
                balanceContractClassHash = legacyBalanceContractClassHash,
            )
        } catch (ex: Exception) {
            devnetClient.close()
            legacyDevnetClient.close()
            throw ex
        }
    }

    data class StandardDeployerParameters(
        val standardDeployer: StandardDeployer,
        val provider: Provider,
        val addressBook: AddressBook,
    )
    data class AddressBook(
        val deployerAddress: Felt,
        val accountAddress: Felt,
        val balanceContractClassHash: Felt,
    )

    @JvmStatic
    fun getStandardDeployerParameters(): List<StandardDeployerParameters> {
        return listOf(
            StandardDeployerParameters(
                StandardDeployer(
                    legacyDevnetAddressBook.deployerAddress,
                    legacyGatewayProvider,
                    StandardAccount(legacyDevnetAddressBook.accountAddress, legacySigner, legacyGatewayProvider),
                ),
                legacyGatewayProvider,
                legacyDevnetAddressBook,
            ),
            StandardDeployerParameters(
                StandardDeployer(
                    devnetAddressBook.deployerAddress,
                    rpcProvider,
                    StandardAccount(devnetAddressBook.accountAddress, signer, rpcProvider),
                ),
                rpcProvider,
                devnetAddressBook,
            ),
        )
    }

    // TODO (#351): Enable this test once invoke transactions are fixed on devnet
    @Disabled("Pending invoke fix on devnet")
    @ParameterizedTest
    @MethodSource("getStandardDeployerParameters")
    fun `test udc deploy`(standardDeployerParameters: StandardDeployerParameters) {
        val standardDeployer = standardDeployerParameters.standardDeployer
        val provider = standardDeployerParameters.provider
        val classHash = standardDeployerParameters.addressBook.balanceContractClassHash

        val deployment = standardDeployer.deployContract(
            classHash = classHash,
            unique = true,
            salt = Felt(1234),
            constructorCalldata = emptyList(),
        ).send()
        val address = standardDeployer.findContractAddress(deployment).send()

        assertDoesNotThrow { provider.callContract(Call(address, "get_balance"), BlockTag.LATEST).send() }
    }

    // TODO (#351): Enable this test once invoke transactions are fixed on devnet
    @Disabled("Pending invoke fix on devnet")
    @ParameterizedTest
    @MethodSource("getStandardDeployerParameters")
    fun `test udc deploy with default parameters`(standardDeployerParameters: StandardDeployerParameters) {
        val standardDeployer = standardDeployerParameters.standardDeployer
        val provider = standardDeployerParameters.provider
        val classHash = standardDeployerParameters.addressBook.balanceContractClassHash

        val deployment = standardDeployer.deployContract(
            classHash = classHash,
            constructorCalldata = emptyList(),
        ).send()
        val address = standardDeployer.findContractAddress(deployment).send()

        assertDoesNotThrow { provider.callContract(Call(address, "get_balance"), BlockTag.LATEST).send() }
    }

    @ParameterizedTest
    @MethodSource("getStandardDeployerParameters")
    fun `test udc deploy with constructor`(standardDeployerParameters: StandardDeployerParameters) {
        val standardDeployer = standardDeployerParameters.standardDeployer
        val provider = standardDeployerParameters.provider

        val classHash = when (provider) {
            is GatewayProvider -> legacyDevnetClient.declareContract(Path.of("src/test/resources/contracts_v0/target/release/contractWithConstructor.json")).address
            is JsonRpcProvider -> devnetClient.declareContract("ContractWithConstructor").classHash
            else -> throw IllegalStateException("Unknown provider type")
        }
        val constructorVal1 = Felt(451)

        val deployment = standardDeployer.deployContract(
            classHash = classHash,
            unique = true,
            salt = Felt(1234),
            constructorCalldata = listOf(
                constructorVal1,
                Felt(789), // Dummy value, ignored by the contract constructor.
            ),
        ).send()
        val address = standardDeployer.findContractAddress(deployment).send()

        val contractValue = provider.callContract(Call(address, "get_val1"), BlockTag.LATEST).send()

        assertEquals(listOf(constructorVal1), contractValue)
    }
}
