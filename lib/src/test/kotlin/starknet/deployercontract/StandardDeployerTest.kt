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
import com.swmansion.starknet.signer.StarkCurveSigner
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import starknet.utils.DevnetClient
import java.nio.file.Path
import starknet.utils.ContractDeployer as TestContractDeployer

object StandardDeployerTest {
    @JvmStatic
    private val devnetClient = DevnetClient(port = 5052)
    private val signer = StarkCurveSigner(Felt(1234))

    private lateinit var testContractDeployer: TestContractDeployer
    private lateinit var gatewayProvider: Provider
    private lateinit var rpcProvider: Provider
    private lateinit var accountAddress: Felt
    private lateinit var rpcDeployerAddress: Felt
    private lateinit var gatewayDeployerAddress: Felt

    @JvmStatic
    @BeforeAll
    fun before() {
        try {
            devnetClient.start()

            testContractDeployer = TestContractDeployer.deployInstance(devnetClient)
            gatewayProvider = GatewayProvider(
                devnetClient.feederGatewayUrl,
                devnetClient.gatewayUrl,
                StarknetChainId.TESTNET,
            )

            rpcProvider = JsonRpcProvider(
                devnetClient.rpcUrl,
                StarknetChainId.TESTNET,
            )

            val (classHash, _) = devnetClient.declareContract(Path.of("src/test/resources/compiled/deployer.json"))
            rpcDeployerAddress = testContractDeployer.deployContract(classHash)
            gatewayDeployerAddress = testContractDeployer.deployContract(classHash)

            deployAccount()
        } catch (ex: Exception) {
            devnetClient.close()
            throw ex
        }
    }

    private fun deployAccount() {
        val (classHash, _) = devnetClient.declareContract(Path.of("src/test/resources/compiled/account.json"))
        accountAddress = testContractDeployer.deployContract(classHash, calldata = listOf(signer.publicKey))
        devnetClient.prefundAccount(accountAddress)
    }

    data class StandardDeployerAndProvider(val standardDeployer: StandardDeployer, val provider: Provider)

    @JvmStatic
    fun getStandardDeployerAndProvider(): List<StandardDeployerAndProvider> {
        return listOf(
            StandardDeployerAndProvider(
                StandardDeployer(
                    gatewayDeployerAddress,
                    gatewayProvider,
                    StandardAccount(accountAddress, signer, gatewayProvider),
                ),
                gatewayProvider,
            ),
            StandardDeployerAndProvider(
                StandardDeployer(
                    rpcDeployerAddress,
                    rpcProvider,
                    StandardAccount(accountAddress, signer, rpcProvider),
                ),
                rpcProvider,
            ),
        )
    }

    @ParameterizedTest
    @MethodSource("getStandardDeployerAndProvider")
    fun `test udc deploy`(standardDeployerAndProvider: StandardDeployerAndProvider) {
        val (standardDeployer, provider) = standardDeployerAndProvider
        val (classHash, _) = devnetClient.declareContract(Path.of("src/test/resources/compiled/balance.json"))
        val deployment = standardDeployer.deployContract(classHash, true, Felt(1234), emptyList()).send()

        val address = standardDeployer.findContractAddress(deployment).send()

        assertNotNull(address)
        assertDoesNotThrow { provider.callContract(Call(address, "get_balance"), BlockTag.LATEST).send() }
    }

    @ParameterizedTest
    @MethodSource("getStandardDeployerAndProvider")
    fun `test udc deploy with default parameters`(standardDeployerAndProvider: StandardDeployerAndProvider) {
        val (standardDeployer, provider) = standardDeployerAndProvider
        val (classHash, _) = devnetClient.declareContract(Path.of("src/test/resources/compiled/balance.json"))
        val deployment = standardDeployer.deployContract(classHash, emptyList()).send()
        val address = standardDeployer.findContractAddress(deployment).send()

        assertNotNull(address)
        assertDoesNotThrow { provider.callContract(Call(address, "get_balance"), BlockTag.LATEST).send() }
    }

    @ParameterizedTest
    @MethodSource("getStandardDeployerAndProvider")
    fun `test udc deploy with constructor`(standardDeployerAndProvider: StandardDeployerAndProvider) {
        val (standardDeployer, provider) = standardDeployerAndProvider
        val constructorValue = Felt(111)
        val (classHash, _) = devnetClient.declareContract(Path.of("src/test/resources/compiled/contractWithConstructor.json"))
        val deployment =
            standardDeployer.deployContract(classHash, true, Felt(1234), listOf(constructorValue, Felt(789))).send()
        val address = standardDeployer.findContractAddress(deployment).send()

        val contractValue = provider.callContract(Call(address, "get_val1"), BlockTag.LATEST).send()

        assertNotNull(address)
        assertEquals(listOf(constructorValue), contractValue)
    }
}
