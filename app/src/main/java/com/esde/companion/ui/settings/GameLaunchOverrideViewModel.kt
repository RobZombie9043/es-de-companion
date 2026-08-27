package com.esde.companion.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esde.companion.domain.model.GameLaunchOverride
import com.esde.companion.domain.model.GamelistSystemSummary
import com.esde.companion.domain.model.InstalledApp
import com.esde.companion.domain.parser.GamelistGameEntry
import com.esde.companion.domain.usecase.ClearGameLaunchOverrideUseCase
import com.esde.companion.domain.usecase.ListGamelistGamesUseCase
import com.esde.companion.domain.usecase.ListGamelistSystemsUseCase
import com.esde.companion.domain.usecase.ObserveGameLaunchOverridesUseCase
import com.esde.companion.domain.usecase.ObserveGameLaunchSystemDefaultsUseCase
import com.esde.companion.domain.usecase.ObserveInstalledAppsUseCase
import com.esde.companion.domain.usecase.SetGameLaunchOverrideUseCase
import com.esde.companion.domain.usecase.SetGameLaunchSystemDefaultUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val STATE_STOP_TIMEOUT_MILLIS = 5_000L

/**
 * The systems list plus whichever single system is currently drilled into (if any) - kept as
 * one local mutable snapshot, combined below with the live settings repositories, rather than
 * a separate `MutableStateFlow` per field. [ListGamelistSystemsUseCase]/[ListGamelistGamesUseCase]
 * are one-shot suspend calls (not reactive flows - see `GamelistLibraryRepository`), so nothing
 * populates this until [onSystemsScreenShown]/[onSystemSelected] actually asks for it.
 */
private data class LibraryState(
    val systems: List<GamelistSystemSummary> = emptyList(),
    val isLoadingSystems: Boolean = false,
    val currentSystemShortName: String? = null,
    val currentSystemGames: List<GamelistGameEntry> = emptyList(),
    val isLoadingGames: Boolean = false,
)

data class GameLaunchOverrideUiState(
    val systems: List<GamelistSystemSummary> = emptyList(),
    val isLoadingSystems: Boolean = false,
    val installedApps: List<InstalledApp> = emptyList(),
    val systemDefaults: Map<String, String> = emptyMap(),
    val gameOverrides: List<GameLaunchOverride> = emptyList(),
    val currentSystemShortName: String? = null,
    val currentSystemGames: List<GamelistGameEntry> = emptyList(),
    val isLoadingGames: Boolean = false,
)

/**
 * Backs both Game Launch Override subpages (the systems list and a drilled-into system's games)
 * - one ViewModel covering both, same reasoning as [ManageAppsViewModel] being its own ViewModel
 * rather than folded into [SettingsViewModel]. [onSystemsScreenShown] reloads the systems list
 * every time that page is shown (a plain directory/file scan, not expensive enough to warrant
 * caching across visits) and [onSystemSelected] loads one system's games on demand when its page
 * is entered. System-default/per-game changes apply immediately, matching the rest of Settings.
 */
@Suppress("LongParameterList")
class GameLaunchOverrideViewModel(
    private val listGamelistSystems: ListGamelistSystemsUseCase,
    private val listGamelistGames: ListGamelistGamesUseCase,
    observeInstalledApps: ObserveInstalledAppsUseCase,
    observeGameLaunchSystemDefaults: ObserveGameLaunchSystemDefaultsUseCase,
    private val setGameLaunchSystemDefault: SetGameLaunchSystemDefaultUseCase,
    observeGameLaunchOverrides: ObserveGameLaunchOverridesUseCase,
    private val setGameLaunchOverride: SetGameLaunchOverrideUseCase,
    private val clearGameLaunchOverride: ClearGameLaunchOverrideUseCase,
) : ViewModel() {
    private val libraryState = MutableStateFlow(LibraryState())

    val uiState: StateFlow<GameLaunchOverrideUiState> =
        combine(
            libraryState,
            observeInstalledApps(),
            observeGameLaunchSystemDefaults(),
            observeGameLaunchOverrides(),
        ) { library, installedApps, systemDefaults, gameOverrides ->
            GameLaunchOverrideUiState(
                systems = library.systems,
                isLoadingSystems = library.isLoadingSystems,
                installedApps = installedApps,
                systemDefaults = systemDefaults,
                gameOverrides = gameOverrides,
                currentSystemShortName = library.currentSystemShortName,
                currentSystemGames = library.currentSystemGames,
                isLoadingGames = library.isLoadingGames,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STATE_STOP_TIMEOUT_MILLIS),
            initialValue = GameLaunchOverrideUiState(),
        )

    fun onSystemsScreenShown() {
        libraryState.value = libraryState.value.copy(isLoadingSystems = true)
        viewModelScope.launch {
            val systems = listGamelistSystems()
            libraryState.value = libraryState.value.copy(systems = systems, isLoadingSystems = false)
        }
    }

    fun onSystemSelected(systemShortName: String) {
        libraryState.value =
            libraryState.value.copy(
                currentSystemShortName = systemShortName,
                currentSystemGames = emptyList(),
                isLoadingGames = true,
            )
        viewModelScope.launch {
            val games = listGamelistGames(systemShortName)
            // A fast back-then-forward navigation could let a slower fetch for a
            // previously-selected system land after a newer selection - only apply the
            // result if it's still for the system this call started for.
            if (libraryState.value.currentSystemShortName != systemShortName) return@launch
            libraryState.value = libraryState.value.copy(currentSystemGames = games, isLoadingGames = false)
        }
    }

    fun onSystemDefaultChanged(
        systemShortName: String,
        packageName: String?,
    ) {
        viewModelScope.launch { setGameLaunchSystemDefault(systemShortName, packageName) }
    }

    fun onGameOverrideChanged(
        systemShortName: String,
        relativeRomPath: String,
        packageName: String?,
    ) {
        viewModelScope.launch { setGameLaunchOverride(systemShortName, relativeRomPath, packageName) }
    }

    fun onGameOverrideClearedToDefault(
        systemShortName: String,
        relativeRomPath: String,
    ) {
        viewModelScope.launch { clearGameLaunchOverride(systemShortName, relativeRomPath) }
    }
}
