package com.esde.companion.domain.usecase

import com.esde.companion.data.backup.JsonConfigBackupRepository
import com.esde.companion.domain.model.AppFolder
import com.esde.companion.domain.model.DockSize
import com.esde.companion.domain.model.FabAssignments
import com.esde.companion.domain.model.FabSlot
import com.esde.companion.domain.model.FabType
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fixture repositories with distinctive, non-default values for the export-then-restore
 * round trip test - one entry per repository [ExportConfigBackupUseCase]/
 * [RestoreConfigBackupUseCase] touch, plus the folder/canvas fixtures the test asserts on. */
private class SourceFixture {
    val fabAssignments = FabAssignments.Default.copy(bottomEnd = FabSlot(FabType.CustomApp, "com.example.app"))
    val onboarding =
        FakeOnboardingRepository(
            logFolderPath = "/storage/emulated/0/CustomESDE",
            themePreference = ThemePreference.Dark,
            gamePlayingBehavior = ScreenBehavior.Dim,
            gamePlayingDimPercent = 70,
            screensaverBehavior = ScreenBehavior.Black,
            screensaverDimPercent = 25,
            overlayOpacityPercent = 33,
            fabAssignments = fabAssignments,
            musicDuckingMode = MusicDuckingMode.Pause,
            debugLoggingEnabled = true,
            updateAchievementsOnScreensaverEnabled = false,
        )
    val appDrawer =
        FakeAppDrawerSettingsRepository(hiddenApps = setOf("com.hidden.app"), gridColumns = 6, showSearchBar = false)
    val folder = AppFolder(id = "folder-1", name = "Arcade", memberPackageNames = setOf("com.arcade.app"))
    val appFolders = FakeAppFolderRepository(folders = listOf(folder))
    val dock =
        FakeDockSettingsRepository(dockEnabled = true, dockSize = DockSize.Large, dockApps = listOf("com.dock.app"))
    val canvas = SavedWidgetCanvas(grid = GridDimensions(columns = 8, rows = 5), widgets = listOf(samplePlacedWidget()))
    val widgets = FakeWidgetLayoutRepository(initial = mapOf(StateGroup.System to canvas))
    val override = GameMatchOverride(systemShortName = "snes", romPath = "/roms/snes/game.sfc", raGameId = 42L)
    val gameMatchOverrides = FakeGameMatchOverrideRepository(initial = listOf(override))
    val calibration =
        HallSensorCalibration(sensorType = 5, sensorName = "Hall Sensor", closedValue = 1f, openValue = 0f)
    val thorSettings =
        FakeThorSettingsRepository(
            lidWakeGuardEnabled = true,
            hallSensorCalibration = calibration,
            autoFpsEnabled = true,
            autoFpsTriggerPackages = setOf("org.libretro.retroarch"),
        )
    val repositories =
        BackupRepositories(onboarding, appDrawer, appFolders, dock, widgets, thorSettings, gameMatchOverrides)

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

class RestoreConfigBackupUseCaseTest {
    @Test
    fun `export from one set of repositories then restore into fresh ones reproduces every field`() =
        runTest {
            val source = SourceFixture()
            val configBackupRepository = JsonConfigBackupRepository()
            val json = ExportConfigBackupUseCase(source.repositories, configBackupRepository)()

            val targetOnboarding = FakeOnboardingRepository()
            val targetAppDrawer = FakeAppDrawerSettingsRepository()
            val targetAppFolders = FakeAppFolderRepository()
            val targetDock = FakeDockSettingsRepository()
            val targetWidgets = FakeWidgetLayoutRepository()
            val targetThorSettings = FakeThorSettingsRepository()
            val targetGameMatchOverrides = FakeGameMatchOverrideRepository()
            val targetRepositories =
                BackupRepositories(
                    targetOnboarding,
                    targetAppDrawer,
                    targetAppFolders,
                    targetDock,
                    targetWidgets,
                    targetThorSettings,
                    targetGameMatchOverrides,
                )
            val restoreUseCase = RestoreConfigBackupUseCase(targetRepositories, configBackupRepository)

            val result = restoreUseCase(json)

            assertTrue(result.isSuccess)
            assertEquals("/storage/emulated/0/CustomESDE", targetOnboarding.observeLogFolderPath().first())
            assertEquals(ThemePreference.Dark, targetOnboarding.observeThemePreference().first())
            assertEquals(ScreenBehavior.Dim, targetOnboarding.observeGamePlayingBehavior().first())
            assertEquals(70, targetOnboarding.observeGamePlayingDimPercent().first())
            assertEquals(25, targetOnboarding.observeScreensaverDimPercent().first())
            assertEquals(33, targetOnboarding.observeOverlayOpacityPercent().first())
            assertEquals(FabType.CustomApp, targetOnboarding.observeFabAssignments().first().bottomEnd.type)
            assertEquals(MusicDuckingMode.Pause, targetOnboarding.observeMusicDuckingMode().first())
            assertTrue(targetOnboarding.observeDebugLoggingEnabled().first())
            assertFalse(targetOnboarding.observeUpdateAchievementsOnScreensaverEnabled().first())
            assertEquals(setOf("com.hidden.app"), targetAppDrawer.observeHiddenApps().first())
            assertEquals(6, targetAppDrawer.observeGridColumns().first())
            assertFalse(targetAppDrawer.observeShowSearchBar().first())
            assertEquals(listOf(source.folder), targetAppFolders.observeFolders().first())
            assertTrue(targetDock.observeDockEnabled().first())
            assertEquals(DockSize.Large, targetDock.observeDockSize().first())
            assertEquals(listOf("com.dock.app"), targetDock.observeDockApps().first())
            assertEquals(source.canvas, targetWidgets.observeCanvas(StateGroup.System).first())
            assertEquals(listOf(source.override), targetGameMatchOverrides.observeAllOverrides().first())
            assertTrue(targetThorSettings.observeLidWakeGuardEnabled().first())
            assertEquals(source.calibration, targetThorSettings.observeHallSensorCalibration().first())
            assertTrue(targetThorSettings.observeAutoFpsEnabled().first())
            assertEquals(setOf("org.libretro.retroarch"), targetThorSettings.observeAutoFpsTriggerPackages().first())
        }

