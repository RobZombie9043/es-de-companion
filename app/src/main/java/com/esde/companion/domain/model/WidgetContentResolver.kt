package com.esde.companion.domain.model

object WidgetContentResolver {

    fun resolve(
        widgetType: WidgetType,
        systemLogoAssetPath: () -> String?,
        systemMediaLookup: (MediaType) -> String?,
        gameMediaLookup: (MediaType) -> String?,
        fallbackBackgroundAssetPath: String?,
    ): WidgetContent = when (widgetType) {
        is WidgetType.SystemLogo ->
            systemLogoAssetPath()
                ?.let { WidgetContent.SystemLogoAsset(it, widgetType.scaleMode, widgetType.effects) }
                ?: WidgetContent.Empty

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
    }
}