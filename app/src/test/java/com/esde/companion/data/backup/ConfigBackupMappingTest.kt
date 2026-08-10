package com.esde.companion.data.backup

import com.esde.companion.data.settings.CanvasDto
import com.esde.companion.domain.model.AppConfigBackup
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConfigBackupMappingTest {
    private fun sampleSnapshot(widgetCanvases: Map<StateGroup, SavedWidgetCanvas> = emptyMap()) =
        AppConfigBackup(
            logFolderPath = "/storage/emulated/0/ES-DE",
            mediaFolderPath = "/storage/emulated/0/ES-DE/downloaded_media",
            customSystemImagesFolderPath = null,
            customLogosFolderPath = "/storage/emulated/0/Logos",
            customMusicFolderPath = null,
            themePreference = ThemePreference.Dark,
            gamePlayingBehavior = ScreenBehavior.Dim,
            screensaverBehavior = ScreenBehavior.GameManual,
            overlayOpacityPercent = 55,
            fabAssignments = FabAssignments.Default.copy(bottomStart = FabSlot(FabType.CustomApp, "com.example.app")),
            videoPlaybackEnabled = true,
            videoDelaySeconds = 4,
            videoAudioEnabled = false,
            musicEnabled = false,
            musicPlayWhileBrowsingSystems = true,
            musicPlayWhileBrowsingGames = false,
            musicPlayDuringScreensaver = true,
            musicDuckingMode = MusicDuckingMode.Unchanged,
            closeCompanionOnQuitEnabled = true,
            launchEsdeOnStartEnabled = false,
            debugLoggingEnabled = true,
            hiddenApps = setOf("com.hidden.app"),
            gridColumns = 6,
            otherScreenLaunchApps = setOf("com.other.app"),
            sortFoldersOnTop = false,
            showSearchBar = false,
            folders = listOf(AppFolder(id = "folder-1", name = "Arcade", memberPackageNames = setOf("com.arcade.app"))),
            dockEnabled = true,
            dockMaxApps = 3,
            dockSize = DockSize.Small,
            dockApps = listOf("com.dock.app"),
            widgetCanvases = widgetCanvases,
        )

    private fun sampleCanvas() =
        SavedWidgetCanvas(
            grid = GridDimensions(columns = 8, rows = 5),
            widgets = listOf(samplePlacedWidget()),
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

    @Test
    fun `a full snapshot round-trips through the DTO unchanged`() {
        val canvas = sampleCanvas()
        val emptyCanvas = SavedWidgetCanvas(grid = null, widgets = emptyList())
        val snapshot =
            sampleSnapshot(widgetCanvases = mapOf(StateGroup.System to canvas, StateGroup.Playing to emptyCanvas))

        val roundTripped = snapshot.toDto().toDomain()

        // Both StateGroup entries were present (one real, one empty) in the input, and
        // toDomain() always reconstructs every StateGroup entry - see toDomainMap() - so
        // the round trip reproduces the snapshot exactly, not just the non-empty canvas.
        assertEquals(snapshot, roundTripped)
    }

    @Test
    fun `a canvas with no grid saved yet is omitted from the DTO and reconstructed as empty on the way back`() {
        val emptyCanvas = SavedWidgetCanvas(grid = null, widgets = emptyList())
        val snapshot = sampleSnapshot(widgetCanvases = mapOf(StateGroup.System to emptyCanvas))

        val dto = snapshot.toDto()
        val roundTripped = dto.toDomain()

        assertEquals(emptyMap<String, CanvasDto>(), dto.widgetCanvases.filterKeys { it == StateGroup.System.name })
        assertEquals(emptyCanvas, roundTripped.widgetCanvases[StateGroup.System])
    }

    @Test
    fun `an unrecognized enum string falls back to a sensible default instead of failing`() {
        val dto = sampleSnapshot().toDto().copy(themePreference = "SomeFutureTheme", dockSize = "SomeFutureSize")

        val domain = dto.toDomain()

        assertEquals(ThemePreference.Auto, domain.themePreference)
        assertEquals(DockSize.Medium, domain.dockSize)
    }

    @Test
    fun `nullable folder paths round-trip as null`() {
        val dto = sampleSnapshot().toDto()

        assertNull(dto.customSystemImagesFolderPath)
        assertNull(dto.toDomain().customSystemImagesFolderPath)
    }
}
