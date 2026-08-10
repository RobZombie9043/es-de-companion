package com.esde.companion.domain.usecase

import com.esde.companion.data.backup.JsonConfigBackupRepository
import com.esde.companion.domain.model.AppFolder
import com.esde.companion.domain.model.DockSize
import com.esde.companion.domain.model.FabAssignments
import com.esde.companion.domain.model.FabSlot
import com.esde.companion.domain.model.FabType
import com.esde.companion.domain.model.GridDimensions
import com.esde.companion.domain.model.MusicDuckingMode
import com.esde.companion.domain.model.PlacedWidget
import com.esde.companion.domain.model.SavedWidgetCanvas
import com.esde.companion.domain.model.ScreenBehavior
import com.esde.companion.domain.model.StateGroup
import com.esde.companion.domain.model.ThemePreference
import com.esde.companion.domain.model.WidgetType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportConfigBackupUseCaseTest {
    private fun placedWidget() =
        PlacedWidget(
            id = "widget-1",
            widgetType = WidgetType.ColorBackground(colorArgb = 0xFF112233, alpha = 1f),
            gridColumn = 0,
            gridRow = 0,
            columnSpan = 2,
            rowSpan = 2,
            zIndex = 0,
        )

    private fun onboardingWithSampleValues(fabAssignments: FabAssignments) =
        FakeOnboardingRepository(
            logFolderPath = "/storage/emulated/0/CustomESDE",
            mediaFolderPath = "/storage/emulated/0/CustomESDE/media",
            customSystemImagesFolderPath = "/storage/emulated/0/SystemImages",
            customLogosFolderPath = "/storage/emulated/0/Logos",
            customMusicFolderPath = "/storage/emulated/0/Music",
            themePreference = ThemePreference.Dark,
            gamePlayingBehavior = ScreenBehavior.Dim,
            screensaverBehavior = ScreenBehavior.Black,
            overlayOpacityPercent = 42,
            fabAssignments = fabAssignments,
            videoPlaybackEnabled = true,
            videoDelaySeconds = 7,
            videoAudioEnabled = false,
            musicEnabled = false,
            musicPlayWhileBrowsingSystems = false,
            musicPlayWhileBrowsingGames = false,
            musicPlayDuringScreensaver = true,
            musicDuckingMode = MusicDuckingMode.Pause,
            closeCompanionOnQuitEnabled = true,
            launchEsdeOnStartEnabled = true,
            debugLoggingEnabled = true,
        )

    @Test
    fun `every field from every repository is present in the exported snapshot`() =
        runTest {
            val fabAssignments =
                FabAssignments.Default.copy(bottomStart = FabSlot(FabType.CustomApp, "com.example.app"))
            val onboarding = onboardingWithSampleValues(fabAssignments)
            val appDrawer =
                FakeAppDrawerSettingsRepository(
                    hiddenApps = setOf("com.hidden.app"),
                    gridColumns = 6,
                    otherScreenLaunchApps = setOf("com.other.app"),
                    sortFoldersOnTop = false,
                    showSearchBar = false,
                )
            val folder = AppFolder(id = "folder-1", name = "Arcade", memberPackageNames = setOf("com.arcade.app"))
            val appFolders = FakeAppFolderRepository(folders = listOf(folder))
            val dock =
                FakeDockSettingsRepository(
                    dockEnabled = true,
                    dockMaxApps = 3,
                    dockSize = DockSize.Large,
                    dockApps = listOf("com.dock.app"),
                )
            val canvasGrid = GridDimensions(columns = 8, rows = 5)
            val canvas = SavedWidgetCanvas(grid = canvasGrid, widgets = listOf(placedWidget()))
            val widgets = FakeWidgetLayoutRepository(initial = mapOf(StateGroup.System to canvas))
            val configBackupRepository = JsonConfigBackupRepository()

            val useCase =
                ExportConfigBackupUseCase(onboarding, appDrawer, appFolders, dock, widgets, configBackupRepository)
            val json = useCase()
            val snapshot = configBackupRepository.deserialize(json).getOrThrow()
            val emptyCanvas = SavedWidgetCanvas(grid = null, widgets = emptyList())

            assertEquals("/storage/emulated/0/CustomESDE", snapshot.logFolderPath)
            assertEquals(ThemePreference.Dark, snapshot.themePreference)
            assertEquals(ScreenBehavior.Dim, snapshot.gamePlayingBehavior)
            assertEquals(42, snapshot.overlayOpacityPercent)
            assertEquals(FabType.CustomApp, snapshot.fabAssignments.bottomStart.type)
            assertEquals("com.example.app", snapshot.fabAssignments.bottomStart.customAppPackageName)
            assertTrue(snapshot.videoPlaybackEnabled)
            assertEquals(MusicDuckingMode.Pause, snapshot.musicDuckingMode)
            assertEquals(setOf("com.hidden.app"), snapshot.hiddenApps)
            assertEquals(6, snapshot.gridColumns)
            assertEquals(listOf(folder), snapshot.folders)
            assertEquals(DockSize.Large, snapshot.dockSize)
            assertEquals(listOf("com.dock.app"), snapshot.dockApps)
            assertEquals(canvas, snapshot.widgetCanvases[StateGroup.System])
            assertEquals(emptyCanvas, snapshot.widgetCanvases[StateGroup.Playing])
        }
}
