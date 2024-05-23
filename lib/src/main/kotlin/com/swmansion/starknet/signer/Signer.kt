package com.swmansion.starknet.signer

import com.swmansion.starknet.data.TypedData
import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.data.types.transactions.*

/**
 * Transaction signer.
 *
 * Implementers of this interface provide methods for signing transactions to be sent to the Starknet.
 */
interface Signer {
    /**
     * Sign a transaction
     *
     * @param transaction a transaction to be signed
     * @return a signature of provided transaction
     */
    fun signTransaction(transaction: Transaction): Signature

    /**
     * Sign TypedData.
     *
     * @param typedData TypedData instance to sign
     * @param accountAddress Account address used in the TypedData hash calculation
     * @return a signature of provided typedData
     */
    fun signTypedData(typedData: TypedData, accountAddress: Felt): Signature

    /**
     * Public key used by a signer.
     */
    val publicKey: Felt
}
