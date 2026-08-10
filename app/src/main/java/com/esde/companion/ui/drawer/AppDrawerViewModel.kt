package com.esde.companion.ui.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esde.companion.domain.model.AppFolder
import com.esde.companion.domain.model.DrawerItem
import com.esde.companion.domain.model.LaunchLocation
import com.esde.companion.domain.model.buildDrawerItems
import com.esde.companion.domain.usecase.ObserveAppFoldersUseCase
import com.esde.companion.domain.usecase.ObserveGridColumnsUseCase
import com.esde.companion.domain.usecase.ObserveHiddenAppsUseCase
import com.esde.companion.domain.usecase.ObserveInstalledAppsUseCase
import com.esde.companion.domain.usecase.ObserveOtherScreenLaunchAppsUseCase
import com.esde.companion.domain.usecase.ObserveOverlayOpacityUseCase
import com.esde.companion.domain.usecase.ObserveShowSearchBarUseCase
import com.esde.companion.domain.usecase.ObserveSortFoldersOnTopUseCase
import com.esde.companion.domain.usecase.SetAppFoldersUseCase
import com.esde.companion.domain.usecase.SetHiddenAppsUseCase
import com.esde.companion.domain.usecase.SetOtherScreenLaunchAppsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class AppDrawerViewModel(
    observeInstalledApps: ObserveInstalledAppsUseCase,
    private val observeHiddenApps: ObserveHiddenAppsUseCase,
    private val setHiddenApps: SetHiddenAppsUseCase,
    private val observeOtherScreenLaunchApps: ObserveOtherScreenLaunchAppsUseCase,
    private val setOtherScreenLaunchApps: SetOtherScreenLaunchAppsUseCase,
    observeOverlayOpacity: ObserveOverlayOpacityUseCase,
    observeGridColumns: ObserveGridColumnsUseCase,
    private val observeAppFolders: ObserveAppFoldersUseCase,
    private val setAppFolders: SetAppFoldersUseCase,
    observeSortFoldersOnTop: ObserveSortFoldersOnTopUseCase,
    observeShowSearchBar: ObserveShowSearchBarUseCase,
) : ViewModel() {
    /** Live search text typed into the drawer header - VM-local, never persisted. A
     * non-empty value switches [drawerItems] to buildDrawerItems' flat search branch. */
    private val searchQueryFlow = MutableStateFlow("")
    val searchQuery: StateFlow<String> = searchQueryFlow.asStateFlow()

    // Merges installed apps (minus hidden ones) with persisted folders into the flat
    // grid - see buildDrawerItems for the actual merge/filter/sort logic, and
    // ObserveSortFoldersOnTopUseCase for the App Drawer setting that picks between
    // folders-first and fully-interleaved-alphabetical ordering.
    val drawerItems: StateFlow<List<DrawerItem>> =
        combine(
            observeInstalledApps(),
            observeHiddenApps(),
            observeAppFolders(),
            observeSortFoldersOnTop(),
            searchQueryFlow,
            ::buildDrawerItems,
        ).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptyList(),
        )

    /** Whether the drawer renders its header (search bar + settings shortcuts) - the
     * Settings > App Drawer and Dock > "Show Search Bar" toggle. */
    val showSearchBar: StateFlow<Boolean> =
        observeShowSearchBar()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = true,
            )

    /** Raw folder list (id/name/membership) - feeds the "add to folder" picker, kept
     * separate from [drawerItems] rather than derived via filterIsInstance since it's the
     * natural shape for that one caller. */
    val folders: StateFlow<List<AppFolder>> =
        observeAppFolders()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = emptyList(),
            )

    /** Packages whose last-used launch location is the other screen - drives both the
     * App Drawer's dot indicator and what a plain single-tap does for that app. */
    val otherScreenLaunchApps: StateFlow<Set<String>> =
        observeOtherScreenLaunchApps()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = emptySet(),
            )

    val drawerOpacityPercent: StateFlow<Int> =
        observeOverlayOpacity()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = 80,
            )

    val gridColumns: StateFlow<Int> =
        observeGridColumns()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = 4,
            )

    /** Persists [location] as the new default for [packageName]. Called for every
     * launch action that should update the remembered preference - the long-press
     * menu's explicit choices and the double-tap gesture - but deliberately not for a
     * plain single tap, which just replays the existing preference instead of writing
     * it again. */
    fun recordLaunchLocation(
        packageName: String,
        location: LaunchLocation,
    ) {
        viewModelScope.launch {
            val current = observeOtherScreenLaunchApps().first()
            val updated = if (location == LaunchLocation.OtherScreen) current + packageName else current - packageName
            setOtherScreenLaunchApps(updated)
        }
    }

    fun setSearchQuery(query: String) {
        searchQueryFlow.value = query
    }

    fun clearSearchQuery() {
        searchQueryFlow.value = ""
    }

    fun hideApp(packageName: String) {
        viewModelScope.launch {
            val current = observeHiddenApps().first()
            setHiddenApps(current + packageName)
        }
    }

    fun createFolderAndAddApp(
        name: String,
        packageName: String,
    ) {
        viewModelScope.launch {
            val newFolder = AppFolder(id = UUID.randomUUID().toString(), name = name, memberPackageNames = setOf(packageName))
            setAppFolders(observeAppFolders().first() + newFolder)
        }
    }

    /** Enforces single-folder membership: adding to [folderId] strips [packageName] out
     * of every other folder first, so an app can never silently belong to two folders at
     * once - see AppFolder's kdoc on this being a ViewModel-level invariant. */
    fun addAppToFolder(
        folderId: String,
        packageName: String,
    ) {
        viewModelScope.launch {
            val updated =
                observeAppFolders().first().map { folder ->
                    if (folder.id == folderId) {
                        folder.copy(memberPackageNames = folder.memberPackageNames + packageName)
                    } else {
                        folder.copy(memberPackageNames = folder.memberPackageNames - packageName)
                    }
                }
            setAppFolders(updated)
        }
    }

    /** Explicit removal that empties a folder deletes it outright - unlike hiding/
     * uninstalling its last member, which is filtered at display time only (see
     * buildDrawerItems) and never mutates storage. */
    fun removeAppFromFolder(
        folderId: String,
        packageName: String,
    ) {
        viewModelScope.launch {
            val updated =
                observeAppFolders().first().mapNotNull { folder ->
                    if (folder.id != folderId) return@mapNotNull folder
                    val remaining = folder.memberPackageNames - packageName
                    remaining.ifEmpty { null }?.let { folder.copy(memberPackageNames = it) }
                }
            setAppFolders(updated)
        }
    }

    fun renameFolder(
        folderId: String,
        newName: String,
    ) {
        viewModelScope.launch {
            val updated =
                observeAppFolders().first().map { folder ->
                    if (folder.id == folderId) folder.copy(name = newName) else folder
                }
            setAppFolders(updated)
        }
    }
}
