package com.swmansion.starknet.extensions

import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.NumAsHex
import java.math.BigInteger

@get:JvmSynthetic
val BigInteger.toFelt: Felt
    get() = Felt(this)

@get:JvmSynthetic
val BigInteger.toNumAsHex: NumAsHex
    get() = NumAsHex(this)

@JvmSynthetic
internal fun BigInteger.toBytes(): ByteArray {
    require(this.signum() != -1) { "Creating ByteArray from negative numbers is not supported." }
    val bitLength = this.bitLength()
    // Big integer calculates byte length as bitLength()/8 + 1 to add sign.
    // This means that if value could be represented with exactly N bytes the BigInteger
    // will use N+1 bytes. Additional byte is important when doing hashing.
    if (bitLength != 0 && bitLength % 8 == 0) {
        return this.toByteArray().drop(1).toByteArray()
    }
    return this.toByteArray()
}
