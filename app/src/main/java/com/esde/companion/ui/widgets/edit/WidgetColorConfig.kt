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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.esde.companion.domain.model.WidgetType
import java.util.Locale
import kotlin.math.roundToInt

/** Fixed preset palette - a full custom color picker (hue/saturation wheel) is more
 * than this widget type needs for a first pass; these presets cover the common cases
 * (tint/darken behind other widgets) with a much simpler UI. */
internal val COLOR_PRESETS =
    listOf(
        // black
        0xFF000000L,
        // white
        0xFFFFFFFFL,
        // gray
        0xFF9E9E9EL,
        // red
        0xFFF44336L,
        // blue
        0xFF2196F3L,
        // green
        0xFF4CAF50L,
        // yellow
        0xFFFFEB3BL,
        // purple
        0xFF9C27B0L,
    )

@Composable
internal fun ColorBackgroundConfig(
    current: WidgetType.ColorBackground,
    onChange: (WidgetType.ColorBackground) -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Color", style = MaterialTheme.typography.titleSmall)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                COLOR_PRESETS.forEach { colorArgb ->
                    Box(
                        modifier =
                            Modifier
                                .padding(4.dp)
                                .size(36.dp)
                                .background(Color(colorArgb), CircleShape)
                                .then(
                                    if (colorArgb == current.colorArgb) {
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    } else {
                                        Modifier
                                    },
                                )
                                .clickable {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onChange(current.copy(colorArgb = colorArgb))
                                },
                    )
                }
            }
            HexColorInput(current = current.colorArgb) { onChange(current.copy(colorArgb = it)) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Transparency: ${(current.alpha * 100).roundToInt()}%",
                style = MaterialTheme.typography.titleSmall,
            )
            Slider(
                value = current.alpha,
                onValueChange = { onChange(current.copy(alpha = it)) },
                valueRange = 0f..1f,
            )
        }
    }
}

/**
 * A labeled preset-swatch row + [HexColorInput], the exact block ColorBackgroundConfig/
 * GameDescriptionConfig each inline once or twice - extracted here since RatingConfig
 * needs it three times (fill, outline, background) in one dialog.
 */
@Composable
internal fun ColorSwatchPicker(
    label: String,
    current: Long,
    onChange: (Long) -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, style = MaterialTheme.typography.titleSmall)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            COLOR_PRESETS.forEach { colorArgb ->
                Box(
                    modifier =
                        Modifier
                            .padding(4.dp)
                            .size(36.dp)
                            .background(Color(colorArgb), CircleShape)
                            .then(
                                if (colorArgb == current) {
                                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                } else {
                                    Modifier
                                },
                            )
                            .clickable {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                onChange(colorArgb)
                            },
                )
            }
        }
        HexColorInput(current = current, onValidHex = onChange)
    }
}

/**
 * Free-form hex entry alongside the preset swatches. [current]'s own alpha channel is
 * always opaque (0xFF...) - the presets are stored that way and transparency is handled
 * separately by ColorBackground.alpha - so this only ever reads/writes the RGB portion.
 *
 * Local [text] is keyed on [current] rather than plain `remember { }`: this means
 * picking a preset (or another external change) reformats the field to match, while
 * typing an in-progress/invalid hex string is left alone across recompositions since
 * [current] doesn't change until a full valid 6-digit value is entered. onValidHex only
 * fires once the text parses cleanly, so partial input never produces a garbage color.
 */
@Composable
internal fun HexColorInput(
    current: Long,
    onValidHex: (Long) -> Unit,
) {
    var text by remember(current) { mutableStateOf(current.toHexRgbString()) }
    val isValid = parseHexColor(text) != null

    OutlinedTextField(
        value = text,
        onValueChange = { newText ->
            text = newText
            parseHexColor(newText)?.let(onValidHex)
        },
        label = { Text("Hex code") },
        leadingIcon = { Text(text = "#", style = MaterialTheme.typography.bodyLarge) },
        isError = !isValid,
        supportingText = { if (!isValid) Text("Enter a 6-digit hex code, e.g. 3A7BD5") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

internal fun Long.toHexRgbString(): String = String.format(Locale.ROOT, "%06X", this and 0xFFFFFF)

internal fun parseHexColor(text: String): Long? {
    val cleaned = text.removePrefix("#").trim()
    if (cleaned.length != 6 || cleaned.any { it !in HEX_CHARS }) return null
    return cleaned.toLongOrNull(16)?.let { 0xFF000000L or it }
}

private val HEX_CHARS = "0123456789abcdefABCDEF".toSet()
