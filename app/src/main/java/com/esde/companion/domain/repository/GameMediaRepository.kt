package com.esde.companion.domain.repository

import com.esde.companion.domain.model.GameMedia
import com.esde.companion.domain.model.MediaType

/**
 * Source of truth for a single game's resolved media files. Domain only depends on this
 * interface - how media is actually located on disk (extension probing, the configured
 * media root folder) is entirely a data-layer concern.
 *
 * [systemPath] is ES-DE's own reported ROM directory for this system - see
 * GameMediaPathResolver's kdoc for why this (rather than assuming the ROM folder on disk is
 * named after [systemShortName]) is what makes media resolution correct for a system whose
 * ROM folder has a custom or differently-cased name. Null when it hasn't been observed yet
 * this session (e.g. immediately after a cold-start replay) - implementations fall back to
 * a best-effort marker search in that case.
 *
 * [mediaTypes] scopes the lookup to exactly what the caller needs (e.g. the GameMedia
 * widgets currently placed on a canvas, or the single type a video/manual overlay needs)
 * rather than probing every [MediaType] on every call regardless of relevance.
 */
interface GameMediaRepository {
    suspend fun resolveMedia(
        systemShortName: String,
        systemPath: String?,
        romPath: String,
        mediaTypes: Set<MediaType>,
    ): GameMedia
}
