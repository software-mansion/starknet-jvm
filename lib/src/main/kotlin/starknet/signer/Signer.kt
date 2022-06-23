package starknet.signer

import starknet.data.types.Signature
import starknet.data.types.Transaction
import java.security.PublicKey

interface Signer {
    // TODO: sign message
    // TODO: add more params
    fun signTransaction(transaction: Transaction): Signature

    fun getPublicKey(): PublicKey
}