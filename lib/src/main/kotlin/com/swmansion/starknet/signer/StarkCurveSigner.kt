package com.swmansion.starknet.signer

import com.swmansion.starknet.crypto.StarknetCurve
import com.swmansion.starknet.data.responses.Transaction
import com.swmansion.starknet.data.types.*

/**
 * Signer implementing a stark curve signature (default signature used on StarkNet).
 *
 * @param privateKey a private key to be used by this signer
 */
class StarkCurveSigner(val privateKey: Felt) : Signer {

    // Generating public key takes a while
    override val publicKey: Felt by lazy { StarknetCurve.getPublicKey(privateKey) }

    override fun signTransaction(transaction: Transaction): Signature {
        return StarknetCurve.sign(privateKey, transaction.hash).toList()
    }
}
