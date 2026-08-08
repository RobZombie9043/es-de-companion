package com.esde.companion.domain.repository

import com.esde.companion.domain.model.ApkAsset
import com.esde.companion.domain.model.DownloadState
import com.esde.companion.domain.model.ReleaseFetchResult
import kotlinx.coroutines.flow.Flow

/**
 * Fetches release information from GitHub Releases and downloads a release's `.apk`
 * asset. The sole network-facing repository in this app - see CLAUDE.md's "What NOT
 * to Do" for why this is a deliberate, narrow exception rather than a general backend.
 */
interface UpdateRepository {
    /** The most recently published release. */
    suspend fun fetchLatestRelease(): ReleaseFetchResult

    /** The release tagged for a specific version name (e.g. "0.7.0-RC1" -> tag "v0.7.0-RC1"). */
    suspend fun fetchReleaseForVersion(versionName: String): ReleaseFetchResult

    fun downloadApk(asset: ApkAsset): Flow<DownloadState>
}
