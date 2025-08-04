package com.swmansion.starknet.deployercontract

import com.swmansion.starknet.account.Account
import com.swmansion.starknet.data.selectorFromName
import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.extensions.map
import com.swmansion.starknet.extensions.toFelt
import com.swmansion.starknet.provider.Provider
import com.swmansion.starknet.provider.Request
import java.security.SecureRandom

private const val UDC_ADDRESS = "0x02ceed65a4bd731034c01113685c831b01c15d7d432f71afb1cf1634b53a2125"

class StandardDeployer(
    private val deployerAddress: Felt,
    private val provider: Provider,
    private val account: Account,
) : Deployer {
    constructor(
        provider: Provider,
        account: Account,
    ) : this(
            Felt.fromHex(UDC_ADDRESS),
            provider,
            account
        )

    override fun deployContractV3(
        classHash: Felt,
        unique: Boolean,
        salt: Felt,
        constructorCalldata: Calldata,
        resourceBounds: ResourceBoundsMapping,
    ): Request<ContractDeployment> {
        val call = buildDeployContractCall(classHash, unique, salt, constructorCalldata)

        return account.executeV3(call, resourceBounds).map { ContractDeployment(it.transactionHash) }
    }

    override fun deployContractV3(
        classHash: Felt,
        unique: Boolean,
        salt: Felt,
        constructorCalldata: Calldata,
    ): Request<ContractDeployment> {
        val call = buildDeployContractCall(classHash, unique, salt, constructorCalldata)

        return account.executeV3(call).map { ContractDeployment(it.transactionHash) }
    }

    override fun deployContractV3(classHash: Felt, constructorCalldata: Calldata, resourceBounds: ResourceBoundsMapping): Request<ContractDeployment> {
        val salt = randomSalt()
        return deployContractV3(classHash, true, salt, constructorCalldata, resourceBounds)
    }

    override fun deployContractV3(classHash: Felt, constructorCalldata: Calldata): Request<ContractDeployment> {
        val salt = randomSalt()
        return deployContractV3(classHash, true, salt, constructorCalldata)
    }

    private fun buildDeployContractCall(
        classHash: Felt,
        unique: Boolean,
        salt: Felt,
        constructorCalldata: Calldata,
    ): Call {
        val feltUnique = if (unique) Felt.ONE else Felt.ZERO
        val invokeCalldata = listOf(classHash, salt, feltUnique, Felt(constructorCalldata.size)) + constructorCalldata
        return Call(deployerAddress, "deployContract", invokeCalldata)
    }

    override fun findContractAddress(contractDeployment: ContractDeployment): Request<Felt> {
        return provider.getTransactionReceipt(contractDeployment.transactionHash).map { receipt ->
            val event = getDeploymentEvent(receipt, contractDeployment)
                ?: throw AddressRetrievalFailedException("No deployment events found", contractDeployment)
            event.data[0]
        }
    }

    private fun getDeploymentEvent(
        transactionReceipt: TransactionReceipt,
        contractDeployment: ContractDeployment,
    ): Event? {
        val events = when (transactionReceipt) {
            is InvokeTransactionReceipt -> transactionReceipt.events
            is DeployTransactionReceipt -> transactionReceipt.events
            is DeployAccountTransactionReceipt -> transactionReceipt.events
            else -> throw AddressRetrievalFailedException("Invalid transaction type", contractDeployment)
        }
        val deploymentEvents = events.filter { it.keys.contains(selectorFromName("ContractDeployed")) }
        if (deploymentEvents.size > 1) {
            throw AddressRetrievalFailedException(
                "Transaction contains multiple deployment events which cannot be distinguished.",
                contractDeployment,
            )
        }
        return deploymentEvents.firstOrNull()
    }

    private fun randomSalt() = SecureRandom().longs(1, 1, Long.MAX_VALUE)
        .findFirst()
        .orElseThrow { SaltGenerationFailedException() }
        .toFelt
}
