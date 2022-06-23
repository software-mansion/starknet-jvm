package starknet.signer

import starknet.crypto.sign
import starknet.data.types.Signature
import starknet.data.types.Transaction
import java.security.KeyPair
import java.security.PublicKey

class StarkCurveSigner(val keyPair: KeyPair) : Signer {
    override fun signTransaction(transaction: Transaction): Signature {
        val hash = transaction.getHash()
        return sign(keyPair, hash.value.toByteArray()).toList()
    }

    override fun getPublicKey(): PublicKey = keyPair.public
}