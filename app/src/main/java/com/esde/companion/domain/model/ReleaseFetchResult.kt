package com.esde.companion.domain.model

/**
 * Outcome of fetching a single release from GitHub. A network/API failure here is a
 * routine, expected outcome (no connectivity, GitHub is down, the tag doesn't exist) -
 * represented as a value rather than a thrown exception, per CLAUDE.md.
 */
sealed class ReleaseFetchResult {
    data class Success(val release: ReleaseInfo) : ReleaseFetchResult()

    /** No release found - either there's no "latest" release yet, or the requested tag doesn't exist. */
    data object NotFound : ReleaseFetchResult()

    data class NetworkError(val message: String) : ReleaseFetchResult()
}
