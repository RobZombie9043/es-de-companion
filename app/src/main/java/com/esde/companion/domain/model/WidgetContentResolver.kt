package com.esde.companion.domain.model

object WidgetContentResolver {

    fun resolve(
        widgetType: WidgetType,
        systemLogoAssetPath: () -> String?,
        customSystemLogoLookup: () -> String?,
        customSystemImageLookup: () -> String?,
        systemMediaLookup: (MediaType) -> String?,
        gameMediaLookup: (MediaType) -> String?,
        gameDescriptionLookup: () -> String?,
        fallbackBackgroundAssetPath: String?,
    ): WidgetContent = when (widgetType) {
        is WidgetType.SystemLogo ->
            customSystemLogoLookup()
                // crossfade = false, isAsset = false: same "transparent overlay" treatment
                // as the built-in SVG (see WidgetContent.Image's kdoc / CLAUDE.md) - a
                // custom logo is expected to be a similarly transparent-background image,
                // and crossfading briefly double-exposes outgoing/incoming logos.
                ?.let { WidgetContent.Image(it, widgetType.scaleMode, crossfade = false, isAsset = false, effects = widgetType.effects) }
                ?: systemLogoAssetPath()
                    ?.let { WidgetContent.SystemLogoAsset(it, widgetType.scaleMode, widgetType.effects) }
                ?: WidgetContent.Empty

        is WidgetType.SystemImage ->
            customSystemImageLookup()
                ?.let { WidgetContent.Image(it, widgetType.scaleMode, crossfade = true, isAsset = false, effects = widgetType.effects) }
                ?: resolveMediaWidgetContent(
                    mediaType = MediaType.FanArt,
                    scaleMode = widgetType.scaleMode,
                    lookup = systemMediaLookup,
                    fallbackBackgroundAssetPath = null,
                    effects = widgetType.effects,
                )

        is WidgetType.SystemMedia ->
            resolveMediaWidgetContent(
                widgetType.mediaType,
                widgetType.scaleMode,
                systemMediaLookup,
                fallbackBackgroundAssetPath,
                widgetType.effects,
            )

        is WidgetType.GameMedia ->
            resolveMediaWidgetContent(
                widgetType.mediaType,
                widgetType.scaleMode,
                gameMediaLookup,
                fallbackBackgroundAssetPath,
                widgetType.effects,
            )

        is WidgetType.ColorBackground ->
            WidgetContent.Color(widgetType.colorArgb, widgetType.alpha)

        is WidgetType.GameDescription ->
            gameDescriptionLookup()
                ?.let {
                    WidgetContent.Text(
                        text = it,
                        fontSizeSp = widgetType.fontSizeSp,
                        textColorArgb = widgetType.textColorArgb,
                        backgroundColorArgb = widgetType.backgroundColorArgb,
                        backgroundAlpha = widgetType.backgroundAlpha,
                    )
                }
                ?: WidgetContent.Empty
    }
}