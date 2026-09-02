package com.esde.companion.ui.widgets

import androidx.compose.animation.core.Animatable
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val TEXT_PADDING = 8.dp
private const val LINE_HEIGHT_RATIO = 1.25f
private const val PAUSE_AT_TOP_MS = 4_500L
private const val PAUSE_AT_BOTTOM_MS = 7_000L
private const val PIXELS_PER_SECOND = 35f
private const val MIN_SCROLL_DURATION_MS = 600
private const val MILLIS_PER_SECOND = 1000
private const val TEXT_FADE_IN_MS = 250

/**
 * Renders [text] wrapped to the available width. If the wrapped content is taller than
 * the box (i.e. [androidx.compose.foundation.ScrollState.maxValue] > 0 once laid out), it
 * auto-scrolls on a loop: hold at top -> scroll down -> hold at bottom -> instant reset
 * to top -> hold at top -> repeat. The reset is a hard cut rather than a reverse scroll,
 * since it happens during a "settled" (paused) state rather than mid-motion, so it reads
 * as an intentional restart rather than a glitch. Short text that already fits just sits
 * still, no scrolling triggered.
 *
 * Keyed on [text] so switching games resets scroll position to the top and restarts the
 * pause/scroll cycle for the new content, rather than continuing mid-scroll into
 * unrelated text.
 *
 * [userScrollEnabled] gates only touch-driven scrolling - the automatic pause/scroll
 * loop below always keeps running regardless. EditWidgetsOverlay passes false: a
 * GameDescription widget's own scrollable text otherwise consumes the drag gesture
 * meant for PlaceholderWidgetBox's move-to-reposition handling (the descendant
 * scrollable claims the gesture before the ancestor's drag detector sees it), and
 * manual scrolling serves no purpose in edit mode anyway.
 */
@Composable
fun ScrollingText(
    text: String,
    fontSizeSp: Float,
    textColorArgb: Long,
    modifier: Modifier = Modifier,
    userScrollEnabled: Boolean = true,
) {
    val scrollState = rememberScrollState()
    val alpha = remember { Animatable(1f) }

    Box(modifier = modifier.clipToBounds()) {
        Text(
            text = text,
            fontSize = fontSizeSp.sp,
            lineHeight = (fontSizeSp * LINE_HEIGHT_RATIO).sp,
            color = Color(textColorArgb),
            modifier =
                Modifier
                    .fillMaxSize()
                    .alpha(alpha.value)
                    .verticalScroll(scrollState, enabled = userScrollEnabled)
                    .padding(TEXT_PADDING),
        )
    }

    // Fades the new text in rather than the previous hard cut on a game/system change. Only
    // one direction (fade in, not a true crossfade) - the old content is already gone by the
    // time recomposition delivers the new `text`, and deliberately not duplicated via
    // Crossfade to fake a two-layer transition: two Text instances briefly sharing the same
    // scrollState would fight over its maxValue/position based on their own (likely
    // different) content heights.
    LaunchedEffect(text) {
        alpha.snapTo(0f)
        alpha.animateTo(1f, animationSpec = tween(TEXT_FADE_IN_MS))
    }

    LaunchedEffect(text, scrollState.maxValue) {
        // scrollState survives across games (ScrollingText is composed under a
        // key(widget.id) in WidgetCanvas, so only content.text changes, not composable
        // identity) - without this, switching games mid-scroll leaves the new
        // description showing wherever the previous one's scroll offset happened to be,
        // rather than starting from the top.
        scrollState.scrollTo(0)

        val maxValue = scrollState.maxValue
        if (maxValue <= 0) return@LaunchedEffect
        val scrollDurationMs =
            ((maxValue / PIXELS_PER_SECOND) * MILLIS_PER_SECOND).toInt().coerceAtLeast(MIN_SCROLL_DURATION_MS)
        val spec = tween<Float>(durationMillis = scrollDurationMs, easing = LinearEasing)

        while (true) {
            delay(PAUSE_AT_TOP_MS)
            scrollState.animateScrollTo(maxValue, animationSpec = spec)
            delay(PAUSE_AT_BOTTOM_MS)
            scrollState.scrollTo(0)
        }
    }
}
