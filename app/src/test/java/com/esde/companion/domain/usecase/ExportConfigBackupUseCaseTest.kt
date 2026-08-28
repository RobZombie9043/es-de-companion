package com.esde.companion.domain.usecase

import com.esde.companion.data.backup.JsonConfigBackupRepository
import com.esde.companion.domain.model.AppFolder
import com.esde.companion.domain.model.DockSize
import com.esde.companion.domain.model.FabAssignments
import com.esde.companion.domain.model.FabSlot
import com.esde.companion.domain.model.FabType
import com.esde.companion.domain.model.GameLaunchDisplayTarget
import com.esde.companion.domain.model.GameLaunchOverride
import com.esde.companion.domain.model.GameMatchOverride
import com.esde.companion.domain.model.GridDimensions
import com.esde.companion.domain.model.HallSensorCalibration
import com.esde.companion.domain.model.MusicDuckingMode
import com.esde.companion.domain.model.PlacedWidget
import com.esde.companion.domain.model.SavedWidgetCanvas
import com.esde.companion.domain.model.ScreenBehavior
import com.esde.companion.domain.model.StateGroup
import com.esde.companion.domain.model.ThemePreference
import com.esde.companion.domain.model.WidgetType
import com.esde.companion.domain.repository.BackupRepositories
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fixture repositories with distinctive, non-default values for the export test - split out
 * of the test method itself purely to stay under detekt's LongMethod threshold. */
private class ExportFixture {
    val fabAssignments = FabAssignments.Default.copy(bottomStart = FabSlot(FabType.CustomApp, "com.example.app"))
    val onboarding =
        FakeOnboardingRepository(
            logFolderPath = "/storage/emulated/0/CustomESDE",
            mediaFolderPath = "/storage/emulated/0/CustomESDE/media",
            customSystemImagesFolderPath = "/storage/emulated/0/SystemImages",
            customLogosFolderPath = "/storage/emulated/0/Logos",
            customMusicFolderPath = "/storage/emulated/0/Music",
            themePreference = ThemePreference.Dark,
            gamePlayingBehavior = ScreenBehavior.Dim,
            gamePlayingDimPercent = 65,
            screensaverBehavior = ScreenBehavior.Black,
            screensaverDimPercent = 20,
            overlayOpacityPercent = 42,
            fabAssignments = fabAssignments,
            musicEnabled = false,
            musicPlayWhileBrowsingSystems = false,
            musicPlayWhileBrowsingGames = false,
            musicPlayDuringScreensaver = true,
            musicDuckingMode = MusicDuckingMode.Pause,
            closeCompanionOnQuitEnabled = true,
            launchEsdeOnStartEnabled = true,
            debugLoggingEnabled = true,
            updateAchievementsOnScreensaverEnabled = false,
        )
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
    val canvas = SavedWidgetCanvas(grid = GridDimensions(columns = 8, rows = 5), widgets = listOf(samplePlacedWidget()))
    val widgets = FakeWidgetLayoutRepository(initial = mapOf(StateGroup.System to canvas))
    val calibration =
        HallSensorCalibration(sensorType = 5, sensorName = "Hall Sensor", closedValue = 1f, openValue = 0f)
    val thorSettings =
        FakeThorSettingsRepository(
            lidWakeGuardEnabled = true,
            hallSensorCalibration = calibration,
            autoFpsEnabled = true,
            autoFpsTriggerPackages = setOf("org.libretro.retroarch"),
        )
    val override = GameMatchOverride(systemShortName = "snes", romPath = "/roms/snes/game.sfc", raGameId = 42L)
    val gameMatchOverrides = FakeGameMatchOverrideRepository(initial = listOf(override))
    val launchOverride = GameLaunchOverride(systemShortName = "n64", relativeRomPath = "./Game.z64", packageName = null)
    val gameLaunchApp =
        FakeGameLaunchAppRepository(
            initialSystemDefaults = mapOf("n64" to "com.example.launcher"),
            initialGameOverrides = listOf(launchOverride),
            initialLaunchDisplayTarget = GameLaunchDisplayTarget.OtherScreen,
            initialCloseAppOnGameEnd = true,
        )
    val repositories =
        BackupRepositories(
            onboarding,
            appDrawer,
            appFolders,
            dock,
            widgets,
            thorSettings,
            gameMatchOverrides,
            gameLaunchApp,
        )

    private fun samplePlacedWidget() =
        PlacedWidget(
            id = "widget-1",
            widgetType = WidgetType.ColorBackground(colorArgb = 0xFF112233, alpha = 1f),
            gridColumn = 0,
            gridRow = 0,
            columnSpan = 2,
            rowSpan = 2,
            zIndex = 0,
        )
}

class ExportConfigBackupUseCaseTest {
    @Test
    fun `every field from every repository is present in the exported snapshot`() =
        runTest {
            val fixture = ExportFixture()
            val configBackupRepository = JsonConfigBackupRepository()
            val json = ExportConfigBackupUseCase(fixture.repositories, configBackupRepository)()
            val snapshot = configBackupRepository.deserialize(json).getOrThrow()
            val emptyCanvas = SavedWidgetCanvas(grid = null, widgets = emptyList())

            assertEquals("/storage/emulated/0/CustomESDE", snapshot.logFolderPath)
            assertEquals(ThemePreference.Dark, snapshot.themePreference)
            assertEquals(ScreenBehavior.Dim, snapshot.gamePlayingBehavior)
            assertEquals(65, snapshot.gamePlayingDimPercent)
            assertEquals(20, snapshot.screensaverDimPercent)
            assertEquals(42, snapshot.overlayOpacityPercent)
            assertEquals(FabType.CustomApp, snapshot.fabAssignments.bottomStart.type)
            assertEquals("com.example.app", snapshot.fabAssignments.bottomStart.customAppPackageName)
            assertEquals(MusicDuckingMode.Pause, snapshot.musicDuckingMode)
            assertEquals(setOf("com.hidden.app"), snapshot.hiddenApps)
            assertEquals(6, snapshot.gridColumns)
            assertEquals(listOf(fixture.folder), snapshot.folders)
            assertEquals(DockSize.Large, snapshot.dockSize)
            assertEquals(listOf("com.dock.app"), snapshot.dockApps)
            assertEquals(fixture.canvas, snapshot.widgetCanvases[StateGroup.System])
            assertEquals(emptyCanvas, snapshot.widgetCanvases[StateGroup.Playing])
            assertEquals(listOf(fixture.override), snapshot.gameMatchOverrides)
            assertEquals(false, snapshot.updateAchievementsOnScreensaverEnabled)
            assertTrue(snapshot.lidWakeGuardEnabled)
            assertEquals(fixture.calibration, snapshot.hallSensorCalibration)
            assertTrue(snapshot.autoFpsEnabled)
            assertEquals(setOf("org.libretro.retroarch"), snapshot.autoFpsTriggerPackages)
            assertEquals(mapOf("n64" to "com.example.launcher"), snapshot.gameLaunchSystemDefaults)
            assertEquals(listOf(fixture.launchOverride), snapshot.gameLaunchOverrides)
            assertEquals(GameLaunchDisplayTarget.OtherScreen, snapshot.gameLaunchDisplayTarget)
            assertTrue(snapshot.closeAppOnGameEndEnabled)
        }
}
