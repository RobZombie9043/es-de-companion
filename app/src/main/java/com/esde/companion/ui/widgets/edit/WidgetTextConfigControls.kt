package com.esde.companion.ui.widgets.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.esde.companion.domain.model.NoRatingBehavior
import com.esde.companion.domain.model.WidgetType
import com.esde.companion.ui.SegmentedButtonLabel
import kotlin.math.roundToInt

@Composable
internal fun GameDescriptionConfig(
    current: WidgetType.GameDescription,
    onChange: (WidgetType.GameDescription) -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Text Size: ${current.fontSizeSp.roundToInt()}sp", style = MaterialTheme.typography.titleSmall)
            Slider(
                value = current.fontSizeSp,
                onValueChange = { onChange(current.copy(fontSizeSp = it)) },
                valueRange = 10f..36f,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Text Color", style = MaterialTheme.typography.titleSmall)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                COLOR_PRESETS.forEach { colorArgb ->
                    Box(
                        modifier =
                            Modifier
                                .padding(4.dp)
                                .size(36.dp)
                                .background(Color(colorArgb), CircleShape)
                                .then(
                                    if (colorArgb == current.textColorArgb) {
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    } else {
                                        Modifier
                                    },
                                )
                                .clickable {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onChange(current.copy(textColorArgb = colorArgb))
                                },
                    )
                }
            }
            HexColorInput(current = current.textColorArgb) { onChange(current.copy(textColorArgb = it)) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Background Color", style = MaterialTheme.typography.titleSmall)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                COLOR_PRESETS.forEach { colorArgb ->
                    Box(
                        modifier =
                            Modifier
                                .padding(4.dp)
                                .size(36.dp)
                                .background(Color(colorArgb), CircleShape)
                                .then(
                                    if (colorArgb == current.backgroundColorArgb) {
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    } else {
                                        Modifier
                                    },
                                )
                                .clickable {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onChange(current.copy(backgroundColorArgb = colorArgb))
                                },
                    )
                }
            }
            HexColorInput(current = current.backgroundColorArgb) { onChange(current.copy(backgroundColorArgb = it)) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Background Transparency: ${(current.backgroundAlpha * 100).roundToInt()}%",
                style = MaterialTheme.typography.titleSmall,
            )
            Slider(
                value = current.backgroundAlpha,
                onValueChange = { onChange(current.copy(backgroundAlpha = it)) },
                valueRange = 0f..1f,
            )
        }
    }
}

/** No text-size control, unlike [GameDescriptionConfig] - the icon+text are sized to fit
 * the widget's own placed bounds (see WidgetCanvas.kt's AchievementSummaryRow), the same
 * "no configurable size" shape [RatingConfig] uses for its star row. Uses [ColorSwatchPicker]
 * (the shared swatch-row-plus-hex-input control [RatingConfig] already uses) rather than
 * duplicating [GameDescriptionConfig]'s own inlined swatch rows. */
@Composable
internal fun AchievementSummaryConfig(
    current: WidgetType.AchievementSummary,
    onChange: (WidgetType.AchievementSummary) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ColorSwatchPicker(
            label = "Text Color",
            current = current.textColorArgb,
        ) { onChange(current.copy(textColorArgb = it)) }
        ColorSwatchPicker(
            label = "Background Color",
            current = current.backgroundColorArgb,
        ) { onChange(current.copy(backgroundColorArgb = it)) }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Background Transparency: ${(current.backgroundAlpha * 100).roundToInt()}%",
                style = MaterialTheme.typography.titleSmall,
            )
            Slider(
                value = current.backgroundAlpha,
                onValueChange = { onChange(current.copy(backgroundAlpha = it)) },
                valueRange = 0f..1f,
            )
        }
    }
}

/**
 * What a Rating widget shows for a game with no <rating> at all in gamelist.xml - see
 * [WidgetType.Rating.noRatingBehavior]'s kdoc for why this is distinct from a rating of
 * exactly 0.
 */
private fun NoRatingBehavior.displayLabel(): String =
    when (this) {
        NoRatingBehavior.Hide -> "Hide Widget"
        NoRatingBehavior.ShowEmptyStars -> "Show Empty"
    }

@Composable
internal fun RatingConfig(
    current: WidgetType.Rating,
    onChange: (WidgetType.Rating) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "No Rating Behavior", style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                NoRatingBehavior.entries.forEachIndexed { index, behavior ->
                    SegmentedButton(
                        selected = behavior == current.noRatingBehavior,
                        onClick = { onChange(current.copy(noRatingBehavior = behavior)) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = NoRatingBehavior.entries.size),
                        label = { SegmentedButtonLabel(behavior.displayLabel()) },
                    )
                }
            }
        }
        ColorSwatchPicker(
            label = "Star Fill Color",
            current = current.filledColorArgb,
        ) { onChange(current.copy(filledColorArgb = it)) }
        ColorSwatchPicker(
            label = "Star Outline Color",
            current = current.outlineColorArgb,
        ) { onChange(current.copy(outlineColorArgb = it)) }
        ColorSwatchPicker(
            label = "Background Color",
            current = current.backgroundColorArgb,
        ) { onChange(current.copy(backgroundColorArgb = it)) }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Background Transparency: ${(current.backgroundAlpha * 100).roundToInt()}%",
                style = MaterialTheme.typography.titleSmall,
            )
            Slider(
                value = current.backgroundAlpha,
                onValueChange = { onChange(current.copy(backgroundAlpha = it)) },
                valueRange = 0f..1f,
            )
        }
    }
}
