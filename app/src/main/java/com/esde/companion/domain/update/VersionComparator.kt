package com.esde.companion.domain.update

/**
 * Compares two app version strings. This is a best-effort heuristic, not strict semver
 * prerelease ordering - see [AppVersion]'s kdoc for why. The numeric `major.minor.patch`
 * tuple always decides first; only at an exact tie does the suffix come into play, where
 * a build with no suffix is considered newer than a suffixed one (matches the usual
 * "1.0.0 is newer than 1.0.0-rc1" convention), and two different suffixes fall back to a
 * plain lowercased string compare, which is not guaranteed to reflect actual release
 * order (e.g. "alpha" vs "beta" happens to sort correctly, but this isn't relied upon).
 */
object VersionComparator {
    fun compare(
        a: String,
        b: String,
    ): Int {
        val versionA = AppVersion.parse(a)
        val versionB = AppVersion.parse(b)

        val numeric = compareValuesBy(versionA, versionB, { it.major }, { it.minor }, { it.patch })
        if (numeric != 0) return numeric

        val suffixA = versionA.suffix
        val suffixB = versionB.suffix
        return when {
            suffixA == null && suffixB == null -> 0
            suffixA == null -> 1
            suffixB == null -> -1
            else -> suffixA.lowercase().compareTo(suffixB.lowercase())
        }
    }

    fun isNewer(
        candidate: String,
        current: String,
    ): Boolean = compare(candidate, current) > 0
}
