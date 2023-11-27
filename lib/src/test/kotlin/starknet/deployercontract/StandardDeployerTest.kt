package starknet.deployercontract

import com.swmansion.starknet.account.StandardAccount
import com.swmansion.starknet.data.types.BlockTag
import com.swmansion.starknet.data.types.Call
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.StarknetChainId
import com.swmansion.starknet.deployercontract.StandardDeployer
import com.swmansion.starknet.provider.Provider
import com.swmansion.starknet.provider.rpc.JsonRpcProvider
import com.swmansion.starknet.signer.Signer
import com.swmansion.starknet.signer.StarkCurveSigner
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import starknet.utils.DevnetClient
import java.nio.file.Paths

@Execution(ExecutionMode.SAME_THREAD)
object StandardDeployerTest {
    private val devnetClient = DevnetClient(
        port = 5053,
        accountDirectory = Paths.get("src/test/resources/accounts/standard_deployer_test"),
        contractsDirectory = Paths.get("src/test/resources/contracts"),
    )
    private val rpcProvider = JsonRpcProvider(
        devnetClient.rpcUrl,
        StarknetChainId.TESTNET,
    )

    private lateinit var signer: Signer

    private lateinit var devnetAddressBook: AddressBook

    @JvmStatic
    @BeforeAll
    fun before() {
        try {
            devnetClient.start()
            val balanceContractClassHash = devnetClient.declareContract("Balance").classHash

            // Prepare devnet address book
            val accountDetails = devnetClient.createDeployAccount("standard_account_test").details
            signer = StarkCurveSigner(accountDetails.privateKey)
            devnetAddressBook = AddressBook(
                deployerAddress = DevnetClient.udcContractAddress,
                accountAddress = accountDetails.address,
                balanceContractClassHash = balanceContractClassHash,
            )
        } catch (ex: Exception) {
            devnetClient.close()
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
                    devnetAddressBook.deployerAddress,
                    rpcProvider,
                    StandardAccount(devnetAddressBook.accountAddress, signer, rpcProvider),
                ),
                rpcProvider,
                devnetAddressBook,
            ),
        )
    }

    @ParameterizedTest
    @MethodSource("getStandardDeployerParameters")
    fun `test udc deploy`(standardDeployerParameters: StandardDeployerParameters) {
        val standardDeployer = standardDeployerParameters.standardDeployer
        val provider = standardDeployerParameters.provider
        val classHash = standardDeployerParameters.addressBook.balanceContractClassHash

        val initialBalance = Felt(1000)
        val deployment = standardDeployer.deployContract(
            classHash = classHash,
            unique = true,
            salt = Felt(1234),
            constructorCalldata = listOf(initialBalance),
        ).send()
        val address = standardDeployer.findContractAddress(deployment).send()

        assertDoesNotThrow { provider.callContract(Call(address, "get_balance"), BlockTag.LATEST).send() }
    }

    @ParameterizedTest
    @MethodSource("getStandardDeployerParameters")
    fun `test udc deploy with default parameters`(standardDeployerParameters: StandardDeployerParameters) {
        val standardDeployer = standardDeployerParameters.standardDeployer
        val provider = standardDeployerParameters.provider
        val classHash = standardDeployerParameters.addressBook.balanceContractClassHash

        val initialBalance = Felt(1000)
        val deployment = standardDeployer.deployContract(
            classHash = classHash,
            constructorCalldata = listOf(initialBalance),
        ).send()
        val address = standardDeployer.findContractAddress(deployment).send()

        assertDoesNotThrow { provider.callContract(Call(address, "get_balance"), BlockTag.LATEST).send() }
    }

    @ParameterizedTest
    @MethodSource("getStandardDeployerParameters")
    fun `test udc deploy with constructor`(standardDeployerParameters: StandardDeployerParameters) {
        val standardDeployer = standardDeployerParameters.standardDeployer
        val provider = standardDeployerParameters.provider

        val classHash = devnetClient.declareContract("ContractWithConstructor").classHash
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
