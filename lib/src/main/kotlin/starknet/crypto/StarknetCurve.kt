package starknet.crypto

import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.signers.HMacDSAKCalculator
import types.Felt
import types.toFelt
import java.math.BigInteger

private fun bigintToNative(input: BigInteger): ByteArray {
    val converted = input.toByteArray().apply { reverse() }
    if (converted.size == 32) {
        return converted;
    }
    return converted.copyOf(newSize = 32)
}

private fun feltToNative(input: Felt): ByteArray = bigintToNative(input.value)

object StarknetCurve {

    @field:JvmField
    val CURVE_ORDER: BigInteger = BigInteger("800000000000010FFFFFFFFFFFFFFFFB781126DCAE7B2321E66A241ADC64D2F", 16)

    init {
        System.loadLibrary("crypto_jni")
    }

    @JvmStatic
    private external fun pedersen(
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

    @JvmStatic
    private external fun sign(privateKey: ByteArray, hash: ByteArray, k: ByteArray): ByteArray

    // This function is internal, because invalid generation of k is VERY dangerous.
    @JvmStatic
    internal fun sign(privateKey: Felt, hash: Felt, k: BigInteger): StarknetCurveSignature {
        val signature = sign(
            feltToNative(privateKey),
            feltToNative(hash),
            bigintToNative(k),
        )
        val r = BigInteger(signature.slice(0 until 32).toByteArray().apply { reverse() })
        val w = BigInteger(signature.slice(32 until 64).toByteArray().apply { reverse() })
        val s = w.modInverse(CURVE_ORDER)
        return StarknetCurveSignature(Felt(r), Felt(s))
    }

    @JvmStatic
    internal fun sign(privateKey: Felt, hash: Felt): StarknetCurveSignature {
        val cal = HMacDSAKCalculator(SHA256Digest()).apply {
            init(CURVE_ORDER, privateKey.value, hash.value.toByteArray())
        }

        // Generated K might not be suitable for signing. Probability of it is very low.
        // https://github.com/starkware-libs/cairo-lang/blob/167b28bcd940fd25ea3816204fa882a0b0a49603/src/starkware/crypto/starkware/crypto/signature/signature.py#L141
        var lastError: Exception? = null;
        for (i in 0 until 3) {
            try {
                return sign(privateKey, hash, cal.nextK())
            } catch (e: IllegalArgumentException) {
                // This shouldn't really happen, all Felt instances should work
                throw e;
            } catch (e: Exception) {
                lastError = e;
            }
        }

        if (lastError == null) {
            // Impossible
            throw AssertionError("No signature or error after signing.")
        }

        throw lastError;
    }

    @JvmStatic
    private external fun verify(publicKey: ByteArray, hash: ByteArray, r: ByteArray, w: ByteArray): Boolean;

    @JvmStatic
    fun verify(publicKey: Felt, hash: Felt, r: Felt, s: Felt): Boolean {
        val w = s.value.modInverse(CURVE_ORDER)
        return verify(
            feltToNative(publicKey),
            feltToNative(hash),
            feltToNative(r),
            bigintToNative(w),
        )
    }

    @JvmStatic
    private external fun getPublicKey(privateKey: ByteArray): ByteArray;

    @JvmStatic
    fun getPublicKey(privateKey: Felt): Felt {
        val publicKey = getPublicKey(feltToNative(privateKey)).apply { reverse() }
        return BigInteger(publicKey).toFelt
    }
}