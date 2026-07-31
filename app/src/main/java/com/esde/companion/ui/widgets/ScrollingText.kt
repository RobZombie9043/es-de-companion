package com.esde.companion.ui.widgets

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val TEXT_PADDING = 8.dp
private const val PAUSE_AT_EDGE_MS = 2_200L
private const val PIXELS_PER_SECOND = 35f
private const val MIN_SCROLL_DURATION_MS = 600

/**
 * Renders [text] wrapped to the available width. If the wrapped content is taller than
 * the box (i.e. [androidx.compose.foundation.ScrollState.maxValue] > 0 once laid out), it
 * auto-scrolls top -> bottom -> top on a loop with a pause at each end, teleprompter-style
 * - this suits multi-paragraph prose (game descriptions) far better than a horizontal
 * marquee would. Short text that already fits just sits still, no scrolling triggered.
 *
 * Keyed on [text] so switching games resets scroll position to the top and restarts the
 * pause/scroll cycle for the new content, rather than continuing mid-scroll into
 * unrelated text.
 */
@Composable
fun ScrollingText(
    text: String,
    fontSizeSp: Float,
    textColorArgb: Long,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Box(modifier = modifier.clipToBounds()) {
        Text(
            text = text,
            fontSize = fontSizeSp.sp,
            color = Color(textColorArgb),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(TEXT_PADDING),
        )
    }

    LaunchedEffect(text, scrollState.maxValue) {
        val maxValue = scrollState.maxValue
        if (maxValue <= 0) return@LaunchedEffect

        val scrollDurationMs = ((maxValue / PIXELS_PER_SECOND) * 1000).toInt().coerceAtLeast(MIN_SCROLL_DURATION_MS)
        val spec = tween<Float>(durationMillis = scrollDurationMs, easing = LinearEasing)

        while (true) {
            delay(PAUSE_AT_EDGE_MS)
            scrollState.animateScrollTo(maxValue, animationSpec = spec)
            delay(PAUSE_AT_EDGE_MS)
            scrollState.animateScrollTo(0, animationSpec = spec)
        }
    }
}