package com.swmansion.starknet.data

import com.swmansion.starknet.crypto.StarknetCurve
import com.swmansion.starknet.data.types.Calldata
import com.swmansion.starknet.data.types.Felt

object ContractAddress {
    @JvmStatic
    val CONTRACT_ADDRESS_PREFIX =
        Felt.fromHex("0x535441524b4e45545f434f4e54524143545f41444452455353") // from_bytes(b"STARKNET_CONTRACT_ADDRESS")

    @JvmStatic
    fun calculateAddressFromHash(
        salt: Felt,
        classHash: Felt,
        calldata: Calldata,
        deployerAddress: Felt,
    ): Felt = StarknetCurve.pedersenOnElements(
        CONTRACT_ADDRESS_PREFIX,
        deployerAddress,
        salt,
        classHash,
        StarknetCurve.pedersenOnElements(calldata),
    )

    @JvmStatic
    fun calculateAddressFromHash(
        salt: Felt,
        classHash: Felt,
        calldata: Calldata,
    ): Felt = calculateAddressFromHash(
        salt = salt,
        classHash = classHash,
        calldata = calldata,
        deployerAddress = Felt.ZERO,
    )
}
