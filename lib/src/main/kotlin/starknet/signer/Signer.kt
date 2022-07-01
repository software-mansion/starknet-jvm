package starknet.signer

import starknet.data.types.Signature
import starknet.data.types.Transaction
import types.Felt

interface Signer {
    // TODO: sign message
    // TODO: add more params
    fun signTransaction(transaction: Transaction): Signature

    val publicKey: Felt
}