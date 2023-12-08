package com.swmansion.starknet.data

import com.swmansion.starknet.crypto.Poseidon
import com.swmansion.starknet.crypto.StarknetCurve
import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.data.types.transactions.DAMode
import com.swmansion.starknet.data.types.transactions.TransactionType
import com.swmansion.starknet.extensions.toFelt
import java.math.BigInteger

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
        maxFee: Felt,
        tip: Uint64,
        resourceBounds: ResourceBoundsMapping,
        paymasterData: PaymasterData,
        accountDeploymentData: List<Felt>,
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
        accountDeploymentData: List<Felt>,
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
        val l1GasPrefix = Felt.fromShortString("L1_GAS").toFelt.value.toString(2).padEnd(60, '0')
        val l1MaxAmount = resourceBounds.l1Gas.maxAmount.toFelt.value.toString(2).padEnd(64, '0')
        val l1MaxPricePerUnit = resourceBounds.l1Gas.maxPricePerUnit.toFelt.value.toString(2).padEnd(128, '0')

        val l2GasPrefix = Felt.fromShortString("L2_GAS").toFelt.value.toString(2).padEnd(60, '0')
        val l2MaxAmount = resourceBounds.l2Gas.maxAmount.toFelt.value.toString(2).padEnd(64, '0')
        val l2MaxPricePerUnit = resourceBounds.l2Gas.maxPricePerUnit.toFelt.value.toString(2).padEnd(128, '0')

        val l1GasBoundsBinary = l1GasPrefix + l1MaxAmount + l1MaxPricePerUnit
        val l2GasBoundsBinary = l2GasPrefix + l2MaxAmount + l2MaxPricePerUnit

        return Pair(
            BigInteger(l1GasBoundsBinary, 2).toFelt,
            BigInteger(l2GasBoundsBinary, 2).toFelt,
        )
    }

    private fun dataAvailabilityModes(
        feeDataAvailabilityMode: DAMode,
        nonceDataAvailabilityMode: DAMode,
    ): Felt {
        val feeModeBinary = feeDataAvailabilityMode.value.toString(2).padEnd(32, '0')
        val nonceModeBinary = nonceDataAvailabilityMode.value.toString(2).padEnd(32, '0')

        val dataAvailabilityModesBinary = "0".repeat(188) + feeModeBinary + nonceModeBinary

        return BigInteger(dataAvailabilityModesBinary, 2).toFelt
    }
}
