package com.esde.companion.domain.repository

import com.esde.companion.domain.model.MusicPool
import com.esde.companion.domain.model.MusicTrack

data class MusicPoolContents(val pool: MusicPool, val tracks: List<MusicTrack>)

/**
 * Resolves + scans + falls back in a single call: a [MusicPool.PerSystem] request that
 * finds zero tracks returns tracks from [MusicPool.General] instead (never another
 * system's dedicated folder), with the returned [MusicPoolContents.pool] reflecting
 * whichever pool was actually used.
 */
interface MusicLibraryRepository {
    suspend fun loadPool(desired: MusicPool): MusicPoolContents
}
