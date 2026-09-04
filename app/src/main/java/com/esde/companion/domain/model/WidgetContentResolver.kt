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
        gameRatingLookup: () -> Float? = { null },
        fallbackBackgroundAssetPath: String?,
        systemNameLookup: () -> String? = { null },
        gameNameLookup: () -> String? = { null },
        videoLookup: () -> String? = { null },
        achievementSummaryLookup: () -> AchievementSummaryWidgetState? = { null },
    ): WidgetContent =
        when (widgetType) {
            is WidgetType.SystemLogo ->
                customSystemLogoLookup()
                    // isTransparentOverlay = true, isAsset = false: same "transparent
                    // overlay" treatment as the built-in SVG (see WidgetContent.Image's
                    // kdoc / CLAUDE.md) - a custom logo is expected to be a similarly
                    // transparent-background image, so it uses LogoTransitionMode at
                    // render time rather than a fade.
                    ?.let {
                        WidgetContent.Image(
                            it,
                            widgetType.scaleMode,
                            isTransparentOverlay = true,
                            isAsset = false,
                            effects = widgetType.effects,
                        )
                    }
                    ?: systemLogoAssetPath()
                        ?.let { WidgetContent.SystemLogoAsset(it, widgetType.scaleMode, widgetType.effects) }
                    ?: systemNameLookup()?.let { WidgetContent.NameFallback(it) }
                    ?: WidgetContent.Empty

            is WidgetType.SystemImage ->
                customSystemImageLookup()
                    ?.let {
                        WidgetContent.Image(
                            it,
                            widgetType.scaleMode,
                            isTransparentOverlay = false,
                            isAsset = false,
                            effects = widgetType.effects,
                        )
                    }
                    ?: resolveMediaWidgetContent(
                        mediaType = MediaType.FanArt,
                        scaleMode = widgetType.scaleMode,
                        lookup = systemMediaLookup,
                        effects = widgetType.effects,
                        // No per-widget config here (SystemImage has no mediaType/
                        // Fallback Artwork field of its own) - matches this variant's
                        // documented fixed behavior, see its kdoc.
                        fallback = MediaWidgetFallback(MediaType.Screenshots, fallbackBackgroundAssetPath),
                    )

            is WidgetType.SystemMedia ->
                resolveMediaWidgetContent(
                    mediaType = widgetType.mediaType,
                    scaleMode = widgetType.scaleMode,
                    lookup = systemMediaLookup,
                    effects = widgetType.effects,
                    fallback = MediaWidgetFallback(widgetType.fallbackMediaType, fallbackBackgroundAssetPath),
                ).withNameFallback(widgetType.mediaType, systemNameLookup)

            is WidgetType.GameMedia ->
                resolveMediaWidgetContent(
                    mediaType = widgetType.mediaType,
                    scaleMode = widgetType.scaleMode,
                    lookup = gameMediaLookup,
                    effects = widgetType.effects,
                    fallback = MediaWidgetFallback(widgetType.fallbackMediaType, fallbackBackgroundAssetPath),
                ).withNameFallback(widgetType.mediaType, gameNameLookup)

            is WidgetType.CustomImage ->
                if (widgetType.path.isNotBlank()) {
                    WidgetContent.Image(
                        widgetType.path,
                        widgetType.scaleMode,
                        isTransparentOverlay = false,
                        isAsset = false,
                        effects = widgetType.effects,
                    )
                } else {
                    WidgetContent.Empty
                }

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

            is WidgetType.Rating -> {
                val starCount = gameRatingLookup()?.let { (it * MAX_STARS).coerceIn(0f, MAX_STARS) }
                if (starCount != null) {
                    WidgetContent.Rating(
                        starCount = starCount,
                        filledColorArgb = widgetType.filledColorArgb,
                        outlineColorArgb = widgetType.outlineColorArgb,
                        backgroundColorArgb = widgetType.backgroundColorArgb,
                        backgroundAlpha = widgetType.backgroundAlpha,
                    )
                } else if (widgetType.noRatingBehavior == NoRatingBehavior.ShowEmptyStars) {
                    WidgetContent.Rating(
                        starCount = 0f,
                        filledColorArgb = widgetType.filledColorArgb,
                        outlineColorArgb = widgetType.outlineColorArgb,
                        backgroundColorArgb = widgetType.backgroundColorArgb,
                        backgroundAlpha = widgetType.backgroundAlpha,
                    )
                } else {
                    WidgetContent.Empty
                }
            }

            is WidgetType.Video ->
                videoLookup()?.let {
                    WidgetContent.Video(
                        path = it,
                        scaleMode = widgetType.scaleMode,
                        audioEnabled = widgetType.audioEnabled,
                        delaySeconds = widgetType.delaySeconds,
                        pillarboxMode = widgetType.pillarboxMode,
                        renderAboveUi = widgetType.renderAboveUi,
                        loopEnabled = widgetType.loopEnabled,
                        cornerRadius = widgetType.cornerRadius,
                    )
                } ?: WidgetContent.Empty

            is WidgetType.AchievementSummary ->
                achievementSummaryLookup()?.let { state ->
                    WidgetContent.AchievementSummary(
                        state = state,
                        fontSizeSp = widgetType.fontSizeSp,
                        textColorArgb = widgetType.textColorArgb,
                        backgroundColorArgb = widgetType.backgroundColorArgb,
                        backgroundAlpha = widgetType.backgroundAlpha,
                        cornerRadius = widgetType.cornerRadius,
                    )
                } ?: WidgetContent.Empty
        }

    /** A gamelist.xml <rating> is a 0f..1f score - the widget always renders on a 5-star
     * scale. */
    private const val MAX_STARS = 5f

    /** Marquees have no generic-background fallback (see [BACKGROUND_FALLBACK_ELIGIBLE]),
     * so a missing marquee resolves all the way to [WidgetContent.Empty] - this catches
     * that specific case and substitutes the system/game's display name instead, same as
     * [WidgetType.SystemLogo] does for a missing logo. Any other media type is returned
     * as-is. */
    private fun WidgetContent.withNameFallback(
        mediaType: MediaType,
        nameLookup: () -> String?,
    ): WidgetContent =
        if (this == WidgetContent.Empty && mediaType == MediaType.Marquees) {
            nameLookup()?.let { WidgetContent.NameFallback(it) } ?: this
        } else {
            this
        }
}
