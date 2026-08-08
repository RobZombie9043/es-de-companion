package com.esde.companion.domain.model

/**
 * A single GitHub release, mapped down to what the update checker actually needs.
 *
 * [versionName] has the tag's leading "v" stripped so it's directly comparable to
 * `BuildConfig.VERSION_NAME` (e.g. tag `v0.7.0-RC1` -> versionName `0.7.0-RC1`).
 * [apkAsset] is null when the release has no `.apk` asset attached - a release can
 * exist without one (e.g. source-only tags), which the update checker treats as "found
 * a release, but there's nothing installable" rather than pretending it doesn't exist.
 */
data class ReleaseInfo(
    val versionName: String,
    val tagName: String,
    val releaseNotes: String,
    val apkAsset: ApkAsset?,
    val htmlUrl: String,
)
