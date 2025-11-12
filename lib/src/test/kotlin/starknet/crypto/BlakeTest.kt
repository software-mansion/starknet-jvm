package starknet.crypto

import com.swmansion.starknet.crypto.Blake
import com.swmansion.starknet.data.types.Felt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigInteger

class BlakeTest {
    companion object {
        @JvmStatic
        private fun getTestCases(): List<Pair<List<Felt>, Felt>> {
            return listOf(
                // Empty array
                Pair(
                    emptyList(),
                    Felt.fromHex("0x1EED01EFD0D230C1EA5A12C48B6551F7C4A3542D02111E194809079307A214A"),
                ),
                // Boundary: small felt at (2^63 - 1)
                Pair(
                    listOf(Felt(BigInteger.ONE.shiftLeft(63).subtract(BigInteger.ONE))),
                    Felt.fromHex("0x354AEF67E2B1A01D5AFE9A85707D79349C8BE8A4B261629360B2129810D8DD"),
                ),
                // Boundary: at 2^63
                Pair(
                    listOf(Felt(BigInteger.ONE.shiftLeft(63))),
                    Felt.fromHex("0xB44AEEA3E1288E4614B3DE3A7A6BEE8D592EF9CD30C70304E7E84B52702EF2"),
                ),
                // Very large felt
                Pair(
                    listOf(Felt(BigInteger("800000000000011000000000000000000000000000000000000000000000000", 16))),
                    Felt.fromHex("0x7C018937C4B4968CC90A67326B1715CA808B7546AD91FB91FBC42A3DC6C52F1"),
                ),
                // Mixed: small and large felts
                Pair(
                    listOf(
                        Felt(42),
                        Felt(BigInteger.ONE.shiftLeft(63)),
                        Felt(1337),
                    ),
                    Felt.fromHex("0x27E2140360D26BF4A53778D8BC2F9B103DDFA83ABF3451308190B9C340D11A5"),
                ),
            )
        }
    }

    @ParameterizedTest
    @MethodSource("getTestCases")
    fun `test blake2sHash`(case: Pair<List<Felt>, Felt>) {
        val (input, expected) = case
        val result = Blake.blake2sHash(input)
        assertEquals(
            expected,
            result,
            "Starknet Kotlin implementation differs from Cairo reference",
        )
    }
}
