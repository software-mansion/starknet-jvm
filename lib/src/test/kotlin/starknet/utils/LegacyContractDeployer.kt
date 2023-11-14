package starknet.utils

import com.swmansion.starknet.data.selectorFromName
import com.swmansion.starknet.data.types.Felt
import java.nio.file.Path
import kotlin.random.Random

class LegacyContractDeploymentFailedException() : Exception()

class LegacyContractDeployer(
    private val address: Felt,
    private val legacyDevnetClient: LegacyDevnetClient,
) {
    fun deployContract(
        classHash: Felt,
        salt: Felt = Felt(Random.nextLong(1L, Long.MAX_VALUE)),
        unique: Boolean = false,
        calldata: List<Felt> = emptyList(),
    ): Felt {
        val invokeCalldata =
            listOf(
                classHash,
                salt,
                if (unique) Felt.ONE else Felt.ZERO,
                Felt(calldata.size),
            ) + calldata
        val transactionHash = legacyDevnetClient.invokeTransaction(
            "deployContract",
            address,
            Path.of("src/test/resources/contracts_v0/target/release/deployerAbi.json"),
            invokeCalldata,
        ).transactionHash
        val receipt = legacyDevnetClient.transactionReceipt(transactionHash)
        val deployEvent = receipt.events.stream()
            .filter { it.keys.contains(selectorFromName("ContractDeployed")) }
            .findFirst()
            .orElseThrow { LegacyContractDeploymentFailedException() }
        return deployEvent.data.first()
    }

    companion object {
        fun deployInstance(legacyDevnetClient: LegacyDevnetClient): LegacyContractDeployer {
            val deployerAddress = legacyDevnetClient.deployContract(Path.of("src/test/resources/contracts_v0/target/release/deployer.json")).address
            return LegacyContractDeployer(deployerAddress, legacyDevnetClient)
        }
    }
}
