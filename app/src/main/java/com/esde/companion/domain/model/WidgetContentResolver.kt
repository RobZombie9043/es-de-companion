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
                ?.let { WidgetContent.SystemLogoAsset(it, widgetType.scaleMode) }
                ?: WidgetContent.Empty

        is WidgetType.SystemMedia ->
            resolveMediaWidgetContent(widgetType.mediaType, widgetType.scaleMode, systemMediaLookup, fallbackBackgroundAssetPath)

        is WidgetType.GameMedia ->
            resolveMediaWidgetContent(widgetType.mediaType, widgetType.scaleMode, gameMediaLookup, fallbackBackgroundAssetPath)

        is WidgetType.ColorBackground ->
            WidgetContent.Color(widgetType.colorArgb, widgetType.alpha)
    }
}