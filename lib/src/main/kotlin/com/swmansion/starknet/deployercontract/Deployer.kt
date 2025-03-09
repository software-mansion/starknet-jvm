package com.swmansion.starknet.deployercontract

import com.swmansion.starknet.data.types.Calldata
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.ResourceBoundsMapping
import com.swmansion.starknet.provider.Request
import com.swmansion.starknet.provider.exceptions.RequestFailedException

data class ContractDeployment(
    val transactionHash: Felt,
)

class AddressRetrievalFailedException(message: String, contractDeployment: ContractDeployment) : RuntimeException(message)

class SaltGenerationFailedException : RuntimeException()

/**
 * Universal Deployer Contract module
 *
 * A module for interacting with Universal Deployer Contracts (UDC).
 */
interface Deployer {
    /**
     * Deploy a contract through Universal Deployer Contract (UDC) using version 3 invoke transaction
     *
     * @param classHash a class hash of the declared contract
     * @param unique set whether deployed contract address should be based on account address or not
     * @param salt a salt to be used to calculate deployed contract address
     * @param constructorCalldata constructor calldata
     * @param resourceBounds L1 and L2 resource bounds for the transaction
     *
     * @throws RequestFailedException
     *
     * @sample starknet.deployercontract.StandardDeployerTest.testUdcDeployV3WithSpecificResourceBounds
     */
    fun deployContractV3(
        classHash: Felt,
        unique: Boolean,
        salt: Felt,
        constructorCalldata: Calldata,
        resourceBounds: ResourceBoundsMapping,
    ): Request<ContractDeployment>

    /**
     * Deploy a contract through Universal Deployer Contract (UDC) using version 3 invoke transaction
     *
     * @param classHash a class hash of the declared contract
     * @param unique set whether deployed contract address should be based on account address or not
     * @param salt a salt to be used to calculate deployed contract address
     * @param constructorCalldata constructor calldata
     *
     * @throws RequestFailedException
     *
     * @sample starknet.deployercontract.StandardDeployerTest.testUdcDeployV3
     */
    fun deployContractV3(
        classHash: Felt,
        unique: Boolean,
        salt: Felt,
        constructorCalldata: Calldata,
    ): Request<ContractDeployment>

    /**
     * Deploy a contract through Universal Deployer Contract (UDC) using version 3 invoke transaction
     * with random generated salt and unique parameter set to true
     *
     * @param classHash a class hash of the declared contract
     * @param constructorCalldata constructor calldata
     * @param resourceBounds L1 and L2 resource bounds for the transaction
     *
     * @throws RequestFailedException
     * @throws SaltGenerationFailedException
     *
     * @sample starknet.deployercontract.StandardDeployerTest.testUdcDeployV3WithSpecificFeeAndDefaultParameters
     */
    fun deployContractV3(classHash: Felt, constructorCalldata: Calldata, resourceBounds: ResourceBoundsMapping): Request<ContractDeployment>

    /**
     * Deploy a contract through Universal Deployer Contract (UDC) using version 3 invoke transaction
     * with random generated salt and unique parameter set to true
     *
     * @param classHash a class hash of the declared contract
     * @param constructorCalldata constructor calldata
     * @throws RequestFailedException
     * @throws SaltGenerationFailedException
     *
     * @sample starknet.deployercontract.StandardDeployerTest.testUdcDeployV3WithDefaultParameters
     */
    fun deployContractV3(classHash: Felt, constructorCalldata: Calldata): Request<ContractDeployment>

    /**
     * Get a contract address from the deployment
     *
     * @param contractDeployment a contract deployment
     *
     * @throws RequestFailedException
     * @throws AddressRetrievalFailedException
     *
     * @sample starknet.deployercontract.StandardDeployerTest.testUdcDeployV3
     */
    fun findContractAddress(contractDeployment: ContractDeployment): Request<Felt>
}
