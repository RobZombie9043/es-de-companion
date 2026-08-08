package com.esde.companion.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class BuildDrawerItemsTest {
    private val appA = InstalledApp(packageName = "com.example.a", label = "Apple")
    private val appB = InstalledApp(packageName = "com.example.b", label = "Banana")
    private val appC = InstalledApp(packageName = "com.example.c", label = "Cherry")

    @Test
    fun `folders sort ahead of apps by default`() {
        // Folder name "Ant" would sort last alphabetically among these three labels if
        // fully interleaved - sortFoldersOnTop defaults to true, so it must still come
        // first regardless.
        val folder = AppFolder(id = "folder-1", name = "Zzz", memberPackageNames = setOf(appC.packageName))

        val result =
            buildDrawerItems(
                installedApps = listOf(appA, appB, appC),
                hiddenPackages = emptySet(),
                folders = listOf(folder),
            )

        assertEquals(
            listOf(DrawerItem.Folder(folder, listOf(appC)), DrawerItem.App(appA), DrawerItem.App(appB)),
            result,
        )
    }

    @Test
    fun `sortFoldersOnTop false fully interleaves folders and apps alphabetically by label`() {
        val folder = AppFolder(id = "folder-1", name = "Banjo", memberPackageNames = setOf(appC.packageName))

        val result =
            buildDrawerItems(
                installedApps = listOf(appA, appB, appC),
                hiddenPackages = emptySet(),
                folders = listOf(folder),
                sortFoldersOnTop = false,
            )

        // Apple, Banana, Banjo (folder) - "Banana" sorts before "Banjo" (4th char 'a' < 'j').
        assertEquals(
            listOf(
                DrawerItem.App(appA),
                DrawerItem.App(appB),
                DrawerItem.Folder(folder, listOf(appC)),
            ),
            result,
        )
    }

    @Test
    fun `a hidden app is excluded from the top level entirely`() {
        val result =
            buildDrawerItems(
                installedApps = listOf(appA, appB),
                hiddenPackages = setOf(appB.packageName),
                folders = emptyList(),
            )

        assertEquals(listOf(DrawerItem.App(appA)), result)
    }

    @Test
    fun `a hidden app is excluded from its folder but the folder itself still renders`() {
        val folder = AppFolder(id = "folder-1", name = "Group", memberPackageNames = setOf(appA.packageName, appB.packageName))

        val result =
            buildDrawerItems(
                installedApps = listOf(appA, appB),
                hiddenPackages = setOf(appB.packageName),
                folders = listOf(folder),
            )

        assertEquals(listOf(DrawerItem.Folder(folder, listOf(appA))), result)
    }

    @Test
    fun `an uninstalled member is silently dropped from a folder`() {
        val folder = AppFolder(id = "folder-1", name = "Group", memberPackageNames = setOf(appA.packageName, "com.example.gone"))

        val result =
            buildDrawerItems(
                installedApps = listOf(appA),
                hiddenPackages = emptySet(),
                folders = listOf(folder),
            )

        assertEquals(listOf(DrawerItem.Folder(folder, listOf(appA))), result)
    }

    @Test
    fun `a grouped app never also appears ungrouped`() {
        val folder = AppFolder(id = "folder-1", name = "Group", memberPackageNames = setOf(appA.packageName))

        val result =
            buildDrawerItems(
                installedApps = listOf(appA, appB),
                hiddenPackages = emptySet(),
                folders = listOf(folder),
                sortFoldersOnTop = false,
            )

        assertEquals(listOf(DrawerItem.App(appB), DrawerItem.Folder(folder, listOf(appA))).sortedBy { it.label }, result)
    }

    @Test
    fun `an app listed in two folders renders in both, tolerated rather than validated`() {
        val folderOne = AppFolder(id = "folder-1", name = "Ant", memberPackageNames = setOf(appA.packageName))
        val folderTwo = AppFolder(id = "folder-2", name = "Zebra", memberPackageNames = setOf(appA.packageName))

        val result =
            buildDrawerItems(
                installedApps = listOf(appA),
                hiddenPackages = emptySet(),
                folders = listOf(folderOne, folderTwo),
            )

        assertEquals(
            listOf(DrawerItem.Folder(folderOne, listOf(appA)), DrawerItem.Folder(folderTwo, listOf(appA))),
            result,
        )
    }

    @Test
    fun `an empty folder still renders rather than vanishing`() {
        val folder = AppFolder(id = "folder-1", name = "Empty", memberPackageNames = emptySet())

        val result =
            buildDrawerItems(
                installedApps = listOf(appA),
                hiddenPackages = emptySet(),
                folders = listOf(folder),
                sortFoldersOnTop = false,
            )

        assertEquals(
            listOf(DrawerItem.App(appA), DrawerItem.Folder(folder, emptyList())).sortedBy { it.label.lowercase() },
            result,
        )
    }

    @Test
    fun `sorting is case-insensitive`() {
        val lowercaseApp = InstalledApp(packageName = "com.example.z", label = "apple juice")

        val result =
            buildDrawerItems(
                installedApps = listOf(appA, lowercaseApp),
                hiddenPackages = emptySet(),
                folders = emptyList(),
            )

        assertEquals(listOf(DrawerItem.App(appA), DrawerItem.App(lowercaseApp)), result)
    }
}
