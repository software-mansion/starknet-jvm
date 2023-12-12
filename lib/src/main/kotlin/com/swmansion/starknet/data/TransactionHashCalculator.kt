package com.swmansion.starknet.data

import com.swmansion.starknet.crypto.Poseidon
import com.swmansion.starknet.crypto.StarknetCurve
import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.data.types.transactions.DAMode
import com.swmansion.starknet.data.types.transactions.TransactionType
import com.swmansion.starknet.extensions.toFelt

/**
 * Toolkit for calculating hashes of transactions.
 */
object TransactionHashCalculator {
    @JvmStatic
    fun calculateInvokeTxV1Hash(
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
    fun calculateInvokeTxV3Hash(
        senderAddress: Felt,
        calldata: Calldata,
        chainId: StarknetChainId,
        version: Felt,
        nonce: Felt,
        tip: Uint64,
        resourceBounds: ResourceBoundsMapping,
        paymasterData: PaymasterData,
        accountDeploymentData: AccountDeploymentData,
        feeDataAvailabilityMode: DAMode,
        nonceDataAvailabilityMode: DAMode,
    ): Felt {
        return Poseidon.poseidonHash(
            *CommonTransanctionV3Fields(
                txType = TransactionType.INVOKE,
                version = version,
                address = senderAddress,
                tip = tip,
                resourceBounds = resourceBounds,
                paymasterData = paymasterData,
                chainId = chainId,
                nonce = nonce,
                nonceDataAvailabilityMode = nonceDataAvailabilityMode,
                feeDataAvailabilityMode = feeDataAvailabilityMode,
            ).toTypedArray(),
            Poseidon.poseidonHash(accountDeploymentData),
            Poseidon.poseidonHash(calldata),
        )
    }

    @JvmStatic
    fun calculateDeployAccountV1TxHash(
        classHash: Felt,
        calldata: Calldata,
        salt: Felt,
        chainId: StarknetChainId,
        version: Felt,
        maxFee: Felt,
        nonce: Felt,
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
            nonce = nonce,
        )
    }

    @JvmStatic
    fun calculateDeployAccountV3TxHash(
        classHash: Felt,
        senderAddress: Felt,
        constructorCalldata: Calldata,
        salt: Felt,
        paymasterData: PaymasterData,
        chainId: StarknetChainId,
        version: Felt,
        nonce: Felt,
        contractAddressSalt: Felt,
        tip: Uint64,
        resourceBounds: ResourceBoundsMapping,
        feeDataAvailabilityMode: DAMode,
        nonceDataAvailabilityMode: DAMode,
    ): Felt {
        val contractAddress = ContractAddressCalculator.calculateAddressFromHash(
            classHash = classHash,
            calldata = constructorCalldata,
            salt = salt,
        )
        return Poseidon.poseidonHash(
            *CommonTransanctionV3Fields(
                txType = TransactionType.DEPLOY_ACCOUNT,
                version = version,
                address = senderAddress, // the information is not clear whether this should be senderAddress or contractAddress
                tip = tip,
                resourceBounds = resourceBounds,
                paymasterData = paymasterData,
                chainId = chainId,
                nonce = nonce,
                nonceDataAvailabilityMode = nonceDataAvailabilityMode,
                feeDataAvailabilityMode = feeDataAvailabilityMode,
            ).toTypedArray(),
            Poseidon.poseidonHash(constructorCalldata),
            classHash,
            contractAddressSalt,
        )
    }

    @JvmStatic
    fun calculateDeclareV1TxHash(
        classHash: Felt,
        chainId: StarknetChainId,
        senderAddress: Felt,
        maxFee: Felt,
        version: Felt,
        nonce: Felt,
    ): Felt {
        val hash = StarknetCurve.pedersenOnElements(listOf(classHash))
        return StarknetCurve.pedersenOnElements(
            TransactionType.DECLARE.txPrefix,
            version,
            senderAddress,
            Felt.ZERO,
            hash,
            maxFee,
            chainId.value,
            nonce,
        )
    }

