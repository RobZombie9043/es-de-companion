package com.esde.companion.ui.gameguides

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.esde.companion.domain.model.GameGuideDisplayPreferences
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

private const val BASE_FONT_SIZE_SP = 14
private const val SCROLL_DEBOUNCE_MILLIS = 500L
private const val HIGHLIGHT_COLOR = 0xFFFFEB3B

data class PlainTextViewerConfig(
    val text: String,
    val matches: List<IntRange>,
    val displayPreferences: GameGuideDisplayPreferences,
)

data class ScrollControl(
    val listState: LazyListState,
    val initialScrollFraction: Float,
    val scrollToFractionRequest: Float?,
    val onScrollToFractionHandled: () -> Unit,
)

/**
 * Renders a saved [com.esde.companion.domain.model.GameGuideFormat.PlainText] guide - the
 * whole guide is a single (often very tall) LazyColumn item, since extraction currently
 * produces one page per guide, so [LazyListState.firstVisibleItemIndex] never advances
 * while scrolling within it and can't represent progress on its own; restoring/jumping/
 * tracking position instead works off that one item's own pixel offset and size - see
 * [scrollFraction]'s kdoc.
 */
@OptIn(FlowPreview::class)
@Composable
fun PlainTextGuideContent(
    config: PlainTextViewerConfig,
    scroll: ScrollControl,
    onScrollFractionChanged: (Float) -> Unit,
) {
    val listState = scroll.listState

    LaunchedEffect(config.text) {
        awaitFirstMeasuredItem(listState)
        scrollToFraction(listState, scroll.initialScrollFraction)
    }

    LaunchedEffect(scroll.scrollToFractionRequest) {
        val fraction = scroll.scrollToFractionRequest ?: return@LaunchedEffect
        awaitFirstMeasuredItem(listState)
        scrollToFraction(listState, fraction)
        scroll.onScrollToFractionHandled()
    }

    LaunchedEffect(listState) {
        snapshotFlow { scrollFraction(listState) }
            .distinctUntilChanged()
            .debounce(SCROLL_DEBOUNCE_MILLIS)
            .collect(onScrollFractionChanged)
    }

    val fontFamily = if (config.displayPreferences.monospaceFont) FontFamily.Monospace else FontFamily.Default
    val annotatedText =
        remember(config.text, config.matches) {
            if (config.matches.isEmpty()) {
                AnnotatedString(config.text)
            } else {
                highlightMatches(config.text, config.matches)
            }
        }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text(
                text = annotatedText,
                fontFamily = fontFamily,
                fontSize = (BASE_FONT_SIZE_SP * config.displayPreferences.fontScale).sp,
            )
        }
    }
}

private suspend fun awaitFirstMeasuredItem(listState: LazyListState) {
    snapshotFlow { listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == 0 } }
        .filterNotNull()
        .first()
}

private suspend fun scrollToFraction(
    listState: LazyListState,
    fraction: Float,
) {
    val item = listState.layoutInfo.visibleItemsInfo.first { it.index == 0 }
    val scrollableRange = (item.size - listState.layoutInfo.viewportSize.height).coerceAtLeast(0)
    listState.scrollToItem(0, (fraction * scrollableRange).toInt())
}

/**
 * Fraction (0f top, 1f bottom) of [state]'s scrollable range currently scrolled past. Reads
 * item 0's own pixel offset/size rather than [LazyListState.firstVisibleItemIndex] - see
 * this file's top-level kdoc for why.
 */
private fun scrollFraction(state: LazyListState): Float {
    val layoutInfo = state.layoutInfo
    val item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == 0 } ?: return 0f
    val scrollableRange = (item.size - layoutInfo.viewportSize.height).coerceAtLeast(1)
    return (-item.offset.toFloat() / scrollableRange).coerceIn(0f, 1f)
}

fun findAllMatches(
    text: String,
    query: String,
): List<IntRange> {
    if (query.isBlank()) return emptyList()
    val matches = mutableListOf<IntRange>()
    var searchStart = 0
    while (searchStart <= text.length - query.length) {
        val index = text.indexOf(query, searchStart, ignoreCase = true)
        if (index < 0) break
        matches += index until (index + query.length)
        searchStart = index + query.length
    }
    return matches
}

private fun highlightMatches(
    text: String,
    matches: List<IntRange>,
): AnnotatedString =
    buildAnnotatedString {
        append(text)
        matches.forEach { range ->
            addStyle(SpanStyle(background = Color(HIGHLIGHT_COLOR), color = Color.Black), range.first, range.last + 1)
        }
    }
