package com.esde.companion.data.update

import com.esde.companion.domain.model.ApkAsset
import com.esde.companion.domain.model.ReleaseInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mirrors (a subset of) the GitHub Releases API response shape - decoded with
 * `ignoreUnknownKeys = true` since the real response has far more fields than this app
 * needs (author, prerelease flag, draft flag, etc.).
 */
@Serializable
internal data class GitHubReleaseDto(
    @SerialName("tag_name") val tagName: String,
    @SerialName("body") val body: String? = null,
    @SerialName("html_url") val htmlUrl: String,
    @SerialName("assets") val assets: List<GitHubAssetDto> = emptyList(),
)

@Serializable
internal data class GitHubAssetDto(
    @SerialName("name") val name: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String,
    @SerialName("size") val size: Long,
)

internal fun GitHubReleaseDto.toDomain(): ReleaseInfo =
    ReleaseInfo(
        versionName = tagName.removePrefix("v").removePrefix("V"),
        tagName = tagName,
        releaseNotes = body.orEmpty(),
        apkAsset = assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }?.toDomain(),
        htmlUrl = htmlUrl,
    )

private fun GitHubAssetDto.toDomain(): ApkAsset = ApkAsset(browserDownloadUrl, name, size)
