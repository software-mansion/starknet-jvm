package com.swmansion.starknet.data

import com.swmansion.starknet.crypto.StarknetCurve
import com.swmansion.starknet.crypto.keccak
import com.swmansion.starknet.data.types.Calldata
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.extensions.toBytes
import java.math.BigInteger
import java.util.*

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

    /**
     * Checks if provided string is in a checksum address format.
     *
     * @param address to check
     */
    @JvmStatic
    fun isChecksumAddressValid(address: String): Boolean {
        return calculateChecksumAddress(Felt.fromHex(address)) == address
    }

    /**
     * Outputs formatted checksum address.
     *
     * Follows implementation of starknet.js. EIP55 was an inspiration, but it is not compatible as it treats hex string
     * as encoded number, instead of encoding it as ASCII string.
     * Example checksum address - 0x02Fd23d9182193775423497fc0c472E156C57C69E4089A1967fb288A2d84e914.
     *
     * @param address to encode
     */
    @JvmStatic
    fun calculateChecksumAddress(address: Felt): String {
        val stringAddress = address.value.toString(16).lowercase(Locale.ENGLISH).padStart(64, '0')
        val chars = stringAddress.toCharArray()
        // This part is different from EIP55. EIP55 digests hex as encoded ASCII string and here we
        // use digest it as bytes.
        val hashed = keccak(BigInteger(stringAddress, 16).toBytes())
        for (i in chars.indices) {
            // We subtract from 256, because we use bits starting from the end
            if (chars[i].isLetter() && hashed.testBit(256 - 4 * i - 1)) {
                chars[i] = chars[i].uppercaseChar()
            }
        }

        return "0x${chars.concatToString()}"
    }
}
