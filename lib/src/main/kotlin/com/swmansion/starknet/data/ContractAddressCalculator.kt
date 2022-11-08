package com.swmansion.starknet.data

import com.swmansion.starknet.crypto.StarknetCurve
import com.swmansion.starknet.data.types.Calldata
import com.swmansion.starknet.data.types.Felt

/**
 * Toolkit offering address related functionalities.
 */
object ContractAddressCalculator {
    /**
     * Prefix used for calculating addresses in StarkNet. It is a hex encoding of string "STARKNET_CONTRACT_ADDRESS".
     */
    private val CONTRACT_ADDRESS_PREFIX = Felt.fromHex("0x535441524b4e45545f434f4e54524143545f41444452455353")

    /**
     * Calculate address of a contract on StarkNet.
     *
     * @param classHash hash of the contract code
     * @param calldata constructor calldata used for deployment
     * @param salt salt used to calculate address
     * @param deployerAddress address that deployed contract
     */
    @JvmStatic
    fun calculateAddressFromHash(
        classHash: Felt,
        calldata: Calldata,
        salt: Felt,
        deployerAddress: Felt,
    ): Felt = StarknetCurve.pedersenOnElements(
        CONTRACT_ADDRESS_PREFIX,
        deployerAddress,
        salt,
        classHash,
        StarknetCurve.pedersenOnElements(calldata),
    )

    /**
     * Calculate address of a contract on StarkNet. Doesn't require deployerAddress.
     *
     * @param classHash hash of the contract code
     * @param calldata constructor calldata used for deployment
     * @param salt salt used to calculate address
     */
    @JvmStatic
    fun calculateAddressFromHash(
        classHash: Felt,
        calldata: Calldata,
        salt: Felt,
    ): Felt = calculateAddressFromHash(
        classHash = classHash,
        calldata = calldata,
        salt = salt,
        deployerAddress = Felt.ZERO,
    )
}
