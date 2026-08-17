package com.esde.companion.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultWidgetLayoutsTest {
    private val grid = GridDimensions(columns = 22, rows = 19)

    @Test
    fun `the default Playing-canvas fanart widget falls back to Screenshots`() {
        val fanartWidget =
            defaultCanvas(StateGroup.Playing, grid)
                .single { it.id == "default-playing-fanart" }
                .widgetType as WidgetType.GameMedia

        assertEquals(MediaType.FanArt, fanartWidget.mediaType)
        assertEquals(MediaType.Screenshots, fanartWidget.fallbackMediaType)
    }
}
