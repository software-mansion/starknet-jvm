package starknet.crypto

import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.signers.HMacDSAKCalculator
import starknet.data.types.Felt
import starknet.data.types.toFelt
import java.math.BigInteger

/**
 * Convert bigInteger to native format.
 *
 * Convert bigInteger to small endian byte array used by native dependencies.
 *
 * @param input a BigInteger to be converted
 * @return small endian byte array
 */
private fun bigintToNative(input: BigInteger): ByteArray {
    val converted = input.toByteArray().apply { reverse() }
    if (converted.size == 32) {
        return converted;
    }
    return converted.copyOf(newSize = 32)
}

/**
 * Convert Felt to native format.
 *
 * Convert Felt to small endian byte array used by native dependencies.
 *
 * @param input a Felt to be converted
 * @return small endian byte array
 */
private fun feltToNative(input: Felt): ByteArray = bigintToNative(input.value)

/**
 * Starknet curve utilities.
 *
 * Class with utility methods related to starknet curve signatures generation and verification.
 */
object StarknetCurve {

    @field:JvmField
    val CURVE_ORDER: BigInteger = BigInteger("800000000000010FFFFFFFFFFFFFFFFB781126DCAE7B2321E66A241ADC64D2F", 16)

    init {
        NativeLoader.load("crypto_jni")
    }

    /**
     * Native pedersen hash.
     */
    @JvmStatic
    private external fun pedersen(
        first: ByteArray?,
        second: ByteArray?
    ): ByteArray

    /**
     * Compute pedersen hash on input values
     */
    @JvmStatic
    fun pedersen(first: Felt, second: Felt): Felt {
        val hash = pedersen(feltToNative(first), feltToNative(second)).apply { reverse() }
        return Felt(BigInteger(hash))
    }

    /**
     * Compute pedersen hash on iterable of Felts.
     *
     * @param values a iterable of Felts
     */
    @JvmStatic
    fun pedersen(values: Iterable<Felt>): Felt = values.fold(Felt.ZERO) { a, b -> pedersen(a, b) }

    /**
     * Compute pedersen hash on collection of Felts.
     *
     * @param values a collection of Felts
     */
    @JvmStatic
    fun pedersenOnElements(values: Collection<Felt>): Felt = pedersen(
        pedersen(values),
        Felt(values.size),
    )

    /**
     * Compute pedersen hash on variable number of arguments.
     *
     * @param values any number of Felts
     */
    @JvmStatic
    fun pedersenOnElements(vararg values: Felt): Felt = pedersenOnElements(values.asList())

    /**
     * Native sign.
     */
    @JvmStatic
    private external fun sign(privateKey: ByteArray, hash: ByteArray, k: ByteArray): ByteArray

    /**
     * Sign a provided hash.
     *
     * @param privateKey a private key used to generate a signature
     * @param hash a hash to be signed
     * @param k cryptographically secure random integer
     */
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

    /**
     * Sign a provided hash.
     *
     * @param privateKey a private key used to generate a signature
     * @param hash a hash to be signed
     */
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

    /**
     * Native signature verification.
     */
    @JvmStatic
    private external fun verify(publicKey: ByteArray, hash: ByteArray, r: ByteArray, w: ByteArray): Boolean;

    /**
     * Verify a signature.
     *
     * @param publicKey a public key to be used to verify a signature
     * @param hash a value that was signed
     * @param r part of signature
     * @param s part of signature
     */
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

    /**
     * Native getPublicKey.
     */
    @JvmStatic
    private external fun getPublicKey(privateKey: ByteArray): ByteArray;

    /**
     * Get a public key for provided private key.
     *
     * @param privateKey a private key from which public key will be derived
     */
    @JvmStatic
    fun getPublicKey(privateKey: Felt): Felt {
        val publicKey = getPublicKey(feltToNative(privateKey)).apply { reverse() }
        return BigInteger(publicKey).toFelt
    }
}