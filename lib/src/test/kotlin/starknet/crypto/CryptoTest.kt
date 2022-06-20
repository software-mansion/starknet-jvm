package starknet.crypto

import types.Felt
import types.toFelt
import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class CryptoTest {
    @Test
    fun getKeyPair() {
        val pk = BigInteger("019800ea6a9a73f94aee6a3d2edf018fc770443e90c7ba121e8303ec6b349279", 16)

        val keyPair = getKeyPair(pk);

        assertEquals(
            "0x33f45f07e1bd1a51b45fc24ec8c8c9908db9e42191be9e169bfcac0c0d99745",
            getStarkKey(keyPair),
        )
    }

    @Test
    fun sign() {
        val pk = BigInteger("019800ea6a9a73f94aee6a3d2edf018fc770443e90c7ba121e8303ec6b349279", 16)
        val keyPair = getKeyPair(pk);
        val msg = "test".toByteArray()
        val signature = sign(keyPair, msg)
        val valid = verify(keyPair, msg, signature)

        assertTrue(valid)
    }

    @Test
    fun pedersen() {
        val maxFelt = (Felt.PRIME - BigInteger.ONE).toFelt;
        // Generated using cairo-lang package
        val cases = arrayOf(
            Triple(Felt(1), Felt(2), "0x5bb9440e27889a364bcb678b1f679ecd1347acdedcbf36e83494f857cc58026"),
            Triple(Felt(0), Felt(0), "0x49ee3eba8c1600700ee1b87eb599f16716b0b1022947733551fde4050ca6804"),
            Triple(Felt(1), Felt(0), "0x268a9d47dde48af4b6e2c33932ed1c13adec25555abaa837c376af4ea2f8a94"),
            Triple(Felt(0), Felt(1), "0x46c9aeb066cc2f41c7124af30514f9e607137fbac950524f5fdace5788f9d43"),
            Triple(maxFelt, maxFelt, "0x7258fccaf3371fad51b117471d9d888a1786c5694c3e6099160477b593a576e"),
            Triple(
                Felt.fromHex("0x7abcde123245643903241432abcde"),
                Felt.fromHex("0x791234124214214728147241242142a89b812221c21d"),
                "0x440a3075f082daa47147a22a4cd0c934ef65ea13ef87bf13adf45613e12f6ee"
            ),
            Triple(
                Felt.fromHex("0x46c9aeb066cc2f41c7124af30514f9e607137fbac950524f5fdace5788f9d43"),
                Felt.fromHex("0x49ee3eba8c1600700ee1b87eb599f16716b0b1022947733551fde4050ca6804"),
                "0x68ad69169c41c758ebd02e2fce51716497a708232a45a1b83e82fac1ade326e"
            ),
        )
        for (case in cases) {
            val result = pedersen(case.first, case.second)
            assertEquals(Felt.fromHex(case.third), result)
        }
    }
}