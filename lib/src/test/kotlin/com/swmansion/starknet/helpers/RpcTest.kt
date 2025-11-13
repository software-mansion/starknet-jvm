package com.swmansion.starknet.helpers

import com.swmansion.starknet.crypto.HashMethod
import io.github.z4kn4fein.semver.Version
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class RpcTest {
    companion object {
        @JvmStatic
        fun getTestCases(): List<Pair<Version, HashMethod>> {
            return listOf(
                Pair(Version(0, 9, 0), HashMethod.POSEIDON),
                Pair(Version(0, 10, 0), HashMethod.BLAKE2S),
                Pair(Version(0, 11, 5), HashMethod.BLAKE2S),
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
