package com.swmansion.starknet.helpers

import com.swmansion.starknet.crypto.HashMethod
import io.github.z4kn4fein.semver.Version

internal fun hashMethodFromRpcVersion(version: Version): HashMethod {
    return if (version >= Version(0, 10, 0)) {
        HashMethod.BLAKE2S
    } else {
        HashMethod.POSEIDON
    }
}
