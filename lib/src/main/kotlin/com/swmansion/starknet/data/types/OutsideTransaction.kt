package com.swmansion.starknet.data.types

import com.swmansion.starknet.crypto.StarknetCurveSignature
import com.swmansion.starknet.data.types.conversions.ConvertibleToCalldata

data class OutsideTransaction(
    val outsideExecution: OutsideExecution,
    val signature: StarknetCurveSignature,
    val signerAddress: Felt,
) : ConvertibleToCalldata {

    override fun toCalldata(): List<Felt> {
        return listOf(
            outsideExecution.toCalldata(),
            listOf(Felt(signature.toCalldata().size)),
            signature.toCalldata(),
        ).flatten()
    }
}
