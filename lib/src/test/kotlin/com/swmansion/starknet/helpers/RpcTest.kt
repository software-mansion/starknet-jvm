package com.swmansion.starknet.helpers

import com.swmansion.starknet.crypto.HashMethod
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class RpcTest {
    companion object {
        @JvmStatic
        private fun getTestCases(): List<Pair<String, HashMethod>> {
            return listOf(
                Pair("0.9.0", HashMethod.POSEIDON),
                Pair("0.9.1", HashMethod.POSEIDON),
                Pair("0.10.0", HashMethod.POSEIDON),
                Pair("0.14.0", HashMethod.POSEIDON),
                Pair("0.14.1-rc.0", HashMethod.BLAKE2S),
                Pair("0.14.1", HashMethod.BLAKE2S),
                Pair("1.0.0", HashMethod.BLAKE2S),
            )
        }
    }

    @ParameterizedTest
    @MethodSource("getTestCases")
    fun `test hash method from Starknet version`(testCase: Pair<String, HashMethod>) {
        val (version, expectedHashMethod) = testCase
        val actualHashMethod = getHashMethodFromStarknetVersion(version)
        assert(actualHashMethod == expectedHashMethod) {
            "For version $version, expected hash method $expectedHashMethod but got $actualHashMethod"
        }
    }
}
