package com.esde.companion.data.settings

import com.esde.companion.domain.model.ImageTransitionMode
import com.esde.companion.domain.model.LogoTransitionMode
import com.esde.companion.domain.model.MediaType
import com.esde.companion.domain.model.PlacedWidget
import com.esde.companion.domain.model.ScaleMode
import com.esde.companion.domain.model.WidgetType
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetLayoutMappingTest {
    private fun roundTrip(widgetType: WidgetType): WidgetType {
        val placed =
            PlacedWidget(
                id = "widget-1",
                widgetType = widgetType,
                gridColumn = 0,
                gridRow = 0,
                columnSpan = 1,
                rowSpan = 1,
                zIndex = 0,
            )
        val dto = listOf(placed).toDtoList()
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
        val widget = WidgetType.SystemLogo(ScaleMode.Fit, logoTransitionMode = LogoTransitionMode.Slide, glintEnabled = true)
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
            ),
            decoded,
        )
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
}
