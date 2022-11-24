package com.swmansion.starknet.data

import com.swmansion.starknet.crypto.StarknetCurve
import com.swmansion.starknet.data.types.Calldata
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.StarknetChainId
import com.swmansion.starknet.data.types.transactions.TransactionType

/**
 * Toolkit for calculating hashes of transactions.
 */
object TransactionHashCalculator {
    /**
     * Calculate hash of invoke function transaction.
     *
     * @param contractAddress address of account that executes transaction
     * @param calldata calldata sent to the account
     * @param chainId id of the chain used
     * @param version version of the tx
     * @param nonce account's nonce
     * @param maxFee maximum fee that account will use for the execution
     */
    @JvmStatic
    fun calculateInvokeTxHash(
        contractAddress: Felt,
        calldata: Calldata,
        chainId: StarknetChainId,
        version: Felt,
        nonce: Felt,
        maxFee: Felt,
    ): Felt = transactionHashCommon(
        txType = TransactionType.INVOKE,
        version = version,
        contractAddress = contractAddress,
        entryPointSelector = Felt.ZERO,
        calldata = calldata,
        maxFee = maxFee,
        chainId = chainId,
        nonce = nonce,
    )

    /**
     * Calculate hash of deploy account transaction.
     *
     * @param classHash hash of the contract code
     * @param calldata constructor calldata used for deployment
     * @param salt salt used to calculate address
     * @param chainId id of the chain used
     * @param version version of the tx
     * @param maxFee maximum fee that account will use for the execution
     */
    @JvmStatic
    fun calculateDeployAccountTxHash(
        classHash: Felt,
        calldata: Calldata,
        salt: Felt,
        chainId: StarknetChainId,
        version: Felt,
        maxFee: Felt,
    ): Felt {
        val contractAddress = ContractAddressCalculator.calculateAddressFromHash(
            classHash = classHash,
            calldata = calldata,
            salt = salt,
        )
        return transactionHashCommon(
            txType = TransactionType.DEPLOY_ACCOUNT,
            version = version,
            contractAddress = contractAddress,
            entryPointSelector = Felt.ZERO,
            calldata = listOf(classHash, salt, *calldata.toTypedArray()),
            maxFee = maxFee,
            chainId = chainId,
            nonce = Felt.ZERO,
        )
    }

    @JvmStatic
    fun calculateDeclareTxHash(
        classHash: Felt,
        chainId: StarknetChainId,
        senderAddress: Felt,
        maxFee: Felt,
        version: Felt,
        nonce: Felt,
    ): Felt {
        return transactionHashCommon(
            txType = TransactionType.DECLARE,
            version = version,
            contractAddress = senderAddress,
            entryPointSelector = Felt.ZERO,
            calldata = listOf(classHash),
            maxFee = maxFee,
            chainId = chainId,
            nonce = nonce,
        )
    }

    private fun transactionHashCommon(
        txType: TransactionType,
        version: Felt,
        contractAddress: Felt,
        entryPointSelector: Felt,
        calldata: Calldata,
        maxFee: Felt,
        chainId: StarknetChainId,
        nonce: Felt,
    ): Felt = StarknetCurve.pedersenOnElements(
        txType.txPrefix,
        version,
        contractAddress,
        entryPointSelector,
        StarknetCurve.pedersenOnElements(calldata),
        maxFee,
        chainId.value,
        nonce,
    )
}
