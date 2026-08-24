package com.esde.companion.data.settings

import com.esde.companion.domain.model.ImageTransitionMode
import com.esde.companion.domain.model.LogoTransitionMode
import com.esde.companion.domain.model.MediaType
import com.esde.companion.domain.model.NoRatingBehavior
import com.esde.companion.domain.model.PillarboxMode
import com.esde.companion.domain.model.PlacedWidget
import com.esde.companion.domain.model.ScaleMode
import com.esde.companion.domain.model.WidgetType
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetLayoutMappingTest {
    private fun placedWidget(widgetType: WidgetType) =
        PlacedWidget(
            id = "widget-1",
            widgetType = widgetType,
            gridColumn = 0,
            gridRow = 0,
            columnSpan = 1,
            rowSpan = 1,
            zIndex = 0,
        )

    private fun roundTrip(widgetType: WidgetType): WidgetType {
        val dto = listOf(placedWidget(widgetType)).toDtoList()
        val roundTripped = dto.toDomainList()
        return roundTripped.single().widgetType
    }

    @Test
    fun `SystemImage round-trips panZoomEnabled true and false`() {
        assertEquals(
            WidgetType.SystemImage(ScaleMode.Fill, panZoomEnabled = true),
            roundTrip(WidgetType.SystemImage(ScaleMode.Fill, panZoomEnabled = true)),
        )
        assertEquals(
            WidgetType.SystemImage(ScaleMode.Fill, panZoomEnabled = false),
            roundTrip(WidgetType.SystemImage(ScaleMode.Fill, panZoomEnabled = false)),
        )
    }

    @Test
    fun `SystemMedia round-trips panZoomEnabled true and false`() {
        assertEquals(
            WidgetType.SystemMedia(MediaType.FanArt, ScaleMode.Fill, panZoomEnabled = true),
            roundTrip(WidgetType.SystemMedia(MediaType.FanArt, ScaleMode.Fill, panZoomEnabled = true)),
        )
        assertEquals(
            WidgetType.SystemMedia(MediaType.FanArt, ScaleMode.Fill, panZoomEnabled = false),
            roundTrip(WidgetType.SystemMedia(MediaType.FanArt, ScaleMode.Fill, panZoomEnabled = false)),
        )
    }

    @Test
    fun `GameMedia round-trips panZoomEnabled true and false`() {
        assertEquals(
            WidgetType.GameMedia(MediaType.Screenshots, ScaleMode.Fill, panZoomEnabled = true),
            roundTrip(WidgetType.GameMedia(MediaType.Screenshots, ScaleMode.Fill, panZoomEnabled = true)),
        )
        assertEquals(
            WidgetType.GameMedia(MediaType.Screenshots, ScaleMode.Fill, panZoomEnabled = false),
            roundTrip(WidgetType.GameMedia(MediaType.Screenshots, ScaleMode.Fill, panZoomEnabled = false)),
        )
    }

    @Test
    fun `CustomImage round-trips panZoomEnabled true and false`() {
        assertEquals(
            WidgetType.CustomImage("path", ScaleMode.Fill, panZoomEnabled = true),
            roundTrip(WidgetType.CustomImage("path", ScaleMode.Fill, panZoomEnabled = true)),
        )
        assertEquals(
            WidgetType.CustomImage("path", ScaleMode.Fill, panZoomEnabled = false),
            roundTrip(WidgetType.CustomImage("path", ScaleMode.Fill, panZoomEnabled = false)),
        )
    }

    @Test
    fun `SystemLogo round-trips logoTransitionMode and glintEnabled`() {
        val widget =
            WidgetType.SystemLogo(
                ScaleMode.Fit,
                logoTransitionMode = LogoTransitionMode.Slide,
                glintEnabled = true,
            )
        assertEquals(widget, roundTrip(widget))
    }

    @Test
    fun `SystemImage round-trips imageTransitionMode`() {
        val widget = WidgetType.SystemImage(ScaleMode.Fill, imageTransitionMode = ImageTransitionMode.Fade)
        assertEquals(widget, roundTrip(widget))
    }

    @Test
    fun `SystemMedia round-trips imageTransitionMode, logoTransitionMode, and glintEnabled`() {
        val widget =
            WidgetType.SystemMedia(
                MediaType.Marquees,
                ScaleMode.Fit,
                imageTransitionMode = ImageTransitionMode.Fade,
                logoTransitionMode = LogoTransitionMode.Scale,
                glintEnabled = true,
            )
        assertEquals(widget, roundTrip(widget))
    }

    @Test
    fun `GameMedia round-trips imageTransitionMode, logoTransitionMode, and glintEnabled`() {
        val widget =
            WidgetType.GameMedia(
                MediaType.Screenshots,
                ScaleMode.Fill,
                imageTransitionMode = ImageTransitionMode.Fade,
                logoTransitionMode = LogoTransitionMode.Slide,
                glintEnabled = true,
            )
        assertEquals(widget, roundTrip(widget))
    }

    @Test
    fun `CustomImage round-trips imageTransitionMode`() {
        val widget = WidgetType.CustomImage("path", ScaleMode.Fill, imageTransitionMode = ImageTransitionMode.Fade)
        assertEquals(widget, roundTrip(widget))
    }

    @Test
    fun `raw JSON without the new transition or glint keys decodes to their defaults`() {
        // Same migration-free reasoning as the panZoomEnabled test below - persisted
        // JSON from before these fields existed (or encoded today with them at their
        // default) never actually contains these keys.
        val json = """{"scaleMode":"Fill"}"""
        val decoded = Json.decodeFromString(WidgetTypeDto.SystemImage.serializer(), json)
        assertEquals(WidgetTypeDto.SystemImage(scaleMode = "Fill", imageTransitionMode = "None"), decoded)
    }

    @Test
    fun `raw JSON without a panZoomEnabled key decodes to false, matching pre-existing persisted data`() {
        // kotlinx.serialization's default Json config omits fields at their default value,
        // so a WidgetTypeDto encoded before panZoomEnabled existed - or one encoded today
        // with it left false - never actually contains this key. Decoding must still
        // succeed and default it to false, without a migration step.
        val json = """{"scaleMode":"Fill"}"""
        val decoded = Json.decodeFromString(WidgetTypeDto.SystemImage.serializer(), json)
        assertEquals(WidgetTypeDto.SystemImage(scaleMode = "Fill", panZoomEnabled = false), decoded)
    }

    @Test
    fun `raw JSON for SystemLogo without optional keys decodes to their defaults`() {
        val json = """{"scaleMode":"Fit"}"""
        val decoded = Json.decodeFromString(WidgetTypeDto.SystemLogo.serializer(), json)
        assertEquals(
            WidgetTypeDto.SystemLogo(
                scaleMode = "Fit",
                blurAmount = 0f,
                darkenAmount = 0f,
                logoTransitionMode = "None",
                glintEnabled = false,
            ),
            decoded,
        )
    }

    @Test
    fun `raw JSON for SystemMedia without optional keys decodes to their defaults`() {
        val json = """{"mediaType":"FanArt","scaleMode":"Fill"}"""
        val decoded = Json.decodeFromString(WidgetTypeDto.SystemMedia.serializer(), json)
        assertEquals(
            WidgetTypeDto.SystemMedia(
                mediaType = "FanArt",
                scaleMode = "Fill",
                blurAmount = 0f,
                darkenAmount = 0f,
                panZoomEnabled = false,
                imageTransitionMode = "None",
                logoTransitionMode = "None",
                glintEnabled = false,
                fallbackMediaType = null,
            ),
            decoded,
        )
    }

    @Test
    fun `raw JSON for GameMedia without optional keys decodes to their defaults`() {
        val json = """{"mediaType":"Screenshots","scaleMode":"Fill"}"""
        val decoded = Json.decodeFromString(WidgetTypeDto.GameMedia.serializer(), json)
        assertEquals(
            WidgetTypeDto.GameMedia(
                mediaType = "Screenshots",
                scaleMode = "Fill",
                blurAmount = 0f,
                darkenAmount = 0f,
                panZoomEnabled = false,
                imageTransitionMode = "None",
                logoTransitionMode = "None",
                glintEnabled = false,
                fallbackMediaType = null,
            ),
            decoded,
        )
    }

    // --- fallbackMediaType: round-trip, explicit None, and old-data migration -------------

    @Test
    fun `SystemMedia round-trips an explicit fallbackMediaType`() {
        val widget = WidgetType.SystemMedia(MediaType.FanArt, ScaleMode.Fill, fallbackMediaType = MediaType.Screenshots)
        assertEquals(widget, roundTrip(widget))
    }

    @Test
    fun `GameMedia round-trips an explicit fallbackMediaType of Covers for ThreeDBoxes`() {
        val widget = WidgetType.GameMedia(MediaType.ThreeDBoxes, ScaleMode.Fit, fallbackMediaType = MediaType.Covers)
        assertEquals(widget, roundTrip(widget))
    }

    @Test
    fun `an explicitly configured None fallback round-trips as None, not the MediaType default`() {
        val widget = WidgetType.GameMedia(MediaType.FanArt, ScaleMode.Fill, fallbackMediaType = null)

        val dto = listOf(placedWidget(widget)).toDtoList().single().widgetType as WidgetTypeDto.GameMedia
        assertEquals("None", dto.fallbackMediaType)
        assertEquals(widget, roundTrip(widget))
    }

    @Test
    fun `raw JSON persisted before fallbackMediaType existed decodes to the MediaType's own default, not None`() {
        // The field is entirely absent, not present-and-null - simulates real pre-feature
        // persisted data, which never had this key at all (see WidgetTypeDto's kdoc for
        // why "absent" and "explicitly None" must decode differently).
        val json = """{"mediaType":"FanArt","scaleMode":"Fill"}"""
        val dto = Json.decodeFromString(WidgetTypeDto.GameMedia.serializer(), json)
        val placed =
            PlacedWidgetDto(
                id = "widget-1",
                widgetType = dto,
                gridColumn = 0,
                gridRow = 0,
                columnSpan = 1,
                rowSpan = 1,
                zIndex = 0,
            )

        val domain = listOf(placed).toDomainList().single().widgetType as WidgetType.GameMedia

        assertEquals(MediaType.Screenshots, domain.fallbackMediaType)
    }

    @Test
    fun `raw JSON persisted before fallbackMediaType existed defaults ThreeDBoxes to Covers`() {
        val json = """{"mediaType":"ThreeDBoxes","scaleMode":"Fit"}"""
        val dto = Json.decodeFromString(WidgetTypeDto.GameMedia.serializer(), json)
        val placed =
            PlacedWidgetDto(
                id = "widget-1",
                widgetType = dto,
                gridColumn = 0,
                gridRow = 0,
                columnSpan = 1,
                rowSpan = 1,
                zIndex = 0,
            )

        val domain = listOf(placed).toDomainList().single().widgetType as WidgetType.GameMedia

        assertEquals(MediaType.Covers, domain.fallbackMediaType)
    }

    @Test
    fun `raw JSON for CustomImage without optional keys decodes to their defaults`() {
        val json = """{"path":"/path/to/image.png","scaleMode":"Fill"}"""
        val decoded = Json.decodeFromString(WidgetTypeDto.CustomImage.serializer(), json)
        assertEquals(
            WidgetTypeDto.CustomImage(
                path = "/path/to/image.png",
                scaleMode = "Fill",
                blurAmount = 0f,
                darkenAmount = 0f,
                panZoomEnabled = false,
                imageTransitionMode = "None",
            ),
            decoded,
        )
    }

    @Test
    fun `raw JSON for GameDescription with no keys decodes to its defaults`() {
        val decoded = Json.decodeFromString(WidgetTypeDto.GameDescription.serializer(), "{}")
        assertEquals(
            WidgetTypeDto.GameDescription(
                fontSizeSp = 16f,
                textColorArgb = 0xFFFFFFFF,
                backgroundColorArgb = 0xFF000000,
                backgroundAlpha = 0.5f,
            ),
            decoded,
        )
    }

    @Test
    fun `Rating round-trips noRatingBehavior and colors`() {
        val widget =
            WidgetType.Rating(
                noRatingBehavior = NoRatingBehavior.ShowEmptyStars,
                filledColorArgb = 0xFFAABBCC,
                outlineColorArgb = 0xFF112233,
                backgroundColorArgb = 0xFF445566,
                backgroundAlpha = 0.25f,
            )
        assertEquals(widget, roundTrip(widget))
    }

    @Test
    fun `raw JSON for Rating with no keys decodes to its defaults`() {
        val decoded = Json.decodeFromString(WidgetTypeDto.Rating.serializer(), "{}")
        assertEquals(
            WidgetTypeDto.Rating(
                noRatingBehavior = "Hide",
                filledColorArgb = 0xFFFFC107,
                outlineColorArgb = 0xFFFFFFFF,
                backgroundColorArgb = 0xFF000000,
                backgroundAlpha = 0.5f,
            ),
            decoded,
        )
    }

    @Test
    fun `Video round-trips scaleMode, audioEnabled, delaySeconds, pillarboxMode, and renderAboveUi`() {
        val widget =
            WidgetType.Video(
                scaleMode = ScaleMode.Fit,
                audioEnabled = false,
                delaySeconds = 5,
                pillarboxMode = PillarboxMode.Transparent,
                renderAboveUi = true,
            )
        assertEquals(widget, roundTrip(widget))
    }

    @Test
    fun `raw JSON for Video without optional keys decodes to their defaults`() {
        val json = """{"scaleMode":"Fill"}"""
        val decoded = Json.decodeFromString(WidgetTypeDto.Video.serializer(), json)
        assertEquals(
            WidgetTypeDto.Video(
                scaleMode = "Fill",
                audioEnabled = true,
                delaySeconds = 0,
                pillarboxMode = "Black",
                renderAboveUi = false,
            ),
            decoded,
        )
    }
}
