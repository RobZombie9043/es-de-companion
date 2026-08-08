package com.esde.companion.domain.update

/**
 * A loosely-parsed app version, e.g. "0.7.0-RC1" or the git-tag form "v0.7.0-alpha".
 *
 * This project's release history isn't strict semver - tags include irregular suffixes
 * like "v0.6.0-pre-release" and "v0.7.0-alpha" - so parsing is deliberately forgiving
 * rather than throwing on anything unexpected. A version string that can't be parsed at
 * all degrades to 0.0.0 with the whole string as [suffix], which [VersionComparator]
 * then treats as "oldest possible" rather than crashing the update check.
 */
data class AppVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val suffix: String?,
    val raw: String,
) {
    companion object {
        private val NUMERIC_PREFIX =
            Regex("""^(?<major>\d+)(?:\.(?<minor>\d+))?(?:\.(?<patch>\d+))?""")

        fun parse(raw: String): AppVersion {
            val trimmed = raw.trim().removePrefix("v").removePrefix("V")
            val match = NUMERIC_PREFIX.find(trimmed)

            if (match == null) {
                return AppVersion(major = 0, minor = 0, patch = 0, suffix = trimmed.ifEmpty { null }, raw = raw)
            }

            val major = match.groups["major"]?.value?.toInt() ?: 0
            val minor = match.groups["minor"]?.value?.toIntOrNull() ?: 0
            val patch = match.groups["patch"]?.value?.toIntOrNull() ?: 0
            val rest = trimmed.substring(match.range.last + 1).trimStart('-', '+', '.')

            return AppVersion(major = major, minor = minor, patch = patch, suffix = rest.ifEmpty { null }, raw = raw)
        }
    }
}
