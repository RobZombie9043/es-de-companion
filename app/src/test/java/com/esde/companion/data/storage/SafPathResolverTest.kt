package com.esde.companion.data.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SafPathResolverTest {
    private val primaryRoot = { "/storage/emulated/0" }

    @Test
    fun `document id without a colon separator returns null`() {
        assertNull(SafPathResolver.resolveFromDocumentId("noColonHere", primaryRoot))
    }

    @Test
    fun `primary volume resolves against the injected root`() {
        assertEquals(
            "/storage/emulated/0/Pictures/foo.jpg",
            SafPathResolver.resolveFromDocumentId("primary:Pictures/foo.jpg", primaryRoot),
        )
    }

    @Test
    fun `primary is matched case-insensitively`() {
        assertEquals(
            "/storage/emulated/0/Music",
            SafPathResolver.resolveFromDocumentId("PRIMARY:Music", primaryRoot),
        )
    }

    @Test
    fun `empty relative path returns the root with no trailing slash`() {
        assertEquals("/storage/emulated/0", SafPathResolver.resolveFromDocumentId("primary:", primaryRoot))
    }

    @Test
    fun `non-primary volume id resolves under storage volume path`() {
        assertEquals(
            "/storage/1234-5678/Music/song.mp3",
            SafPathResolver.resolveFromDocumentId("1234-5678:Music/song.mp3", primaryRoot),
        )
    }
}
