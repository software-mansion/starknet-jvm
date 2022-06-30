package starknet.crypto

import types.Felt
import java.math.BigInteger


fun feltToNative(input: Felt): ByteArray {
    val converted = input.value.toByteArray().apply { reverse() }
    if (converted.size == 32) {
        return converted;
    }
    return converted.copyOf(newSize = 32)
}

object CryptoCpp {
    init {
        System.loadLibrary("crypto_jni")
    }

    @JvmStatic
    external fun pedersen(
        first: ByteArray?,
        second: ByteArray?
    ): ByteArray

    @JvmStatic
    fun pedersen(first: Felt, second: Felt): Felt {
        val hash = pedersen(feltToNative(first), feltToNative(second)).apply { reverse() }
        return Felt(BigInteger(hash))
    }

    @JvmStatic
    fun pedersen(values: Iterable<Felt>): Felt = values.fold(Felt.ZERO) { a, b -> pedersen(a, b) }

    @JvmStatic
    fun pedersenOnElements(values: Collection<Felt>): Felt = pedersen(
        pedersen(values),
        Felt(values.size),
    )

    @JvmStatic
    fun pedersenOnElements(vararg values: Felt): Felt = pedersenOnElements(values.asList())


    // TODO
    //    public static native byte[] getPublicKey(byte[] privateKey);
    //
    //    public static native byte[] verify(byte[] privateKey, byte[] hash, byte[] r, byte[] s);
    //
    //    public static native byte[] sign(byte[] privateKey, byte[] hash, byte[] k);
}