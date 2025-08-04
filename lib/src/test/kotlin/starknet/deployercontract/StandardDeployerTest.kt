package starknet.deployercontract

import com.swmansion.starknet.account.StandardAccount
import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.deployercontract.StandardDeployer
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

    private lateinit var accountAddress: Felt
    private lateinit var balanceContractClassHash: Felt
    private lateinit var standardDeployer: StandardDeployer

    @JvmStatic
    @BeforeAll
    fun before() {
        try {
            devnetClient.start()

            // Prepare devnet address book
            val accountDetails = devnetClient.createDeployAccount().details
            balanceContractClassHash = devnetClient.declareContract("Balance").classHash
            signer = StarkCurveSigner(accountDetails.privateKey)
            accountAddress = accountDetails.address
            val chainId = provider.getChainId().send()
            standardDeployer = StandardDeployer(
                provider,
                StandardAccount(accountAddress, signer, provider, chainId),
            )
        } catch (ex: Exception) {
            devnetClient.close()
            throw ex
        }
    }

    @Test
    fun testUdcDeployV3() {
        val initialBalance = Felt(1000)
        val deployment = standardDeployer.deployContractV3(
            classHash = balanceContractClassHash,
            unique = true,
            salt = Felt(301),
            constructorCalldata = listOf(initialBalance),
        ).send()
        val address = standardDeployer.findContractAddress(deployment).send()

        val contractValue = provider.callContract(Call(address, "get_balance")).send()
        assertEquals(listOf(initialBalance), contractValue)
    }

    @Test
    fun testUdcDeployV3WithSpecificResourceBounds() {
        val initialBalance = Felt(1000)
        val resourceBounds = ResourceBoundsMapping(
            l1Gas = ResourceBounds(
                maxAmount = Uint64(50000),
                maxPricePerUnit = Uint128(100_000_000_000),
            ),
            l2Gas = ResourceBounds(
                maxAmount = Uint64(50000000),
                maxPricePerUnit = Uint128(100_000_000_000),
            ),
            l1DataGas = ResourceBounds(
                maxAmount = Uint64(50000),
                maxPricePerUnit = Uint128(100_000_000_000),
            ),
        )
        val deployment = standardDeployer.deployContractV3(
            classHash = balanceContractClassHash,
            unique = true,
            salt = Felt(302),
            constructorCalldata = listOf(initialBalance),
            resourceBounds = resourceBounds,
        ).send()
        val address = standardDeployer.findContractAddress(deployment).send()

        val contractValue = provider.callContract(Call(address, "get_balance")).send()
        assertEquals(listOf(initialBalance), contractValue) }

    @Test
    fun testUdcDeployV3WithDefaultParameters() {
        val initialBalance = Felt(1000)
        val deployment = standardDeployer.deployContractV3(
            classHash = balanceContractClassHash,
            constructorCalldata = listOf(initialBalance),
        ).send()
        val address = standardDeployer.findContractAddress(deployment).send()

        val contractValue = provider.callContract(Call(address, "get_balance")).send()
        assertEquals(listOf(initialBalance), contractValue)
    }

    @Test
    fun testUdcDeployV3WithSpecificFeeAndDefaultParameters() {
        val initialBalance = Felt(1000)
        val resourceBounds = ResourceBoundsMapping(
            l1Gas = ResourceBounds(
                maxAmount = Uint64(50000),
                maxPricePerUnit = Uint128(100_000_000_000),
            ),
            l2Gas = ResourceBounds(
                maxAmount = Uint64(50000000),
                maxPricePerUnit = Uint128(100_000_000_000),
            ),
            l1DataGas = ResourceBounds(
                maxAmount = Uint64(50000),
                maxPricePerUnit = Uint128(100_000_000_000),
            ),
        )
        val deployment = standardDeployer.deployContractV3(
            classHash = balanceContractClassHash,
            constructorCalldata = listOf(initialBalance),
            resourceBounds = resourceBounds,
        ).send()
        val address = standardDeployer.findContractAddress(deployment).send()

        val contractValue = provider.callContract(Call(address, "get_balance")).send()
        assertEquals(listOf(initialBalance), contractValue) }
}
