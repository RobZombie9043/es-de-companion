package com.esde.companion.data.backup

import com.esde.companion.domain.model.AppConfigBackup
import com.esde.companion.domain.model.DockSize
import com.esde.companion.domain.model.FabAssignments
import com.esde.companion.domain.model.HallSensorCalibration
import com.esde.companion.domain.model.MusicDuckingMode
import com.esde.companion.domain.model.SavedWidgetCanvas
import com.esde.companion.domain.model.ScreenBehavior
import com.esde.companion.domain.model.StateGroup
import com.esde.companion.domain.model.ThemePreference
import com.esde.companion.domain.model.VolumeSyncMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonConfigBackupRepositoryTest {
    private fun minimalSnapshot() =
        AppConfigBackup(
            logFolderPath = "/storage/emulated/0/ES-DE",
            mediaFolderPath = "/storage/emulated/0/ES-DE/downloaded_media",
            customSystemImagesFolderPath = null,
            customLogosFolderPath = null,
            customMusicFolderPath = null,
            themePreference = ThemePreference.Auto,
            gamePlayingBehavior = ScreenBehavior.Nothing,
            gamePlayingDimPercent = 50,
            screensaverBehavior = ScreenBehavior.Nothing,
            screensaverDimPercent = 50,
            overlayOpacityPercent = 80,
            fabAssignments = FabAssignments.Default,
            videoPlaybackEnabled = false,
            videoDelaySeconds = 3,
            videoAudioEnabled = true,
            musicEnabled = true,
            musicPlayWhileBrowsingSystems = true,
            musicPlayWhileBrowsingGames = true,
            musicPlayDuringScreensaver = false,
            musicDuckingMode = MusicDuckingMode.LowerVolume,
            closeCompanionOnQuitEnabled = false,
            launchEsdeOnStartEnabled = false,
            debugLoggingEnabled = false,
            hiddenApps = emptySet(),
            gridColumns = 5,
            otherScreenLaunchApps = emptySet(),
            sortFoldersOnTop = true,
            showSearchBar = true,
            folders = emptyList(),
            dockEnabled = false,
            dockMaxApps = 5,
            dockSize = DockSize.Medium,
            dockApps = emptyList(),
            // Matches what ExportConfigBackupUseCase actually produces - one entry per
            // StateGroup, always, defaulting to an empty canvas rather than an absent key
            // (see ConfigBackupMapping's toDomainMap, which always reconstructs every entry).
            widgetCanvases = StateGroup.entries.associateWith { SavedWidgetCanvas(grid = null, widgets = emptyList()) },
            lidWakeGuardEnabled = false,
            hallSensorCalibration = HallSensorCalibration.Uncalibrated,
            autoFpsEnabled = false,
            autoFpsTriggerPackages = emptySet(),
            taskKillerEnabled = false,
            taskKillerExcludedPackages = setOf("org.es_de.frontend", "com.esde.companion"),
            volumeSyncEnabled = false,
            volumeSyncMode = VolumeSyncMode.Linked,
        )

    @Test
    fun `serialize then deserialize reproduces the original snapshot`() {
        val repository = JsonConfigBackupRepository()
        val snapshot = minimalSnapshot()

        val result = repository.deserialize(repository.serialize(snapshot))

        assertEquals(Result.success(snapshot), result)
    }

    @Test
    fun `malformed JSON fails as a Result rather than throwing`() {
        val repository = JsonConfigBackupRepository()

        val result = repository.deserialize("this is not json")

        assertTrue(result.isFailure)
    }

    @Test
    fun `a version newer than this build understands fails`() {
        val repository = JsonConfigBackupRepository()
        val json = repository.serialize(minimalSnapshot()).replace("\"version\":1", "\"version\":999")

        val result = repository.deserialize(json)

        assertTrue(result.isFailure)
    }

    @Test
    fun `a backup from before the Dim slider existed decodes with the default dim percent`() {
        val repository = JsonConfigBackupRepository()
        val json =
            repository.serialize(minimalSnapshot())
                .replace("\"gamePlayingDimPercent\":50,", "")
                .replace("\"screensaverDimPercent\":50,", "")

        val result = repository.deserialize(json)

        assertEquals(Result.success(minimalSnapshot()), result)
    }

    @Test
    fun `unknown extra JSON keys are ignored rather than failing decode`() {
        val repository = JsonConfigBackupRepository()
        val json = repository.serialize(minimalSnapshot())
        val withExtraKey = json.dropLast(1) + ""","futureField":"someValue"}"""

        val result = repository.deserialize(withExtraKey)

        assertEquals(Result.success(minimalSnapshot()), result)
    }
}
