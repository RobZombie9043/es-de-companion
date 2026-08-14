package com.esde.companion.ui.widgets

import com.esde.companion.domain.model.PlacedWidget
import com.esde.companion.domain.model.ScaleMode
import com.esde.companion.domain.model.WidgetContent
import com.esde.companion.domain.model.WidgetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class CanvasBackdropTest {
    private fun imageWidget(
        id: String,
        columnSpan: Int = 4,
        rowSpan: Int = 4,
        zIndex: Int = 0,
        widgetType: WidgetType = WidgetType.SystemImage(scaleMode = ScaleMode.Fill),
    ) = PlacedWidget(
        id,
        widgetType,
        gridColumn = 0,
        gridRow = 0,
        columnSpan = columnSpan,
        rowSpan = rowSpan,
        zIndex = zIndex,
    )

    private fun imageContent(path: String = "/media/fanart.png") =
        WidgetContent.Image(path, ScaleMode.Fill, isTransparentOverlay = false, isAsset = false)

    @Test
    fun `backdropWidgetOf picks the largest non-logo image widget`() {
        val small = imageWidget("small", columnSpan = 2, rowSpan = 2)
        val large = imageWidget("large", columnSpan = 6, rowSpan = 6)
        val content = mapOf("small" to imageContent(), "large" to imageContent())

        assertEquals(large, backdropWidgetOf(listOf(small, large), content))
    }

    @Test
    fun `backdropWidgetOf breaks an area tie by lowest zIndex`() {
        val front = imageWidget("front", zIndex = 5)
        val back = imageWidget("back", zIndex = 1)
        val content = mapOf("front" to imageContent(), "back" to imageContent())

        assertEquals(back, backdropWidgetOf(listOf(front, back), content))
    }

    @Test
    fun `backdropWidgetOf breaks an area and zIndex tie by id`() {
        val widgetB = imageWidget("b-widget")
        val widgetA = imageWidget("a-widget")
        val content = mapOf("b-widget" to imageContent(), "a-widget" to imageContent())

        assertEquals(widgetA, backdropWidgetOf(listOf(widgetB, widgetA), content))
    }

    @Test
    fun `backdropWidgetOf excludes logo-style widgets even when they resolve to Image content`() {
        val logo = imageWidget("logo", widgetType = WidgetType.SystemLogo(scaleMode = ScaleMode.Fit))
        val content = mapOf("logo" to imageContent())

        assertNull(backdropWidgetOf(listOf(logo), content))
    }

    @Test
    fun `backdropWidgetOf returns null when nothing resolves to Image content`() {
        val textWidget = imageWidget("text")
        val content = mapOf("text" to WidgetContent.Empty)

        assertNull(backdropWidgetOf(listOf(textWidget), content))
    }

    @Test
    fun `backdropWidgetOf returns null for an empty widget list`() {
        assertNull(backdropWidgetOf(emptyList(), emptyMap()))
    }

    @Test
    fun `canvasSwapNeedsHold is false when the backdrop widget id is unchanged`() {
        val displayed = showing(imageWidget("backdrop"), imageContent("/media/old.png"))
        val target = showing(imageWidget("backdrop"), imageContent("/media/new.png"))

        assertEquals(false, canvasSwapNeedsHold(displayed, target))
    }

    @Test
    fun `canvasSwapNeedsHold is true when the backdrop widget id changes`() {
        val displayed = showing(imageWidget("system-backdrop"), imageContent())
        val target = showing(imageWidget("playing-backdrop"), imageContent())

        assertEquals(true, canvasSwapNeedsHold(displayed, target))
    }

    @Test
    fun `canvasSwapNeedsHold is false when the target canvas has no image backdrop`() {
        val displayed = showing(imageWidget("backdrop"), imageContent())
        val target = showing(imageWidget("text"), WidgetContent.Empty)

        assertEquals(false, canvasSwapNeedsHold(displayed, target))
    }

    @Test
    fun `backdropModelOf returns the themed fallback asset path for fallback content, ignoring the stored path`() {
        val content =
            WidgetContent.Image("stale_placeholder_path", ScaleMode.Fill, isTransparentOverlay = false, isAsset = true)

        assertEquals(fallbackBackgroundAssetPath(isDarkTheme = true), backdropModelOf(content, isDarkTheme = true))
        assertEquals(fallbackBackgroundAssetPath(isDarkTheme = false), backdropModelOf(content, isDarkTheme = false))
    }

    @Test
    fun `backdropModelOf returns a File for real media content`() {
        val content = imageContent("/media/fanart/game.png")

        assertEquals(File("/media/fanart/game.png"), backdropModelOf(content, isDarkTheme = false))
    }

    @Test
    fun `backdropModelOf returns null for non-image content`() {
        assertNull(backdropModelOf(WidgetContent.Empty, isDarkTheme = false))
    }

    private fun showing(
        widget: PlacedWidget,
        content: WidgetContent,
    ) = WidgetCanvasState.Showing(
        widgets = listOf(widget),
        contentByWidgetId = mapOf(widget.id to content),
        navigationDirection = null,
    )
}
