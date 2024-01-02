package starknet.deployercontract

import com.swmansion.starknet.account.StandardAccount
import com.swmansion.starknet.data.types.BlockTag
import com.swmansion.starknet.data.types.Call
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.deployercontract.StandardDeployer
import com.swmansion.starknet.provider.Provider
import com.swmansion.starknet.provider.rpc.JsonRpcProvider
import com.swmansion.starknet.signer.Signer
import com.swmansion.starknet.signer.StarkCurveSigner
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import starknet.utils.DevnetClient
import java.nio.file.Paths

@Execution(ExecutionMode.SAME_THREAD)
object StandardDeployerTest {
    private val devnetClient = DevnetClient(
        port = 5053,
        accountDirectory = Paths.get("src/test/resources/accounts/standard_deployer_test"),
        contractsDirectory = Paths.get("src/test/resources/contracts"),
    )
    private val provider = JsonRpcProvider(devnetClient.rpcUrl)

    private lateinit var signer: Signer

    private val deployerAddress = DevnetClient.udcContractAddress
    private lateinit var accountAddress: Felt
    private lateinit var balanceContractClassHash: Felt
    private lateinit var standardDeployer: StandardDeployer

    @JvmStatic
    @BeforeAll
    fun before() {
        try {
            devnetClient.start()
            balanceContractClassHash = devnetClient.declareContract("Balance").classHash

            // Prepare devnet address book
            val accountDetails = devnetClient.deployAccount("standard_deployer_test", prefund = true).details
            signer = StarkCurveSigner(accountDetails.privateKey)
            accountAddress = accountDetails.address
            standardDeployer = StandardDeployer(
                deployerAddress,
                provider,
                StandardAccount(accountAddress, signer, provider),
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

    @Test
    fun `test udc deploy`() {
        val initialBalance = Felt(1000)
        val deployment = standardDeployer.deployContract(
            classHash = balanceContractClassHash,
            unique = true,
            salt = Felt(1234),
            constructorCalldata = listOf(initialBalance),
        ).send()
        val address = standardDeployer.findContractAddress(deployment).send()

        assertDoesNotThrow { provider.callContract(Call(address, "get_balance"), BlockTag.LATEST).send() }
    }

    @Test
    fun `test udc deploy with specific fee`() {
        val initialBalance = Felt(1000)
        val deployment = standardDeployer.deployContract(
            classHash = balanceContractClassHash,
            unique = true,
            salt = Felt(789),
            constructorCalldata = listOf(initialBalance),
            maxFee = Felt(1_000_000_000_000_000),
        ).send()
        val address = standardDeployer.findContractAddress(deployment).send()

        assertDoesNotThrow { provider.callContract(Call(address, "get_balance"), BlockTag.LATEST).send() }
    }

    @Test
    fun `test udc deploy with default parameters`() {
        val initialBalance = Felt(1000)
        val deployment = standardDeployer.deployContract(
            classHash = balanceContractClassHash,
            constructorCalldata = listOf(initialBalance),
        ).send()
        val address = standardDeployer.findContractAddress(deployment).send()

        assertDoesNotThrow { provider.callContract(Call(address, "get_balance"), BlockTag.LATEST).send() }
    }

    @Test
    fun `test udc deploy with specific fee and default parameters`() {
        val initialBalance = Felt(1000)
        val deployment = standardDeployer.deployContract(
            classHash = balanceContractClassHash,
            constructorCalldata = listOf(initialBalance),
            maxFee = Felt(1_000_000_000_000_000),
        ).send()
        val address = standardDeployer.findContractAddress(deployment).send()

        assertDoesNotThrow { provider.callContract(Call(address, "get_balance"), BlockTag.LATEST).send() }
    }

    @Test
    fun `test udc deploy with constructor`() {
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
