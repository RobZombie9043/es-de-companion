package com.esde.companion.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.esde.companion.domain.model.ThemePreference

/**
 * Whether the app is currently rendering in dark mode - the same resolved value
 * [EsdeCompanionTheme] uses to pick a color scheme, exposed for the few places outside
 * Material3 theming that also need it (e.g. picking a light/dark variant of a bundled
 * fallback image asset - see WidgetCanvas.kt/WidgetOverlay.kt). Defaults to true (the
 * pre-existing single dark asset) so any composable rendered without EsdeCompanionTheme in
 * its ancestry - previews, tests - doesn't need to provide this explicitly.
 */
val LocalIsDarkTheme = staticCompositionLocalOf { true }

@Composable
fun EsdeCompanionTheme(
    themePreference: ThemePreference = ThemePreference.Auto,
    content: @Composable () -> Unit,
) {
    val isDark = when (themePreference) {
        ThemePreference.Auto -> isSystemInDarkTheme()
        ThemePreference.Light -> false
        ThemePreference.Dark -> true
    }
    val colorScheme = if (isDark) darkColorScheme() else lightColorScheme()
    CompositionLocalProvider(LocalIsDarkTheme provides isDark) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}