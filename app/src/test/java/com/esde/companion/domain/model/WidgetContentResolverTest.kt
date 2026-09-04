package com.esde.companion.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetContentResolverTest {
    private fun resolve(
        widgetType: WidgetType,
        systemLogoAssetPath: () -> String? = { null },
        customSystemLogoLookup: () -> String? = { null },
        customSystemImageLookup: () -> String? = { null },
        systemMediaLookup: (MediaType) -> String? = { null },
        gameMediaLookup: (MediaType) -> String? = { null },
        gameDescriptionLookup: () -> String? = { null },
        gameRatingLookup: () -> Float? = { null },
        fallbackBackgroundAssetPath: String? = "fallback.webp",
        systemNameLookup: () -> String? = { null },
        gameNameLookup: () -> String? = { null },
        videoLookup: () -> String? = { null },
        achievementSummaryLookup: () -> AchievementSummaryWidgetState? = { null },
    ): WidgetContent =
        WidgetContentResolver.resolve(
            widgetType = widgetType,
            systemLogoAssetPath = systemLogoAssetPath,
            customSystemLogoLookup = customSystemLogoLookup,
            customSystemImageLookup = customSystemImageLookup,
            systemMediaLookup = systemMediaLookup,
            gameMediaLookup = gameMediaLookup,
            gameDescriptionLookup = gameDescriptionLookup,
            gameRatingLookup = gameRatingLookup,
            fallbackBackgroundAssetPath = fallbackBackgroundAssetPath,
            systemNameLookup = systemNameLookup,
            gameNameLookup = gameNameLookup,
            videoLookup = videoLookup,
            achievementSummaryLookup = achievementSummaryLookup,
        )

    // --- SystemImage: generic background fallback ---------------------------------------

    @Test
    fun `SystemImage falls back to the generic background when no FanArt or Screenshot is found`() {
        val content =
            resolve(
                widgetType = WidgetType.SystemImage(ScaleMode.Fill),
                fallbackBackgroundAssetPath = "fallback.webp",
            )

        assertEquals(
            WidgetContent.Image("fallback.webp", ScaleMode.Fill, isTransparentOverlay = false, isAsset = true),
            content,
        )
    }

    @Test
    fun `SystemImage resolves to Empty when no fallback path is supplied (edit-mode preview)`() {
        val content =
            resolve(
                widgetType = WidgetType.SystemImage(ScaleMode.Fill),
                fallbackBackgroundAssetPath = null,
            )

        assertEquals(WidgetContent.Empty, content)
    }

    @Test
    fun `SystemImage prefers a real FanArt over the generic fallback`() {
        val content =
            resolve(
                widgetType = WidgetType.SystemImage(ScaleMode.Fill),
                systemMediaLookup = { mediaType -> if (mediaType == MediaType.FanArt) "/media/fanart.png" else null },
                fallbackBackgroundAssetPath = "fallback.webp",
            )

        assertEquals(
            WidgetContent.Image("/media/fanart.png", ScaleMode.Fill, isTransparentOverlay = false, isAsset = false),
            content,
        )
    }

    // --- SystemMedia/GameMedia: unaffected background-fallback behavior ------------------

    @Test
    fun `SystemMedia FanArt still falls back to the generic background when nothing is found`() {
        val content =
            resolve(
                widgetType = WidgetType.SystemMedia(MediaType.FanArt, ScaleMode.Fill),
                fallbackBackgroundAssetPath = "fallback.webp",
            )

        assertEquals(
            WidgetContent.Image("fallback.webp", ScaleMode.Fill, isTransparentOverlay = false, isAsset = true),
            content,
        )
    }

    @Test
    fun `GameMedia Screenshots still falls back to the generic background when nothing is found`() {
        val content =
            resolve(
                widgetType = WidgetType.GameMedia(MediaType.Screenshots, ScaleMode.Fill),
                fallbackBackgroundAssetPath = "fallback.webp",
            )

        assertEquals(
            WidgetContent.Image("fallback.webp", ScaleMode.Fill, isTransparentOverlay = false, isAsset = true),
            content,
        )
    }

    @Test
    fun `SystemImage falls back to a Screenshot when no FanArt is found (fixed, non-configurable)`() {
        val screenshotOnly: (MediaType) -> String? = {
            if (it == MediaType.Screenshots) "/media/screenshot.png" else null
        }
        val content =
            resolve(
                widgetType = WidgetType.SystemImage(ScaleMode.Fill),
                systemMediaLookup = screenshotOnly,
                fallbackBackgroundAssetPath = "fallback.webp",
            )

        assertEquals(
            WidgetContent.Image("/media/screenshot.png", ScaleMode.Fill, isTransparentOverlay = false, isAsset = false),
            content,
        )
    }

    // --- SystemMedia/GameMedia: configurable Fallback Artwork ------------------------------

    @Test
    fun `SystemMedia FanArt falls back to its configured fallback when FanArt is missing`() {
        val screenshotOnly: (MediaType) -> String? = {
            if (it == MediaType.Screenshots) "/media/screenshot.png" else null
        }
        val content =
            resolve(
                widgetType = WidgetType.SystemMedia(MediaType.FanArt, ScaleMode.Fill),
                systemMediaLookup = screenshotOnly,
            )

        assertEquals(
            WidgetContent.Image("/media/screenshot.png", ScaleMode.Fill, isTransparentOverlay = false, isAsset = false),
            content,
        )
    }

    @Test
    fun `GameMedia Screenshots falls back to its configured fallback when Screenshots is missing`() {
        val fanArtOnly: (MediaType) -> String? = { if (it == MediaType.FanArt) "/media/fanart.png" else null }
        val content =
            resolve(
                widgetType = WidgetType.GameMedia(MediaType.Screenshots, ScaleMode.Fill),
                gameMediaLookup = fanArtOnly,
            )

        assertEquals(
            WidgetContent.Image("/media/fanart.png", ScaleMode.Fill, isTransparentOverlay = false, isAsset = false),
            content,
        )
    }

    @Test
    fun `GameMedia ThreeDBoxes falls back to Box Cover by default when ThreeDBoxes is missing`() {
        val coverOnly: (MediaType) -> String? = { if (it == MediaType.Covers) "/media/cover.png" else null }
        val content =
            resolve(
                widgetType = WidgetType.GameMedia(MediaType.ThreeDBoxes, ScaleMode.Fit),
                gameMediaLookup = coverOnly,
            )

        assertEquals(
            WidgetContent.Image("/media/cover.png", ScaleMode.Fit, isTransparentOverlay = false, isAsset = false),
            content,
        )
    }

    @Test
    fun `explicitly configuring None disables the fallback even when the fallback media is available`() {
        val content =
            resolve(
                widgetType = WidgetType.GameMedia(MediaType.ThreeDBoxes, ScaleMode.Fit, fallbackMediaType = null),
                gameMediaLookup = { mediaType -> if (mediaType == MediaType.Covers) "/media/cover.png" else null },
            )

        assertEquals(WidgetContent.Empty, content)
    }

    @Test
    fun `prefers the primary media type over its configured fallback when both are available`() {
        val content =
            resolve(
                widgetType = WidgetType.SystemMedia(MediaType.FanArt, ScaleMode.Fill),
                systemMediaLookup = { mediaType ->
                    when (mediaType) {
                        MediaType.FanArt -> "/media/fanart.png"
                        MediaType.Screenshots -> "/media/screenshot.png"
                        else -> null
                    }
                },
            )

        assertEquals(
            WidgetContent.Image("/media/fanart.png", ScaleMode.Fill, isTransparentOverlay = false, isAsset = false),
            content,
        )
    }

    // --- SystemLogo: name-text fallback ---------------------------------------------------

    @Test
    fun `SystemLogo falls back to the system's display name when no logo is available`() {
        val content =
            resolve(
                widgetType = WidgetType.SystemLogo(ScaleMode.Fit),
                systemNameLookup = { "Sega Dreamcast" },
            )

        assertEquals(WidgetContent.NameFallback("Sega Dreamcast"), content)
    }

    @Test
    fun `SystemLogo resolves to Empty when no logo and no system name are available`() {
        val content = resolve(widgetType = WidgetType.SystemLogo(ScaleMode.Fit))

        assertEquals(WidgetContent.Empty, content)
    }

    @Test
    fun `SystemLogo prefers the built-in asset over the name fallback`() {
        val content =
            resolve(
                widgetType = WidgetType.SystemLogo(ScaleMode.Fit),
                systemLogoAssetPath = { "file:///android_asset/system_logos/dreamcast.svg" },
                systemNameLookup = { "Sega Dreamcast" },
            )

        assertEquals(
            WidgetContent.SystemLogoAsset("file:///android_asset/system_logos/dreamcast.svg", ScaleMode.Fit),
            content,
        )
    }

    // --- CustomImage: fixed user-picked path, independent of any lookup -------------------

    @Test
    fun `CustomImage resolves to the widget's own fixed path`() {
        val content =
            resolve(
                widgetType =
                    WidgetType.CustomImage(
                        path = "/storage/emulated/0/Pictures/cover.jpg",
                        scaleMode = ScaleMode.Fill,
                    ),
            )

        assertEquals(
            WidgetContent.Image(
                "/storage/emulated/0/Pictures/cover.jpg",
                ScaleMode.Fill,
                isTransparentOverlay = false,
                isAsset = false,
            ),
            content,
        )
    }

    @Test
    fun `CustomImage with no path chosen yet resolves to Empty`() {
        val content = resolve(widgetType = WidgetType.CustomImage(path = "", scaleMode = ScaleMode.Fill))

        assertEquals(WidgetContent.Empty, content)
    }

    // --- Marquee: name-text fallback ------------------------------------------------------

    @Test
    fun `GameMedia Marquees falls back to the game's display name when no marquee is available`() {
        val content =
            resolve(
                widgetType = WidgetType.GameMedia(MediaType.Marquees, ScaleMode.Fit),
                gameNameLookup = { "Crazy Taxi" },
            )

        assertEquals(WidgetContent.NameFallback("Crazy Taxi"), content)
    }

    @Test
    fun `SystemMedia Marquees falls back to the system's display name when no marquee is available`() {
        val content =
            resolve(
                widgetType = WidgetType.SystemMedia(MediaType.Marquees, ScaleMode.Fit),
                systemNameLookup = { "Sega Dreamcast" },
            )

        assertEquals(WidgetContent.NameFallback("Sega Dreamcast"), content)
    }

    @Test
    fun `GameMedia Marquees resolves to Empty when no marquee and no game name are available`() {
        val content = resolve(widgetType = WidgetType.GameMedia(MediaType.Marquees, ScaleMode.Fit))

        assertEquals(WidgetContent.Empty, content)
    }

    @Test
    fun `GameMedia Marquees prefers a real marquee over the name fallback`() {
        val content =
            resolve(
                widgetType = WidgetType.GameMedia(MediaType.Marquees, ScaleMode.Fit),
                gameMediaLookup = { mediaType -> if (mediaType == MediaType.Marquees) "/media/marquee.png" else null },
                gameNameLookup = { "Crazy Taxi" },
            )

        assertEquals(
            WidgetContent.Image("/media/marquee.png", ScaleMode.Fit, isTransparentOverlay = true, isAsset = false),
            content,
        )
    }

    // --- Rating: 0f..1f -> 0f..5f star conversion and no-rating behavior ------------------

    @Test
    fun `Rating converts a 0f to 1f gamelist value onto a 0f to 5f star scale`() {
        val content =
            resolve(
                widgetType = WidgetType.Rating(),
                gameRatingLookup = { 0.8f },
            )

        assertEquals(
            WidgetContent.Rating(
                starCount = 4f,
                filledColorArgb = 0xFFFFC107,
                outlineColorArgb = 0xFFFFFFFF,
                backgroundColorArgb = 0xFF000000,
                backgroundAlpha = 0.5f,
            ),
            content,
        )
    }

    @Test
    fun `Rating with Hide behavior resolves to Empty when the game has no rating`() {
        val content =
            resolve(
                widgetType = WidgetType.Rating(noRatingBehavior = NoRatingBehavior.Hide),
                gameRatingLookup = { null },
            )

        assertEquals(WidgetContent.Empty, content)
    }

    @Test
    fun `Rating with ShowEmptyStars behavior resolves to zero filled stars when the game has no rating`() {
        val content =
            resolve(
                widgetType = WidgetType.Rating(noRatingBehavior = NoRatingBehavior.ShowEmptyStars),
                gameRatingLookup = { null },
            )

        assertEquals(
            WidgetContent.Rating(
                starCount = 0f,
                filledColorArgb = 0xFFFFC107,
                outlineColorArgb = 0xFFFFFFFF,
                backgroundColorArgb = 0xFF000000,
                backgroundAlpha = 0.5f,
            ),
            content,
        )
    }

    @Test
    fun `Rating of exactly 0 always renders as zero filled stars regardless of noRatingBehavior`() {
        val content =
            resolve(
                widgetType = WidgetType.Rating(noRatingBehavior = NoRatingBehavior.Hide),
                gameRatingLookup = { 0f },
            )

        assertEquals(
            WidgetContent.Rating(
                starCount = 0f,
                filledColorArgb = 0xFFFFC107,
                outlineColorArgb = 0xFFFFFFFF,
                backgroundColorArgb = 0xFF000000,
                backgroundAlpha = 0.5f,
            ),
            content,
        )
    }

    @Test
    fun `Rating threads through the widget's own configured colors`() {
        val widgetType =
            WidgetType.Rating(
                filledColorArgb = 0xFFAABBCC,
                outlineColorArgb = 0xFF112233,
                backgroundColorArgb = 0xFF445566,
                backgroundAlpha = 0.25f,
            )
        val content = resolve(widgetType = widgetType, gameRatingLookup = { 1f })

        assertEquals(
            WidgetContent.Rating(
                starCount = 5f,
                filledColorArgb = 0xFFAABBCC,
                outlineColorArgb = 0xFF112233,
                backgroundColorArgb = 0xFF445566,
                backgroundAlpha = 0.25f,
            ),
            content,
        )
    }

    // --- Video: resolves only when the caller supplies a path (eligibility gate lives
    // entirely in the caller's videoLookup, not in the resolver itself) ------------------

    @Test
    fun `Video resolves to WidgetContent Video when videoLookup returns a path`() {
        val widgetType =
            WidgetType.Video(
                scaleMode = ScaleMode.Fit,
                audioEnabled = false,
                delaySeconds = 5,
                pillarboxMode = PillarboxMode.Transparent,
                renderAboveUi = true,
                loopEnabled = false,
                cornerRadius = CornerRadius.Medium,
            )
        val content = resolve(widgetType = widgetType, videoLookup = { "/media/video.mp4" })

        assertEquals(
            WidgetContent.Video(
                path = "/media/video.mp4",
                scaleMode = ScaleMode.Fit,
                audioEnabled = false,
                delaySeconds = 5,
                pillarboxMode = PillarboxMode.Transparent,
                renderAboveUi = true,
                loopEnabled = false,
                cornerRadius = CornerRadius.Medium,
            ),
            content,
        )
    }

    @Test
    fun `Video resolves to Empty when videoLookup returns null (not eligible or no video found)`() {
        val content = resolve(widgetType = WidgetType.Video(), videoLookup = { null })

        assertEquals(WidgetContent.Empty, content)
    }

    @Test
    fun `Video resolves to Empty by default when the caller supplies no videoLookup at all`() {
        val content = resolve(widgetType = WidgetType.Video())

        assertEquals(WidgetContent.Empty, content)
    }

    // --- AchievementSummary: same "eligibility lives in the caller's lookup" shape as Video -
    // the resolver only ever passes the state through unchanged, attaching style fields; the
    // Loading/Unavailable/Loaded decision itself is tested at WidgetsViewModel/EditWidgetsViewModel
    // level, where it's actually computed.

    private fun loadedAchievementSummaryState(
        unlocked: Int = 3,
        total: Int = 10,
        isRefreshing: Boolean = false,
    ) = AchievementSummaryWidgetState.Loaded(
        unlockedCount = unlocked,
        totalCount = total,
        earnedPoints = 150,
        totalPoints = 500,
        completionPercent = 30f,
        isRefreshing = isRefreshing,
    )

    @Test
    fun `AchievementSummary resolves to WidgetContent AchievementSummary when the lookup returns a state`() {
        val widgetType =
            WidgetType.AchievementSummary(
                textColorArgb = 0xFF00FF00,
                backgroundColorArgb = 0xFF111111,
                backgroundAlpha = 0.6f,
                cornerRadius = CornerRadius.Small,
            )
        val state = loadedAchievementSummaryState(unlocked = 3, total = 10)

        val content = resolve(widgetType = widgetType, achievementSummaryLookup = { state })

        assertEquals(
            WidgetContent.AchievementSummary(
                state = state,
                textColorArgb = 0xFF00FF00,
                backgroundColorArgb = 0xFF111111,
                backgroundAlpha = 0.6f,
                cornerRadius = CornerRadius.Small,
            ),
            content,
        )
    }

    @Test
    fun `AchievementSummary passes a Loading state through unchanged`() {
        val content =
            resolve(
                widgetType = WidgetType.AchievementSummary(),
                achievementSummaryLookup = { AchievementSummaryWidgetState.Loading },
            )

        assertEquals(AchievementSummaryWidgetState.Loading, (content as WidgetContent.AchievementSummary).state)
    }

    @Test
    fun `AchievementSummary passes an Unavailable state through unchanged`() {
        val content =
            resolve(
                widgetType = WidgetType.AchievementSummary(),
                achievementSummaryLookup = { AchievementSummaryWidgetState.Unavailable },
            )

        assertEquals(AchievementSummaryWidgetState.Unavailable, (content as WidgetContent.AchievementSummary).state)
    }

    @Test
    fun `AchievementSummary resolves to Empty when the lookup returns null (not eligible at all)`() {
        val content = resolve(widgetType = WidgetType.AchievementSummary(), achievementSummaryLookup = { null })

        assertEquals(WidgetContent.Empty, content)
    }

    @Test
    fun `AchievementSummary resolves to Empty by default when no achievementSummaryLookup is supplied`() {
        val content = resolve(widgetType = WidgetType.AchievementSummary())

        assertEquals(WidgetContent.Empty, content)
    }
}
