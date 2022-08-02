package starknet.crypto

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import starknet.data.types.Felt
import starknet.data.types.toFelt
import java.math.BigInteger

val PRIVATE_KEY = Felt.fromHex("0x4070e7abfa479cf8a30d38895e93800a88862c4a65aa00e2b11495998818046")
val PUBLIC_KEY = Felt.fromHex("0x7697f8f9a4c3e2b1efd882294462fda2ca9c439d02a3a04cf0a0cdb627f11ee")

internal class StarknetCurveTest {
    @Test
    fun `signing with K`() {
        val hash = Felt.fromHex("0x052fc40e34aee86948cd47e1a0096fa67df8410f81421f314a1eb18102251a82")
        val signature = StarknetCurve.sign(
            privateKey = PRIVATE_KEY,
            hash = hash,
            k = Felt.fromHex("0x6d45bce40ffc4a8cd4cb656048d023a90913e70e589362b41e4334c721cec4b").value,
        )

        // Verified it is a correct signature using cairo-lang package
        val r = Felt.fromHex("0x76a835cfbccd598b9429f6fce09acace91001abcfa68c36022e42dbdb024385")
        val s = Felt.fromHex("0x198ef0ca145ad0fbd175426788d9a7c84de3764f51bfc0fe0579caca660bfe4")
        assertEquals(
            StarknetCurveSignature(r, s),
            signature,
        )

        assertTrue(
            StarknetCurve.verify(
                publicKey = PUBLIC_KEY,
                hash = hash,
                r = signature.r,
                s = signature.s,
            ),
        )
    }

    @Test
    fun `signing without K`() {
        val hash = Felt.fromHex("0x052fc40e34aee86948cd47e1a0096fa67df8410f81421f314a1eb18102251a82")
        val signature = StarknetCurve.sign(
            privateKey = PRIVATE_KEY,
            hash = hash,
        )

        // Verified it is a correct signature using cairo-lang package
        val r = Felt.fromHex("0x674a535c0b84fbabd8df411908842bb56d40e9c21197e95aafe9433e7807b8c")
        val s = Felt.fromHex("0x5eed1e83d0df6a22f1cd168331ae85a4c3b74022f3065531488ed0aaa5b0b3")
        assertEquals(
            StarknetCurveSignature(r, s),
            signature,
        )

        assertTrue(
            StarknetCurve.verify(
                publicKey = PUBLIC_KEY,
                hash = hash,
                r = signature.r,
                s = signature.s,
            ),
        )
    }

    @Test
    fun verify() {
        val r = Felt.fromHex("0x66f8955f5c4cbad5c21905ca2a968bc32a183e81069b851b7fc388eceaf57f1")
        val s = Felt.fromHex("0x13d5af50c934213f27a8cc5863aa304165aa886487fcc575fe6e1228879f9fe")
        val positiveResult = StarknetCurve.verify(
            publicKey = PUBLIC_KEY,
            hash = Felt.fromHex("0x1"),
            r = r,
            s = s,
        )

        assertTrue(positiveResult)

        val negativeResult = StarknetCurve.verify(
            publicKey = PUBLIC_KEY,
            hash = Felt.fromHex("0x1"),
            r = s,
            s = r,
        )
        assertFalse(negativeResult)
    }

    @Test
    fun getPublicKey() {
        val publicKey = StarknetCurve.getPublicKey(PRIVATE_KEY)

        assertEquals(PUBLIC_KEY, publicKey)
    }

    @Test
    fun pedersen() {
        val maxFelt = (Felt.PRIME - BigInteger.ONE).toFelt
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
                "0x440a3075f082daa47147a22a4cd0c934ef65ea13ef87bf13adf45613e12f6ee",
            ),
            Triple(
                Felt.fromHex("0x46c9aeb066cc2f41c7124af30514f9e607137fbac950524f5fdace5788f9d43"),
                Felt.fromHex("0x49ee3eba8c1600700ee1b87eb599f16716b0b1022947733551fde4050ca6804"),
                "0x68ad69169c41c758ebd02e2fce51716497a708232a45a1b83e82fac1ade326e",
            ),
            Triple(
                Felt.fromHex("0x15d40a3d6ca2ac30f4031e42be28da9b056fef9bb7357ac5e85627ee876e5ad"),
                Felt.fromHex("0x43e637ca70a5daac877cba6b57e0b9ceffc5b37d28509e46b4fd2dee968a70c"),
                "0x4b9281c85cfc5ab1f4046663135329020f57c1a88a50f4423eff37dd5fe81e8",
            ),
            Triple(
                Felt.fromHex("0x0"),
                Felt.fromHex("0x15d40a3d6ca2ac30f4031e42be28da9b056fef9bb7357ac5e85627ee876e5ad"),
                "0x1a0c3e0f68c3ee702017fdb6452339244840eedbb70ab3d4f45e2affd1c9420",
            ),
        )
        for (case in cases) {
            val result = StarknetCurve.pedersen(case.first, case.second)
            assertEquals(Felt.fromHex(case.third), result)
        }
    }

    @Test
    fun pedersenOnElements() {
        // Generated using cairo-lang package
        val cases = arrayOf(
            Pair(listOf(), Felt.fromHex("0x49ee3eba8c1600700ee1b87eb599f16716b0b1022947733551fde4050ca6804")),
            Pair(
                listOf(Felt(123782376), Felt(213984), Felt(128763521321)),
                Felt.fromHex("0x7b422405da6571242dfc245a43de3b0fe695e7021c148b918cd9cdb462cac59"),
            ),
            Pair(
                listOf(
                    Felt.fromHex("0x15d40a3d6ca2ac30f4031e42be28da9b056fef9bb7357ac5e85627ee876e5ad"),
                    Felt.fromHex("0x10927538dee311ae5093324fc180ab87f23bbd7bc05456a12a1a506f220db25"),
                ),
                Felt.fromHex("0x43e637ca70a5daac877cba6b57e0b9ceffc5b37d28509e46b4fd2dee968a70c"),
            ),
        )
        for ((input, expected) in cases) {
            val resultWithCollection = StarknetCurve.pedersenOnElements(input)
            val resultWithVarargs = StarknetCurve.pedersenOnElements(*input.toTypedArray())
            assertEquals(expected, resultWithCollection)
            assertEquals(expected, resultWithVarargs)
        }
    }
}