    @JvmStatic
    fun calculateDeclareV2TxHash(
        classHash: Felt,
        chainId: StarknetChainId,
        senderAddress: Felt,
        maxFee: Felt,
        version: Felt,
        nonce: Felt,
        compiledClassHash: Felt,
    ): Felt {
        val calldataHash = StarknetCurve.pedersenOnElements(listOf(classHash))
        return StarknetCurve.pedersenOnElements(
            TransactionType.DECLARE.txPrefix,
            version,
            senderAddress,
            Felt.ZERO,
            calldataHash,
            maxFee,
            chainId.value,
            nonce,
            compiledClassHash,
        )
    }

    @JvmStatic
    fun calculateDeclareV3TxHash(
        classHash: Felt,
        chainId: StarknetChainId,
        senderAddress: Felt,
        version: Felt,
        nonce: Felt,
        compiledClassHash: Felt,
        tip: Uint64,
        resourceBounds: ResourceBoundsMapping,
        paymasterData: PaymasterData,
        accountDeploymentData: AccountDeploymentData,
        feeDataAvailabilityMode: DAMode,
        nonceDataAvailabilityMode: DAMode,
    ): Felt {
        return Poseidon.poseidonHash(
            *CommonTransanctionV3Fields(
                txType = TransactionType.DECLARE,
                version = version,
                address = senderAddress,
                tip = tip,
                resourceBounds = resourceBounds,
                paymasterData = paymasterData,
                chainId = chainId,
                nonce = nonce,
                nonceDataAvailabilityMode = nonceDataAvailabilityMode,
                feeDataAvailabilityMode = feeDataAvailabilityMode,
            ).toTypedArray(),
            Poseidon.poseidonHash(accountDeploymentData),
            classHash,
            compiledClassHash,
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
    ): Felt {
        return StarknetCurve.pedersenOnElements(
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

    private fun CommonTransanctionV3Fields(
        txType: TransactionType,
        version: Felt,
        address: Felt,
        tip: Uint64,
        resourceBounds: ResourceBoundsMapping,
        paymasterData: PaymasterData,
        chainId: StarknetChainId,
        nonce: Felt,
        nonceDataAvailabilityMode: DAMode,
        feeDataAvailabilityMode: DAMode,
    ): List<Felt> {
        return listOf(
            txType.txPrefix,
            version,
            address,
            Poseidon.poseidonHash(
                tip.toFelt,
                *resourceBoundsForFee(resourceBounds).toList().toTypedArray(),
            ),
            Poseidon.poseidonHash(paymasterData),
            chainId.value.toFelt,
            nonce,
            dataAvailabilityModes(
                feeDataAvailabilityMode,
                nonceDataAvailabilityMode,
            ),
        )
    }

    private fun resourceBoundsForFee(resourceBounds: ResourceBoundsMapping): Pair<Felt, Felt> {
        // TODO: instead, store hardcoded hexes encoded from short string
        val l1GasBound = Felt.fromShortString("L1_GAS").value.shiftLeft(64 + 128)
            .add(resourceBounds.l1Gas.maxAmount.value.shiftLeft(128))
            .add(resourceBounds.l1Gas.maxPricePerUnit.value)
            .toFelt
        val l2GasBound = Felt.fromShortString("L2_GAS").value.shiftLeft(64 + 128)
            .add(resourceBounds.l2Gas.maxAmount.value.shiftLeft(128))
            .add(resourceBounds.l2Gas.maxPricePerUnit.value)
            .toFelt

        return l1GasBound to l2GasBound
    }

    private fun dataAvailabilityModes(
        feeDataAvailabilityMode: DAMode,
        nonceDataAvailabilityMode: DAMode,
    ): Felt {
        return nonceDataAvailabilityMode.value.toBigInteger().shiftLeft(32)
            .add(feeDataAvailabilityMode.value.toBigInteger())
            .toFelt
    }
}
