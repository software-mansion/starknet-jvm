package com.swmansion.starknet.signer

import com.swmansion.starknet.crypto.StarknetCurve
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.Signature
import com.swmansion.starknet.data.types.Transaction

/**
 * Signer implementing a stark curve signature (default signature used on StarkNet).
 *
 * @param privateKey a private key to be used by this signer
 */
class StarkCurveSigner(val privateKey: Felt) : Signer {

    // Generating public key takes a while
    override val publicKey: Felt by lazy { StarknetCurve.getPublicKey(privateKey) }

    override fun signTransaction(transaction: Transaction): Signature {
        val hash = transaction.getHash()
        return StarknetCurve.sign(privateKey, hash).toList()
    }
}
