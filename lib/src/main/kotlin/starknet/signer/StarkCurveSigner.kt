package starknet.signer

import starknet.crypto.getKeyPair
import starknet.crypto.sign
import starknet.data.types.Signature
import starknet.data.types.Transaction
import java.math.BigInteger
import java.security.KeyPair
import java.security.PublicKey

class StarkCurveSigner(val keyPair: KeyPair) : Signer {

    constructor(privateKey: BigInteger) : this(getKeyPair(privateKey))

    override fun signTransaction(transaction: Transaction): Signature {
        val hash = transaction.getHash()
        return sign(keyPair, hash.value.toByteArray()).toList()
    }

    override fun getPublicKey(): PublicKey = keyPair.public
}