    @Test
    fun `a malformed backup fails without touching any repository`() =
        runTest {
            val onboarding = FakeOnboardingRepository(themePreference = ThemePreference.Light)
            val appDrawer = FakeAppDrawerSettingsRepository(gridColumns = 4)
            val repositories =
                BackupRepositories(
                    onboarding,
                    appDrawer,
                    FakeAppFolderRepository(),
                    FakeDockSettingsRepository(),
                    FakeWidgetLayoutRepository(),
                    FakeThorSettingsRepository(),
                    FakeGameMatchOverrideRepository(),
                )
            val useCase = RestoreConfigBackupUseCase(repositories, JsonConfigBackupRepository())

            val result = useCase("not valid json")

            assertTrue(result.isFailure)
            assertEquals(ThemePreference.Light, onboarding.observeThemePreference().first())
            assertEquals(4, appDrawer.observeGridColumns().first())
        }

    @Test
    fun `a backup from a newer app version fails`() =
        runTest {
            val repositories =
                BackupRepositories(
                    FakeOnboardingRepository(),
                    FakeAppDrawerSettingsRepository(),
                    FakeAppFolderRepository(),
                    FakeDockSettingsRepository(),
                    FakeWidgetLayoutRepository(),
                    FakeThorSettingsRepository(),
                    FakeGameMatchOverrideRepository(),
                )
            val configBackupRepository = JsonConfigBackupRepository()
            val useCase = RestoreConfigBackupUseCase(repositories, configBackupRepository)
            val futureJson =
                ExportConfigBackupUseCase(repositories, configBackupRepository)()
                    .replace("\"version\":1", "\"version\":999")

            val result = useCase(futureJson)

            assertTrue(result.isFailure)
        }
}
