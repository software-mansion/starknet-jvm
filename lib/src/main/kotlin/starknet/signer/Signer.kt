package starknet.signer

import starknet.data.types.Signature
import starknet.data.types.Transaction
import starknet.data.types.Felt

/**
 * Transaction signer.
 *
 * Implementers of this interface provide methods for signing transactions to be sent to the StarkNet.
 */
interface Signer {
    /**
     * Sign a transaction
     *
     * @param transaction a transaction to be signed
     * @return a signature of provided transaction
     */
    // TODO: sign message
    // TODO: add more params
    fun signTransaction(transaction: Transaction): Signature

    /**
     * Public key used by a signer.
     */
    val publicKey: Felt
}