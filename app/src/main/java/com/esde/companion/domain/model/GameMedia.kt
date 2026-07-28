package com.esde.companion.domain.model

/**
 * Resolved on-disk media info for a single game.
 *
 * [baseRelativePath] is the system-relative, extension-resolved path media files are
 * expected under (see GameMediaPathResolver) - null only when it couldn't be computed at
 * all (e.g. the rom path didn't contain the expected system folder segment). Exposed here
 * rather than requiring callers to recompute it, so UI code can build an "expected path"
 * without duplicating resolution logic or needing filesystem access itself - the data
 * layer already had to compute this to do the actual lookup below.
 *
 * [filesByType] maps to files that were actually confirmed to exist on disk. A type
 * missing from this map means no matching file was found for that type - an expected,
 * routine outcome, not an error.
 */
data class GameMedia(
    val baseRelativePath: String?,
    val filesByType: Map<MediaType, String>,
) {
    fun path(type: MediaType): String? = filesByType[type]
}