package com.esde.companion.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.esde.companion.domain.model.ThemePreference

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
    MaterialTheme(colorScheme = colorScheme, content = content)
}