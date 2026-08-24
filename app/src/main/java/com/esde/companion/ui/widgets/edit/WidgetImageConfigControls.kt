package com.esde.companion.ui.widgets.edit

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.esde.companion.data.storage.SafPathResolver
import com.esde.companion.domain.model.ImageEffects
import com.esde.companion.domain.model.MediaType
import com.esde.companion.domain.model.ScaleMode
import com.esde.companion.domain.model.WidgetType
import com.esde.companion.domain.model.fallbackMediaTypeOptions
import kotlin.math.roundToInt

/** Shows the currently-picked file's name (or a prompt if none picked yet, e.g. this
 * widget was just added and the picker was cancelled) and a button to (re)launch the
 * same document picker used on add - see addCustomImageLauncher in EditWidgetsOverlay. */
@Composable
internal fun CustomImageConfig(
    current: WidgetType.CustomImage,
    onChange: (WidgetType.CustomImage) -> Unit,
) {
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let(SafPathResolver::resolveDocumentPath)?.let { path -> onChange(current.copy(path = path)) }
        }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Image", style = MaterialTheme.typography.titleSmall)
        Text(
            text = if (current.path.isNotBlank()) current.path.substringAfterLast('/') else "No image chosen",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = { launcher.launch(arrayOf("image/*")) }) {
            Text(if (current.path.isNotBlank()) "Change Image" else "Choose Image")
        }
    }
}

private fun ScaleMode.displayLabel(): String =
    when (this) {
        ScaleMode.Fit -> "Contain"
        ScaleMode.Fill -> "Cover"
    }

private fun ScaleMode.icon(): ImageVector =
    when (this) {
        ScaleMode.Fit -> Icons.Filled.CropFree // whole image visible, letterboxed
        ScaleMode.Fill -> Icons.Filled.Crop // cropped to fill the frame
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScaleModeConfig(
    current: ScaleMode,
    onSelect: (ScaleMode) -> Unit,
) {
    Column {
        Text(text = "Image Scaling", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ScaleMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = mode == current,
                    onClick = { onSelect(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = ScaleMode.entries.size),
                    icon = {
                        SegmentedButtonDefaults.Icon(active = mode == current) {
                            Icon(
                                imageVector = mode.icon(),
                                contentDescription = null,
                                modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                            )
                        }
                    },
                    label = { Text(mode.displayLabel()) },
                )
            }
        }
    }
}

/**
 * Blur + darken controls for image-backed widget types (SystemLogo, SystemMedia,
 * GameMedia). Both scale as simple 0f..1f sliders - see ImageEffects' kdoc for why, and
 * applyBlurEffect/DarkenOverlay in WidgetCanvas.kt for how these map to rendering.
 * Darken is a flat black overlay rather than a tint color - its main use is muting a
 * busy background image so a logo/widget placed on top reads more clearly.
 */
@Composable
internal fun ImageEffectsConfig(
    current: ImageEffects,
    onChange: (ImageEffects) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Blur: ${(current.blurAmount * 100).roundToInt()}%",
                style = MaterialTheme.typography.titleSmall,
            )
            Slider(
                value = current.blurAmount,
                onValueChange = { onChange(current.copy(blurAmount = it)) },
                valueRange = 0f..1f,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Darken: ${(current.darkenAmount * 100).roundToInt()}%",
                style = MaterialTheme.typography.titleSmall,
            )
            Slider(
                value = current.darkenAmount,
                onValueChange = { onChange(current.copy(darkenAmount = it)) },
                valueRange = 0f..1f,
            )
        }
    }
}

/**
 * Only shown when [WidgetType.supportsPanZoom] is true for the widget being configured -
 * eligible image-backed types with ScaleMode.Fill (see its kdoc for why Fit is excluded).
 * See PanZoomImage.kt for the animation this toggle drives.
 */
@Composable
internal fun PanZoomConfig(
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Pan & Zoom", style = MaterialTheme.typography.titleSmall)
            Text(
                text = "Slowly zooms and pans across the image while it's displayed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = enabled, onCheckedChange = onChange)
    }
}

/**
 * Only shown when [WidgetType.supportsFallbackArtwork] is true - lets a Fan Art,
 * Screenshot, or 3D Box widget substitute a different MediaType (or nothing) when its
 * own primary artwork isn't available for the currently browsed system/game - see
 * [MediaType.fallbackMediaTypeOptions] for which choices apply to which [mediaType], and
 * resolveMediaWidgetContent for where this is actually applied at render time.
 */
@Composable
internal fun FallbackArtworkConfig(
    mediaType: MediaType,
    current: MediaType?,
    onSelected: (MediaType?) -> Unit,
) {
    val options = mediaType.fallbackMediaTypeOptions()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Fallback Artwork", style = MaterialTheme.typography.titleSmall)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == current,
                    onClick = { onSelected(option) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    label = { Text(option.fallbackArtworkDisplayLabel()) },
                )
            }
        }
    }
}

/** Plain-English label for a Fallback Artwork choice - deliberately distinct from
 * MediaType.gameWidgetLabel()/systemWidgetLabel() (those describe what a whole widget
 * shows, e.g. "Random Game Fanart"; this describes a fallback target on its own, e.g.
 * just "Fan Art"). `null` is the explicit "None" choice. */
private fun MediaType?.fallbackArtworkDisplayLabel(): String =
    when (this) {
        null -> "None"
        MediaType.FanArt -> "Fan Art"
        MediaType.Screenshots -> "Screenshot"
        MediaType.Covers -> "Box Cover"
        else -> name
    }
