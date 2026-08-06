package com.esde.companion.data.settings

import com.esde.companion.domain.model.MediaType
import com.esde.companion.domain.model.PlacedWidget
import com.esde.companion.domain.model.ScaleMode
import com.esde.companion.domain.model.WidgetType
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetLayoutMappingTest {

    private fun roundTrip(widgetType: WidgetType): WidgetType {
        val placed = PlacedWidget(
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
    fun `raw JSON without a panZoomEnabled key decodes to false, matching pre-existing persisted data`() {
        // kotlinx.serialization's default Json config omits fields at their default value,
        // so a WidgetTypeDto encoded before panZoomEnabled existed - or one encoded today
        // with it left false - never actually contains this key. Decoding must still
        // succeed and default it to false, without a migration step.
        val json = """{"scaleMode":"Fill"}"""
        val decoded = kotlinx.serialization.json.Json.decodeFromString(WidgetTypeDto.SystemImage.serializer(), json)
        assertEquals(WidgetTypeDto.SystemImage(scaleMode = "Fill", panZoomEnabled = false), decoded)
    }
}
