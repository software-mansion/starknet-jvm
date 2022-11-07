package com.swmansion.starknet.data

import com.swmansion.starknet.crypto.StarknetCurve
import com.swmansion.starknet.data.types.Calldata
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.StarknetChainId
import com.swmansion.starknet.data.types.transactions.TransactionType

object Hashing {
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

    @JvmStatic
    fun calculateDeployAccountTxHash(
        classHash: Felt,
        salt: Felt,
        calldata: Calldata,
        chainId: StarknetChainId,
        version: Felt,
        nonce: Felt,
        maxFee: Felt = Felt.ZERO,
    ): Felt {
        val contractAddress = ContractAddress.calculateAddressFromHash(
            salt = salt,
            classHash = classHash,
            calldata = calldata,
        )
        return transactionHashCommon(
            txType = TransactionType.DEPLOY_ACCOUNT,
            version = version,
            contractAddress = contractAddress,
            entryPointSelector = Felt.ZERO,
            calldata = listOf(classHash, salt, *calldata.toTypedArray()),
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
