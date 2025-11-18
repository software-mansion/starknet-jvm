package com.swmansion.starknet.helpers

import com.github.zafarkhaja.semver.Version
import com.swmansion.starknet.crypto.HashMethod

internal fun hashMethodFromRpcVersion(version: Version): HashMethod {
    // Compare only (major, minor, patch) tuple to ignore pre-release/dev versions.
    val versionTriple = Triple(version.majorVersion().toInt(), version.minorVersion().toInt(), version.patchVersion().toInt())
    return if (versionTriple >= Triple(0, 10, 0)) {
        HashMethod.BLAKE2S
    } else {
        HashMethod.POSEIDON
    }
}

operator fun <A : Comparable<A>, B : Comparable<B>, C : Comparable<C>>
    Triple<A, B, C>.compareTo(other: Triple<A, B, C>): Int =
    compareValuesBy(this, other, Triple<A, B, C>::first, Triple<A, B, C>::second, Triple<A, B, C>::third)
