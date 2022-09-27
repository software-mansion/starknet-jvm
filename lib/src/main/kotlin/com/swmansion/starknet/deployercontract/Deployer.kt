package com.swmansion.starknet.deployercontract

import com.swmansion.starknet.data.types.Calldata
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.provider.Request
import com.swmansion.starknet.provider.exceptions.RequestFailedException

data class ContractDeployment(
    val transactionHash: Felt,
)

class AddressRetrievalFailedException(message: String, contractDeployment: ContractDeployment) : Exception(message)

/**
 * Universal Deployer Contract module
 *
 * A module for interacting with Universal Deployer Contracts (UDC).
 */
interface Deployer {
    /**
     * Deploy a contract through Universal Deployer Contract (UDC)
     *
     * @param classHash a class hash of the declared contract
     * @param unique set whether deployed contract address should be based on account address or not
     * @param salt a salt to be used to calculate deployed contract address
     * @param constructorCalldata constructor calldata
     *
     * @throws RequestFailedException
     */
    fun deployContract(classHash: Felt, unique: Boolean, salt: Felt, constructorCalldata: Calldata): Request<ContractDeployment>

    /**
     * Get a contract address from the deployment
     *
     * @param contractDeployment a contract deployment
     *
     * @throws RequestFailedException
     * @throws AddressRetrievalFailedException
     */
    fun findContractAddress(contractDeployment: ContractDeployment): Request<Felt>
}
