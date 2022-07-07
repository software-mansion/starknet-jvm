package starknet.signer

import starknet.crypto.StarknetCurve
import starknet.data.types.Signature
import starknet.data.types.Transaction
import types.Felt

class StarkCurveSigner(val privateKey: Felt) : Signer {

    // Generating public key takes a while
    override val publicKey: Felt by lazy { StarknetCurve.getPublicKey(privateKey) }

    override fun signTransaction(transaction: Transaction): Signature {
        val hash = transaction.getHash()
        return StarknetCurve.sign(privateKey, hash).toList()
    }
}