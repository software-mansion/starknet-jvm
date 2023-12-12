package com.swmansion.starknet.signer

import com.swmansion.starknet.crypto.StarknetCurve
import com.swmansion.starknet.data.TypedData
import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.data.types.transactions.*

/**
 * Signer implementing a stark curve signature (default signature used on Starknet).
 *
 * @param privateKey a private key to be used by this signer
 */
class StarkCurveSigner(private val privateKey: Felt) : Signer {

    // Generating public key takes a while
    override val publicKey: Felt by lazy { StarknetCurve.getPublicKey(privateKey) }

    override fun signTransaction(transaction: Transaction): Signature {
        // TODO: if hash is missing, generate it on demand
        requireNotNull(transaction.hash) { "Invalid transaction: hash is missing." }

        return StarknetCurve.sign(privateKey, transaction.hash!!).toList()
    }

    override fun signTypedData(typedData: TypedData, accountAddress: Felt): Signature {
        val messageHash = typedData.getMessageHash(accountAddress)
        return StarknetCurve.sign(privateKey, messageHash).toList()
    }
}
