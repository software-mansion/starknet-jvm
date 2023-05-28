package starknet.utils

import com.swmansion.starknet.data.selectorFromName
import com.swmansion.starknet.data.types.Felt
import java.nio.file.Path
import kotlin.random.Random

class ContractDeploymentFailedException() : Exception()

class ContractDeployer(
    private val address: Felt,
    private val devnetClient: DevnetClient,
) {
    fun deployContract(
        classHash: Felt,
        salt: Felt = Felt(Random.nextLong(1L, Long.MAX_VALUE)),
        unique: Boolean = false,
        calldata: List<Felt> = emptyList(),
    ): Felt {
        val invokeCalldata =
            listOf(classHash, salt, if (unique) Felt.ONE else Felt.ZERO, Felt(calldata.size)) + calldata
        val (_, transactionHash) = devnetClient.invokeTransaction(
            "deployContract",
            address,
            Path.of("src/test/resources/compiled_v0/deployerAbi.json"),
            invokeCalldata,
        )
        val receipt = devnetClient.transactionReceipt(transactionHash)
        val deployEvent = receipt.events.stream()
            .filter { it.keys.contains(selectorFromName("ContractDeployed")) }
            .findFirst()
            .orElseThrow { ContractDeploymentFailedException() }
        return deployEvent.data.first()
    }

    companion object {
        fun deployInstance(devnetClient: DevnetClient): ContractDeployer {
            val (deployerAddress, _) = devnetClient.deployContract(Path.of("src/test/resources/compiled_v0/deployer.json"))
            return ContractDeployer(deployerAddress, devnetClient)
        }
    }
}
