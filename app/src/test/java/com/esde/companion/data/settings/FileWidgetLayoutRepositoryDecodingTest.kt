package com.esde.companion.data.settings

import android.content.ContextWrapper
import com.esde.companion.domain.model.GridDimensions
import com.esde.companion.domain.model.PlacedWidget
import com.esde.companion.domain.model.WidgetType
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class FileWidgetLayoutRepositoryDecodingTest {
    // decodeCanvas touches nothing on Context - this exists only to satisfy the constructor.
    private class FakeContext : ContextWrapper(null)

    private val repository = FileWidgetLayoutRepository(FakeContext())

    private val widget =
        PlacedWidget(
            id = "widget-1",
            widgetType = WidgetType.ColorBackground(colorArgb = 0xFF000000, alpha = 1f),
            gridColumn = 0,
            gridRow = 0,
            columnSpan = 2,
            rowSpan = 2,
            zIndex = 0,
        )

    @Test
    fun `new-format JSON with a grid object decodes with a non-null grid`() {
        val grid = GridDimensions(columns = 10, rows = 10)
        val json = Json.encodeToString(canvasDtoOf(listOf(widget), grid))

        val decoded = repository.decodeCanvas(json)

        assertEquals(grid, decoded.grid)
        assertEquals(widget, decoded.widgets.single())
    }

    @Test
    fun `legacy bare-array JSON decodes via the fallback path with grid null and widgets mapped`() {
        val json = Json.encodeToString(listOf(widget).toDtoList())

        val decoded = repository.decodeCanvas(json)

        assertNull(decoded.grid)
        assertEquals(widget, decoded.widgets.single())
    }

    @Test
    fun `the legacy bare-array JSON that the fallback exists for does not decode directly as CanvasDto`() {
        // Documents the precondition decodeCanvas's try-catch depends on - if this ever
        // stopped throwing, the fallback branch would silently become dead code.
        val json = Json.encodeToString(listOf(widget).toDtoList())

        assertThrows(SerializationException::class.java) {
            Json.decodeFromString<CanvasDto>(json)
        }
    }
}
