package com.esde.companion.data.update

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private val json = Json { ignoreUnknownKeys = true }

class GitHubReleaseDtoTest {
    @Test
    fun `maps a realistic releases-latest response to ReleaseInfo`() {
        val body =
            """
            {
              "tag_name": "v0.7.0-RC1",
              "body": "## What's new\n- Fixed a bug",
              "html_url": "https://github.com/RobZombie9043/es-de-companion/releases/tag/v0.7.0-RC1",
              "assets": [
                {
                  "name": "ES-DE-Companion-v0.7.0-RC1.apk",
                  "browser_download_url": "https://github.com/RobZombie9043/es-de-companion/releases/download/v0.7.0-RC1/ES-DE-Companion-v0.7.0-RC1.apk",
                  "size": 12345678
                }
              ]
            }
            """.trimIndent()

        val release = json.decodeFromString<GitHubReleaseDto>(body).toDomain()

        assertEquals("0.7.0-RC1", release.versionName)
        assertEquals("v0.7.0-RC1", release.tagName)
        assertEquals("## What's new\n- Fixed a bug", release.releaseNotes)
        assertEquals(
            "https://github.com/RobZombie9043/es-de-companion/releases/tag/v0.7.0-RC1",
            release.htmlUrl,
        )
        assertEquals("ES-DE-Companion-v0.7.0-RC1.apk", release.apkAsset?.fileName)
        assertEquals(12345678L, release.apkAsset?.sizeBytes)
    }

    @Test
    fun `apkAsset is null when the release has no apk asset`() {
        val body =
            """
            {
              "tag_name": "v0.7.0-RC1",
              "body": "notes",
              "html_url": "https://example.com/release",
              "assets": []
            }
            """.trimIndent()

        val release = json.decodeFromString<GitHubReleaseDto>(body).toDomain()

        assertNull(release.apkAsset)
    }

    @Test
    fun `picks the apk asset over other attached assets`() {
        val body =
            """
            {
              "tag_name": "v0.7.0-RC1",
              "body": "notes",
              "html_url": "https://example.com/release",
              "assets": [
                { "name": "Source code.zip", "browser_download_url": "https://example.com/source.zip", "size": 1 },
                { "name": "app-release.apk", "browser_download_url": "https://example.com/app-release.apk", "size": 2 }
              ]
            }
            """.trimIndent()

        val release = json.decodeFromString<GitHubReleaseDto>(body).toDomain()

        assertEquals("app-release.apk", release.apkAsset?.fileName)
    }

    @Test
    fun `unrecognized extra fields are ignored rather than failing to decode`() {
        val body =
            """
            {
              "tag_name": "v0.7.0-RC1",
              "body": "notes",
              "html_url": "https://example.com/release",
              "assets": [],
              "id": 12345,
              "author": { "login": "someone" },
              "prerelease": true,
              "draft": false
            }
            """.trimIndent()

        val release = json.decodeFromString<GitHubReleaseDto>(body).toDomain()

        assertTrue(release.versionName == "0.7.0-RC1")
    }

    @Test
    fun `leading v is stripped from versionName but the raw tag is preserved`() {
        val body = """{ "tag_name": "v1.2.3", "html_url": "https://example.com/release" }"""

        val release = json.decodeFromString<GitHubReleaseDto>(body).toDomain()

        assertEquals("1.2.3", release.versionName)
        assertEquals("v1.2.3", release.tagName)
    }

    @Test
    fun `missing body maps to empty release notes`() {
        val body = """{ "tag_name": "v1.2.3", "html_url": "https://example.com/release" }"""

        val release = json.decodeFromString<GitHubReleaseDto>(body).toDomain()

        assertEquals("", release.releaseNotes)
    }
}
