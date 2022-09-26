package com.swmansion.starknet.deployer

import com.swmansion.starknet.account.Account
import com.swmansion.starknet.data.selectorFromName
import com.swmansion.starknet.data.types.Call
import com.swmansion.starknet.data.types.Calldata
import com.swmansion.starknet.data.types.Event
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.transactions.GatewayTransactionReceipt
import com.swmansion.starknet.data.types.transactions.InvokeTransactionReceipt
import com.swmansion.starknet.data.types.transactions.TransactionReceipt
import com.swmansion.starknet.extensions.map
import com.swmansion.starknet.provider.Provider
import com.swmansion.starknet.provider.Request

class StandardDeployer(
    private val deployerAddress: Felt,
    private val provider: Provider,
    private val account: Account,
) : Deployer {
    override fun deployContract(classHash: Felt, salt: Felt, calldata: Calldata): Request<ContractDeployment> {
        val invokeCalldata = listOf(classHash, salt, Felt.ONE, Felt(calldata.size)) + calldata
        val call = Call(deployerAddress, "deployContract", invokeCalldata)

        return account.execute(call).map { ContractDeployment(it.transactionHash) }
    }

    override fun findContractAddress(contractDeployment: ContractDeployment): Request<Felt> {
        return provider.getTransactionReceipt(contractDeployment.transactionHash).map { receipt ->
            val event = getDeploymentEvent(receipt)
                ?: throw AddressRetrievalFailedException("No deployment events found for contract deployment $contractDeployment")
            event.data[0]
        }
    }

    private fun getDeploymentEvent(transactionReceipt: TransactionReceipt): Event? {
        val events = when (transactionReceipt) {
            is InvokeTransactionReceipt -> transactionReceipt.events
            is GatewayTransactionReceipt -> transactionReceipt.events
            else -> throw AddressRetrievalFailedException("Invalid transaction type")
        }
        return events.find { it.keys.contains(selectorFromName("ContractDeployed")) }
    }
}
