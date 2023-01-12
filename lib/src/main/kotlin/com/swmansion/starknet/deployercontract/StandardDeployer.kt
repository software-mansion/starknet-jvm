package com.swmansion.starknet.deployercontract

import com.swmansion.starknet.account.Account
import com.swmansion.starknet.data.selectorFromName
import com.swmansion.starknet.data.types.Call
import com.swmansion.starknet.data.types.Calldata
import com.swmansion.starknet.data.types.Event
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.transactions.GatewayTransactionReceipt
import com.swmansion.starknet.data.types.transactions.RpcTransactionReceipt
import com.swmansion.starknet.data.types.transactions.TransactionReceipt
import com.swmansion.starknet.extensions.map
import com.swmansion.starknet.provider.Provider
import com.swmansion.starknet.provider.Request

class StandardDeployer(
    private val deployerAddress: Felt,
    private val provider: Provider,
    private val account: Account,
) : Deployer {
    override fun deployContract(
        classHash: Felt,
        unique: Boolean,
        salt: Felt,
        constructorCalldata: Calldata,
    ): Request<ContractDeployment> {
        val feltUnique = if (unique) Felt.ONE else Felt.ZERO
        val invokeCalldata = listOf(classHash, salt, feltUnique, Felt(constructorCalldata.size)) + constructorCalldata
        val call = Call(deployerAddress, "deployContract", invokeCalldata)

        return account.execute(call).map { ContractDeployment(it.transactionHash) }
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
            is RpcTransactionReceipt -> transactionReceipt.events
            is GatewayTransactionReceipt -> transactionReceipt.events
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
}
