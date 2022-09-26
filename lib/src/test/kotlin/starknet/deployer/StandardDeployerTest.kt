package starknet.deployer

import com.swmansion.starknet.account.Account
import com.swmansion.starknet.account.StandardAccount
import com.swmansion.starknet.data.types.BlockTag
import com.swmansion.starknet.data.types.Call
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.StarknetChainId
import com.swmansion.starknet.deployercontract.Deployer
import com.swmansion.starknet.deployercontract.StandardDeployer
import com.swmansion.starknet.provider.Provider
import com.swmansion.starknet.provider.gateway.GatewayProvider
import com.swmansion.starknet.signer.StarkCurveSigner
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import starknet.utils.DevnetClient
import java.nio.file.Path
import starknet.utils.ContractDeployer as TestContractDeployer

object StandardDeployerTest {
    @JvmStatic
    private val devnetClient = DevnetClient(port = 5052)

    private lateinit var testContractDeployer: TestContractDeployer
    private lateinit var contractDeployer: Deployer
    private lateinit var provider: Provider

    @JvmStatic
    @BeforeAll
    fun before() {
        try {
            devnetClient.start()

            testContractDeployer = TestContractDeployer.deployInstance(devnetClient)
            provider = GatewayProvider(
                devnetClient.feederGatewayUrl,
                devnetClient.gatewayUrl,
                StarknetChainId.TESTNET,
            )
            val (classHash, _) = devnetClient.declareContract(Path.of("src/test/resources/compiled/deployer.json"))
            val deployerAddress = testContractDeployer.deployContract(classHash)
            val account = deployAccount()
            contractDeployer = StandardDeployer(deployerAddress, provider, account)
        } catch (ex: Exception) {
            devnetClient.close()
            throw ex
        }
    }

    private fun deployAccount(): Account {
        val (classHash, _) = devnetClient.declareContract(Path.of("src/test/resources/compiled/account.json"))
        val signer = StarkCurveSigner(Felt(1234))
        val accountAddress = testContractDeployer.deployContract(classHash, calldata = listOf(signer.publicKey))
        devnetClient.prefundAccount(accountAddress)
        return StandardAccount(accountAddress, signer, provider)
    }

    @Test
    fun `test udc deploy`() {
        val (classHash, _) = devnetClient.declareContract(Path.of("src/test/resources/compiled/balance.json"))
        val deployment = contractDeployer.deployContract(classHash, Felt(1234), emptyList()).send()
        val address = contractDeployer.findContractAddress(deployment).send()

        assertNotNull(address)
        assertDoesNotThrow { provider.callContract(Call(address, "get_balance"), BlockTag.LATEST).send() }
    }

    @Test
    fun `test udc deploy with constructor`() {
        val constructorValue = Felt(111)
        val (classHash, _) = devnetClient.declareContract(Path.of("src/test/resources/compiled/contractWithConstructor.json"))
        val deployment =
            contractDeployer.deployContract(classHash, Felt(1234), listOf(constructorValue, Felt(789))).send()
        val address = contractDeployer.findContractAddress(deployment).send()

        val contractValue = provider.callContract(Call(address, "get_val1"), BlockTag.LATEST).send()

        assertNotNull(address)
        assertEquals(listOf(constructorValue), contractValue)
    }
}
