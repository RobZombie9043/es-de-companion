package com.esde.companion.domain.usecase

import app.cash.turbine.test
import com.esde.companion.domain.model.GridDimensions
import com.esde.companion.domain.model.PlacedWidget
import com.esde.companion.domain.model.SavedWidgetCanvas
import com.esde.companion.domain.model.StateGroup
import com.esde.companion.domain.model.WidgetType
import com.esde.companion.domain.model.defaultCanvas
import com.esde.companion.domain.repository.WidgetLayoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetLayoutUseCasesTest {
    private class FakeWidgetLayoutRepository(
        seeded: SavedWidgetCanvas = SavedWidgetCanvas(grid = null, widgets = emptyList()),
    ) : WidgetLayoutRepository {
        private val canvas = MutableStateFlow(seeded)

        override fun observeCanvas(stateGroup: StateGroup): Flow<SavedWidgetCanvas> = canvas

        override suspend fun saveCanvas(
            stateGroup: StateGroup,
            widgets: List<PlacedWidget>,
            grid: GridDimensions,
        ) {
            canvas.value = SavedWidgetCanvas(grid, widgets)
        }
    }

    private fun placedWidget(
        id: String,
        gridColumn: Int,
        gridRow: Int,
        columnSpan: Int,
        rowSpan: Int,
    ) = PlacedWidget(
        id = id,
        widgetType = WidgetType.ColorBackground(colorArgb = 0xFF000000, alpha = 1f),
        gridColumn = gridColumn,
        gridRow = gridRow,
        columnSpan = columnSpan,
        rowSpan = rowSpan,
        zIndex = 0,
    )

    @Test
    fun `an empty saved canvas returns the default canvas for that StateGroup and grid`() =
        runTest {
            val grid = GridDimensions(columns = 10, rows = 10)
            val repository = FakeWidgetLayoutRepository()
            val useCase = ObserveWidgetCanvasUseCase(repository)

            useCase(StateGroup.System, grid).test {
                assertEquals(defaultCanvas(StateGroup.System, grid), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `widgets saved without a tagged grid are returned unchanged`() =
        runTest {
            val widgets = listOf(placedWidget("widget-a", gridColumn = 5, gridRow = 0, columnSpan = 5, rowSpan = 10))
            val repository = FakeWidgetLayoutRepository(SavedWidgetCanvas(grid = null, widgets = widgets))
            val useCase = ObserveWidgetCanvasUseCase(repository)

            useCase(StateGroup.System, GridDimensions(columns = 20, rows = 10)).test {
                assertEquals(widgets, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `widgets saved under the same grid as requested are returned unchanged`() =
        runTest {
            val grid = GridDimensions(columns = 10, rows = 10)
            val widgets = listOf(placedWidget("widget-a", gridColumn = 5, gridRow = 0, columnSpan = 5, rowSpan = 10))
            val repository = FakeWidgetLayoutRepository(SavedWidgetCanvas(grid = grid, widgets = widgets))
            val useCase = ObserveWidgetCanvasUseCase(repository)

            useCase(StateGroup.System, grid).test {
                assertEquals(widgets, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `widgets saved under a different grid are rescaled to fit the requested grid`() =
        runTest {
            // Same fixture as EditWidgetsViewModelTest's grid-rescaling test, to keep the
            // expected result grounded in an already-verified rescale computation rather
            // than re-deriving new math here.
            val savedGrid = GridDimensions(columns = 10, rows = 10)
            val requestedGrid = GridDimensions(columns = 20, rows = 10)
            val widgets = listOf(placedWidget("widget-a", gridColumn = 5, gridRow = 0, columnSpan = 5, rowSpan = 10))
            val repository = FakeWidgetLayoutRepository(SavedWidgetCanvas(grid = savedGrid, widgets = widgets))
            val useCase = ObserveWidgetCanvasUseCase(repository)

            useCase(StateGroup.System, requestedGrid).test {
                val rescaled = awaitItem().single()
                assertEquals(10, rescaled.gridColumn)
                assertEquals(10, rescaled.columnSpan)
                assertEquals(0, rescaled.gridRow)
                assertEquals(10, rescaled.rowSpan)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
