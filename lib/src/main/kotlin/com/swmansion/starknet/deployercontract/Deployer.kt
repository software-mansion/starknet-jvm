package com.swmansion.starknet.deployercontract

import com.swmansion.starknet.data.types.Calldata
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.provider.Request
import com.swmansion.starknet.provider.exceptions.RequestFailedException
import java.security.SecureRandom

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
     * Deploy a contract through Universal Deployer Contract (UDC)
     *
     * @param classHash a class hash of the declared contract
     * @param unique set whether deployed contract address should be based on account address or not
     * @param salt a salt to be used to calculate deployed contract address
     * @param constructorCalldata constructor calldata
     * @param maxFee maximum fee that account will use for the deployment
     *
     * @throws RequestFailedException
     */
    fun deployContract(
        classHash: Felt,
        unique: Boolean,
        salt: Felt,
        constructorCalldata: Calldata,
        maxFee: Felt,
    ): Request<ContractDeployment>

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
    fun deployContract(
        classHash: Felt,
        unique: Boolean,
        salt: Felt,
        constructorCalldata: Calldata,
    ): Request<ContractDeployment>

    /**
     * Deploy a contract through Universal Deployer Contract (UDC) with random generated salt and
     * unique parameter set to true
     *
     * @param classHash a class hash of the declared contract
     * @param constructorCalldata constructor calldata
     * @param maxFee maximum fee that account will use for the deployment
     * @throws RequestFailedException
     * @throws SaltGenerationFailedException
     */
    fun deployContract(classHash: Felt, constructorCalldata: Calldata, maxFee: Felt): Request<ContractDeployment> {
        val random = SecureRandom()
        val salt = random.longs(1, 1, Long.MAX_VALUE).findFirst().orElseThrow { SaltGenerationFailedException() }
        val feltSalt = Felt(salt)
        return deployContract(classHash, true, feltSalt, constructorCalldata, maxFee)
    }

    /**
     * Deploy a contract through Universal Deployer Contract (UDC) with random generated salt and
     * unique parameter set to true
     *
     * @param classHash a class hash of the declared contract
     * @param constructorCalldata constructor calldata
     * @throws RequestFailedException
     * @throws SaltGenerationFailedException
     */
    fun deployContract(classHash: Felt, constructorCalldata: Calldata): Request<ContractDeployment> {
        val random = SecureRandom()
        val salt = random.longs(1, 1, Long.MAX_VALUE).findFirst().orElseThrow { SaltGenerationFailedException() }
        val feltSalt = Felt(salt)
        return deployContract(classHash, true, feltSalt, constructorCalldata)
    }

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
