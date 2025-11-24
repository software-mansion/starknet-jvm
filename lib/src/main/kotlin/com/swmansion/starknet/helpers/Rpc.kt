package com.swmansion.starknet.helpers

import com.github.zafarkhaja.semver.Version
import com.swmansion.starknet.crypto.HashMethod

/**
 * Get hash method from provided Starknet version.
 *
 * @param starknetVersion Starknet version
 * @return Hash method
 */
fun getHashMethodFromStarknetVersion(starknetVersion: String): HashMethod {
    // Compare only (major, minor, patch) tuple to ignore pre-release/dev versions.Add a comment on  lines R12 to R13Add diff commentMarkdown input:  edit mode selected.WritePreviewAdd a suggestionHeadingBoldItalicQuoteCodeLinkUnordered listNumbered listTask listMentionReferenceSaved repliesAdd FilesPaste, drop, or click to add filesCancelCommentStart a reviewReturn to code
    val version = Version.parse(starknetVersion)
    val versionTriple =
        Triple(version.majorVersion().toInt(), version.minorVersion().toInt(), version.patchVersion().toInt())
    return if (versionTriple >= Triple(0, 14, 1)) {
        HashMethod.BLAKE2S
    } else {
        HashMethod.POSEIDON
    }
}

operator fun <A : Comparable<A>, B : Comparable<B>, C : Comparable<C>>
        Triple<A, B, C>.compareTo(other: Triple<A, B, C>): Int =
    compareValuesBy(this, other, Triple<A, B, C>::first, Triple<A, B, C>::second, Triple<A, B, C>::third)
