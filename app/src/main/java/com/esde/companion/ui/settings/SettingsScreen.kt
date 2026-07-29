package com.esde.companion.ui.settings

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.esde.companion.data.storage.AllFilesAccessPermission
import com.esde.companion.data.storage.SafPathResolver
import com.esde.companion.domain.model.LogFolderValidation
import com.esde.companion.domain.model.MediaFolderValidation
import com.esde.companion.domain.model.ThemePreference

/**
 * Shared corner radius for the background panel behind every settings item (category
 * rows, folder settings, toggles, the theme picker) so they read as a consistent set of
 * cards rather than each item picking its own rounding.
 */
private val SettingsItemShape = RoundedCornerShape(16.dp)

/**
 * Settings entry point: shows a top-level list of [SettingsCategory] items, and drills
 * into a subpage for whichever one is selected. `selectedCategory == null` means the
 * category list is showing; back from a subpage returns to the list, back from the list
 * calls [onDone]. See [SettingsCategory] for why this is plain state rather than a nested
 * NavHost.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onDone: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val currentOnRefresh = rememberUpdatedState(viewModel::refreshPermissionState)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                currentOnRefresh.value(AllFilesAccessPermission.isGranted())
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var selectedCategory by rememberSaveable { mutableStateOf<SettingsCategory?>(null) }
    val onBack: () -> Unit = { if (selectedCategory != null) selectedCategory = null else onDone() }

    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedCategory?.title ?: "Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background,
        ) {
            AnimatedContent(
                targetState = selectedCategory,
                transitionSpec = {
                    // Drilling into a subpage slides in from the right; returning to the
                    // list slides in from the left - mirrors typical drill-down navigation
                    // even though this isn't nav-compose under the hood.
                    val enteringSubpage = targetState != null
                    val slideDistance = { width: Int -> width / 3 }
                    if (enteringSubpage) {
                        (slideInHorizontally(tween(220), slideDistance) + fadeIn(tween(220)))
                            .togetherWith(slideOutHorizontally(tween(220)) { -slideDistance(it) } + fadeOut(tween(150)))
                    } else {
                        (slideInHorizontally(tween(220)) { -slideDistance(it) } + fadeIn(tween(220)))
                            .togetherWith(slideOutHorizontally(tween(220), slideDistance) + fadeOut(tween(150)))
                    }
                },
                label = "settingsContent",
            ) { category ->
                when (category) {
                    null -> SettingsCategoryList(onCategorySelected = { selectedCategory = it })
                    SettingsCategory.Setup -> SetupSettingsContent(
                        uiState = uiState,
                        onLogFolderPicked = viewModel::onLogFolderPicked,
                        onMediaFolderPicked = viewModel::onMediaFolderPicked,
                    )
                    SettingsCategory.UI -> UISettingsContent(
                        themePreference = uiState.themePreference,
                        onThemePreferenceChanged = viewModel::onThemePreferenceChanged,
                    )
                    SettingsCategory.Other -> OtherSettingsContent(
                        overlayEnabled = uiState.overlayEnabled,
                        onOverlayEnabledChanged = viewModel::onOverlayEnabledChanged,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCategoryList(onCategorySelected: (SettingsCategory) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SettingsCategory.entries.forEach { category ->
            SettingsCategoryRow(category = category, onClick = { onCategorySelected(category) })
        }
    }
}

@Composable
private fun SettingsCategoryRow(category: SettingsCategory, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(text = category.description, style = MaterialTheme.typography.bodySmall)
            }
            Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun SetupSettingsContent(
    uiState: SettingsUiState,
    onLogFolderPicked: (String) -> Unit,
    onMediaFolderPicked: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        if (!uiState.permissionGranted) {
            Text(
                "All files access isn't currently granted - folder changes below " +
                        "may not take effect until it's re-enabled in system Settings.",
            )
        }

        FolderSetting(
            label = "ES-DE folder",
            path = uiState.logFolderPath,
            isValidating = uiState.isValidatingLogFolder,
            statusText = uiState.logFolderValidation.toStatusText(),
            onPick = { uri -> SafPathResolver.resolvePath(uri)?.let(onLogFolderPicked) },
        )

        FolderSetting(
            label = "Media folder",
            path = uiState.mediaFolderPath,
            isValidating = uiState.isValidatingMediaFolder,
            statusText = uiState.mediaFolderValidation.toStatusText(),
            onPick = { uri -> SafPathResolver.resolvePath(uri)?.let(onMediaFolderPicked) },
        )
    }
}

@Composable
private fun UISettingsContent(
    themePreference: ThemePreference,
    onThemePreferenceChanged: (ThemePreference) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        ThemePicker(selected = themePreference, onSelected = onThemePreferenceChanged)
    }
}

@Composable
private fun OtherSettingsContent(
    overlayEnabled: Boolean,
    onOverlayEnabledChanged: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        OverlayToggle(
            enabled = overlayEnabled,
            onEnabledChange = onOverlayEnabledChanged,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemePicker(selected: ThemePreference, onSelected: (ThemePreference) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ThemePreference.entries.forEachIndexed { index, theme ->
                    SegmentedButton(
                        selected = theme == selected,
                        onClick = { onSelected(theme) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ThemePreference.entries.size,
                        ),
                        icon = {
                            SegmentedButtonDefaults.Icon(active = theme == selected) {
                                Icon(
                                    imageVector = theme.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                                )
                            }
                        },
                        label = { Text(theme.label) },
                    )
                }
            }
        }
    }
}

// Presentation-only icon/label, kept in the UI layer rather than on the enum itself so
// ThemePreference stays a plain domain identifier with no display concerns.
private val ThemePreference.icon: ImageVector
    get() = when (this) {
        ThemePreference.Auto -> Icons.Filled.BrightnessAuto
        ThemePreference.Light -> Icons.Filled.LightMode
        ThemePreference.Dark -> Icons.Filled.DarkMode
    }

private val ThemePreference.label: String
    get() = when (this) {
        ThemePreference.Auto -> "Auto"
        ThemePreference.Light -> "Light"
        ThemePreference.Dark -> "Dark"
    }

@Composable
private fun OverlayToggle(enabled: Boolean, onEnabledChange: (Boolean) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Debug overlay",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Show debug info overlay",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }
        }
    }
}

@Composable
private fun FolderSetting(
    label: String,
    path: String,
    isValidating: Boolean,
    statusText: String,
    onPick: (Uri) -> Unit,
) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let(onPick)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(text = path, style = MaterialTheme.typography.bodyMedium)
            if (isValidating) {
                CircularProgressIndicator()
            } else {
                Text(text = statusText, style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = { launcher.launch(null) }) {
                Text("Change folder")
            }
        }
    }
}

private fun LogFolderValidation?.toStatusText(): String = when (this) {
    null -> ""
    is LogFolderValidation.FolderNotFound -> "Folder not found"
    is LogFolderValidation.FolderFound -> if (logFileFound) "es_log.txt found" else "Folder found, but es_log.txt is missing"
}

private fun MediaFolderValidation?.toStatusText(): String = when (this) {
    null -> ""
    is MediaFolderValidation.FolderNotFound -> "Folder not found"
    is MediaFolderValidation.FolderFound -> "Folder found"
}