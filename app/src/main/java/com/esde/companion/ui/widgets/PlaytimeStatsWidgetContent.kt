package com.esde.companion.ui.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.esde.companion.domain.model.GamePlaytimeStats
import com.esde.companion.domain.model.PlaytimeStatsMode
import com.esde.companion.domain.model.PlaytimeStatsWidgetState
import com.esde.companion.domain.model.WidgetContent

private val PLAYTIME_STATS_LINE_SPACING = 4.dp

/** Split out of WidgetCanvas.kt purely to keep that file's function count under this
 * project's TooManyFunctions limit - see CLAUDE.md's ktlint/detekt gotcha on hand-editing
 * baseline entries vs. splitting files; this is a genuine, separable "one widget type's
 * rendering" unit, not an arbitrary split. */
@Composable
internal fun PlaytimeStatsWidgetBody(content: WidgetContent.PlaytimeStats) {
    val textColor = Color(content.textColorArgb)
    when (val state = content.state) {
        PlaytimeStatsWidgetState.Loading ->
            CircularProgressIndicator(
                color = textColor,
                strokeWidth = 2.dp,
                modifier = Modifier.size(content.fontSizeSp.dp),
            )

        PlaytimeStatsWidgetState.Unavailable ->
            PlaytimeStatsTwoLineText(
                line1 = beatLine(null),
                line2 = content.mode.secondLine(null),
                textColor = textColor,
                fontSizeSp = content.fontSizeSp,
            )

        is PlaytimeStatsWidgetState.Loaded ->
            PlaytimeStatsTwoLineText(
                line1 = beatLine(content.mode.beatSecondsOf(state.stats)),
                line2 = content.mode.secondLine(content.mode.secondSecondsOf(state.stats)),
                textColor = textColor,
                fontSizeSp = content.fontSizeSp,
            )
    }
}

/** Which of [GamePlaytimeStats]'s two "Beat" fields (Casual's softcore median or
 * Hardcore's) this mode shows on the widget's first line. */
private fun PlaytimeStatsMode.beatSecondsOf(stats: GamePlaytimeStats): Int? =
    when (this) {
        PlaytimeStatsMode.Casual -> stats.beatSeconds
        PlaytimeStatsMode.Hardcore -> stats.beatHardcoreSeconds
    }

/** Which of [GamePlaytimeStats]'s two full-completion fields ("Completed" for Casual,
 * "Mastered" for Hardcore) this mode shows on the widget's second line. */
private fun PlaytimeStatsMode.secondSecondsOf(stats: GamePlaytimeStats): Int? =
    when (this) {
        PlaytimeStatsMode.Casual -> stats.completedSeconds
        PlaytimeStatsMode.Hardcore -> stats.masteredSeconds
    }

private fun beatLine(seconds: Int?): String = "Beat the game - ${formatPlaytimeSeconds(seconds)}"

private fun PlaytimeStatsMode.secondLine(seconds: Int?): String {
    val label = if (this == PlaytimeStatsMode.Casual) "Completed" else "Mastered"
    return "$label - ${formatPlaytimeSeconds(seconds)}"
}

private const val SECONDS_PER_HOUR = 3600
private const val SECONDS_PER_MINUTE = 60

/** "none" for a milestone too few players have reached (or a game with none logged) - see
 * [GamePlaytimeStats]'s kdoc - otherwise "XhXXm", hours unpadded, minutes zero-padded. */
private fun formatPlaytimeSeconds(seconds: Int?): String {
    if (seconds == null) return "none"
    val hours = seconds / SECONDS_PER_HOUR
    val minutes = (seconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
    return "${hours}h ${minutes.toString().padStart(2, '0')}m"
}

@Composable
private fun PlaytimeStatsTwoLineText(
    line1: String,
    line2: String,
    textColor: Color,
    fontSizeSp: Float,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PLAYTIME_STATS_LINE_SPACING),
        modifier = Modifier.padding(8.dp),
    ) {
        Text(text = line1, color = textColor, fontSize = fontSizeSp.sp, textAlign = TextAlign.Center, softWrap = false)
        Text(text = line2, color = textColor, fontSize = fontSizeSp.sp, textAlign = TextAlign.Center, softWrap = false)
    }
}
