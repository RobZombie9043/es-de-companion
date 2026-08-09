package com.esde.companion.ui.widgets

import androidx.compose.ui.unit.dp
import com.esde.companion.domain.model.GridDimensions
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetGridTest {
    @Test
    fun `an exact multiple of the target cell size divides evenly`() {
        assertEquals(GridDimensions(columns = 10, rows = 5), gridDimensionsFor(240.dp, 120.dp))
    }

    @Test
    fun `a non-exact multiple truncates rather than rounds`() {
        // 100 / 24 = 4.16... -> 4, not 5.
        assertEquals(4, gridDimensionsFor(100.dp, 240.dp).columns)
    }

    @Test
    fun `a dimension smaller than one cell still yields at least 1`() {
        assertEquals(1, gridDimensionsFor(10.dp, 240.dp).columns)
    }

    @Test
    fun `zero width yields 1, never 0`() {
        assertEquals(1, gridDimensionsFor(0.dp, 240.dp).columns)
    }
}
