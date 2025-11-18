package com.swmansion.starknet.helpers

import com.github.zafarkhaja.semver.Version
import com.swmansion.starknet.crypto.HashMethod
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class RpcTest {
    companion object {
        @JvmStatic
        private fun getTestCases(): List<Pair<Version, HashMethod>> {
            return listOf(
                Pair(Version.tryParse("0.9.0").orElseThrow(), HashMethod.POSEIDON),
                Pair(Version.tryParse("0.9.1").orElseThrow(), HashMethod.POSEIDON),
                Pair(Version.tryParse("0.10.0").orElseThrow(), HashMethod.BLAKE2S),
                Pair(Version.tryParse("0.10.0-rc.1").orElseThrow(), HashMethod.BLAKE2S),
                Pair(Version.tryParse("0.11.5").orElseThrow(), HashMethod.BLAKE2S),
                Pair(Version.tryParse("1.0.0").orElseThrow(), HashMethod.BLAKE2S),
            )
        }
    }

    @ParameterizedTest
    @MethodSource("getTestCases")
    fun `test hash method from RPC version`(testCase: Pair<Version, HashMethod>) {
        val (version, expectedHashMethod) = testCase
        val actualHashMethod = hashMethodFromRpcVersion(version)
        assert(actualHashMethod == expectedHashMethod) {
            "For version $version, expected hash method $expectedHashMethod but got $actualHashMethod"
        }
    }
}
