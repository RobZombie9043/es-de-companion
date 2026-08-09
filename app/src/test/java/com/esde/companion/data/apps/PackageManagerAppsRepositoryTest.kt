package com.esde.companion.data.apps

import com.esde.companion.domain.model.InstalledApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageManagerAppsRepositoryTest {
    @Test
    fun `excludes the app's own package`() {
        val apps =
            listOf(
                InstalledApp(packageName = "com.esde.companion", label = "ES-DE Companion"),
                InstalledApp(packageName = "com.example.foo", label = "Foo"),
            )

        val result = dedupeAndSort(apps, excludePackageName = "com.esde.companion")

        assertTrue(result.none { it.packageName == "com.esde.companion" })
    }

    @Test
    fun `deduplicates by package name, keeping the first occurrence`() {
        val apps =
            listOf(
                InstalledApp(packageName = "com.example.foo", label = "First Label"),
                InstalledApp(packageName = "com.example.foo", label = "Second Label"),
            )

        val result = dedupeAndSort(apps, excludePackageName = "com.esde.companion")

        assertEquals(1, result.size)
        assertEquals("First Label", result.single().label)
    }

    @Test
    fun `sorts by label case-insensitively`() {
        val apps =
            listOf(
                InstalledApp(packageName = "com.example.banana", label = "banana"),
                InstalledApp(packageName = "com.example.apple", label = "Apple"),
            )

        val result = dedupeAndSort(apps, excludePackageName = "com.esde.companion")

        assertEquals(listOf("Apple", "banana"), result.map { it.label })
    }

    @Test
    fun `empty input returns empty output`() {
        assertTrue(dedupeAndSort(emptyList(), excludePackageName = "com.esde.companion").isEmpty())
    }
}
