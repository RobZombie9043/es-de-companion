package com.esde.companion.domain.model

/**
 * Reader display settings for the Game Guides viewer. Dark/light mode is deliberately not
 * duplicated here - the viewer follows the app's own [ThemePreference] (see
 * `ui/theme/Theme.kt`'s `LocalIsDarkTheme`) like everything else in the app, rather than a
 * second, independent theme toggle. [reflowEnabled] and [monospaceFont] only affect
 * [GameGuideFormat.PlainText] guides - an [GameGuideFormat.Html] guide's own markup already
 * dictates its layout and font.
 */
data class GameGuideDisplayPreferences(
    val fontScale: Float = 1.0f,
    val reflowEnabled: Boolean = true,
    val monospaceFont: Boolean = true,
)
