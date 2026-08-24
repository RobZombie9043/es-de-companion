package com.esde.companion.ui.widgets.edit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.esde.companion.domain.model.MediaType
import com.esde.companion.domain.model.WidgetType
import com.esde.companion.domain.model.allowsFadeTransition
import com.esde.companion.domain.model.glintEnabled
import com.esde.companion.domain.model.imageTransitionMode
import com.esde.companion.domain.model.isLogoStyle
import com.esde.companion.domain.model.logoTransitionMode
import com.esde.companion.domain.model.supportsFallbackArtwork
import com.esde.companion.domain.model.supportsImageTransition
import com.esde.companion.domain.model.supportsPanZoom
import com.esde.companion.domain.model.supportsPillarbox

internal fun WidgetType.label(): String =
    when (this) {
        is WidgetType.SystemLogo -> "System Logo"
        is WidgetType.SystemImage -> "System Image"
        is WidgetType.SystemMedia -> mediaType.systemWidgetLabel()
        is WidgetType.GameMedia -> mediaType.gameWidgetLabel()
        is WidgetType.CustomImage -> "Custom Image"
        is WidgetType.ColorBackground -> "Color Background"
        is WidgetType.GameDescription -> "Description"
        is WidgetType.Rating -> "Rating"
        is WidgetType.Video -> "Video"
    }

/** Display label for a MediaType-backed System-canvas widget - only FanArt/Screenshots
 * ever appear there (see widgetCatalogFor), named for what they actually show: a random
 * game's art for the browsed system, not the system's own art. */
private fun MediaType.systemWidgetLabel(): String =
    when (this) {
        MediaType.FanArt -> "Random Game Fanart"
        MediaType.Screenshots -> "Random Game Screenshot"
        else -> "System: $name"
    }

/** Display label for a MediaType-backed Game-canvas widget - plain-English names in
 * place of the raw MediaType enum constant (e.g. "Box Cover" instead of "Covers", "3D
 * Box" instead of "ThreeDBoxes"). */
private fun MediaType.gameWidgetLabel(): String =
    when (this) {
        MediaType.Marquees -> "Marquee"
        MediaType.Covers -> "Box Cover"
        MediaType.ThreeDBoxes -> "3D Box"
        MediaType.MixImages -> "Mix Image"
        MediaType.Screenshots -> "Screenshot"
        MediaType.FanArt -> "Fan Art"
        MediaType.TitleScreens -> "Title Screen"
        MediaType.BackCovers -> "Box Back Cover"
        MediaType.PhysicalMedia -> "Physical Media"
        else -> "Game: $name"
    }

/**
 * Simple list picker for widgetCatalogFor(selectedCanvas) - one dialog serves both
 * canvases, since the catalog itself is already filtered per-canvas by the caller.
 */
@Composable
internal fun AddWidgetDialog(
    catalog: List<WidgetType>,
    onPick: (WidgetType) -> Unit,
    onDismiss: () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Widget") },
        text = {
            LazyColumn {
                items(catalog) { widgetType ->
                    Text(
                        text = widgetType.label(),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onPick(widgetType)
                                }
                                .padding(vertical = 12.dp),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

/**
 * Content shown depends entirely on [widgetType]'s variant: image-backed types
 * (SystemLogo, SystemMedia, GameMedia) get a scale-mode choice; ColorBackground gets a
 * preset color swatch row plus an alpha slider. Neither variant needs a Confirm step -
 * [onChange] fires immediately on each selection/slider move (see
 * EditWidgetsViewModel.updateWidgetConfig's kdoc for why that's fine here), so the
 * dialog only needs a way to close, not a way to commit.
 */
@Composable
internal fun ConfigureWidgetDialog(
    widgetType: WidgetType,
    onChange: (WidgetType) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Widget") },
        text = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (widgetType) {
                    is WidgetType.SystemLogo -> {
                        ScaleModeConfig(current = widgetType.scaleMode) { onChange(widgetType.copy(scaleMode = it)) }
                        LogoTransitionPicker(
                            current = widgetType.logoTransitionMode,
                        ) { onChange(widgetType.copy(logoTransitionMode = it)) }
                        GlintConfig(enabled = widgetType.glintEnabled) { onChange(widgetType.copy(glintEnabled = it)) }
                        ImageEffectsConfig(current = widgetType.effects) { onChange(widgetType.copy(effects = it)) }
                    }

                    is WidgetType.SystemImage -> {
                        ScaleModeConfig(current = widgetType.scaleMode) { onChange(widgetType.copy(scaleMode = it)) }
                        if (widgetType.supportsImageTransition && widgetType.allowsFadeTransition) {
                            ImageTransitionPicker(
                                current = widgetType.imageTransitionMode,
                            ) { onChange(widgetType.copy(imageTransitionMode = it)) }
                        }
                        if (widgetType.supportsPanZoom) {
                            PanZoomConfig(
                                enabled = widgetType.panZoomEnabled,
                            ) { onChange(widgetType.copy(panZoomEnabled = it)) }
                        }
                        ImageEffectsConfig(current = widgetType.effects) { onChange(widgetType.copy(effects = it)) }
                    }

                    is WidgetType.SystemMedia -> {
                        ScaleModeConfig(current = widgetType.scaleMode) { onChange(widgetType.copy(scaleMode = it)) }
                        if (widgetType.isLogoStyle) {
                            LogoTransitionPicker(
                                current = widgetType.logoTransitionMode,
                            ) { onChange(widgetType.copy(logoTransitionMode = it)) }
                            GlintConfig(
                                enabled = widgetType.glintEnabled,
                            ) { onChange(widgetType.copy(glintEnabled = it)) }
                        } else if (widgetType.supportsImageTransition && widgetType.allowsFadeTransition) {
                            ImageTransitionPicker(
                                current = widgetType.imageTransitionMode,
                            ) { onChange(widgetType.copy(imageTransitionMode = it)) }
                        }
                        if (widgetType.supportsPanZoom) {
                            PanZoomConfig(
                                enabled = widgetType.panZoomEnabled,
                            ) { onChange(widgetType.copy(panZoomEnabled = it)) }
                        }
                        if (widgetType.supportsFallbackArtwork) {
                            FallbackArtworkConfig(
                                mediaType = widgetType.mediaType,
                                current = widgetType.fallbackMediaType,
                            ) { onChange(widgetType.copy(fallbackMediaType = it)) }
                        }
                        ImageEffectsConfig(current = widgetType.effects) { onChange(widgetType.copy(effects = it)) }
                    }

                    is WidgetType.GameMedia -> {
                        ScaleModeConfig(current = widgetType.scaleMode) { onChange(widgetType.copy(scaleMode = it)) }
                        if (widgetType.isLogoStyle) {
                            LogoTransitionPicker(
                                current = widgetType.logoTransitionMode,
                            ) { onChange(widgetType.copy(logoTransitionMode = it)) }
                            GlintConfig(
                                enabled = widgetType.glintEnabled,
                            ) { onChange(widgetType.copy(glintEnabled = it)) }
                        } else if (widgetType.supportsImageTransition && widgetType.allowsFadeTransition) {
                            ImageTransitionPicker(
                                current = widgetType.imageTransitionMode,
                            ) { onChange(widgetType.copy(imageTransitionMode = it)) }
                        }
                        if (widgetType.supportsPanZoom) {
                            PanZoomConfig(
                                enabled = widgetType.panZoomEnabled,
                            ) { onChange(widgetType.copy(panZoomEnabled = it)) }
                        }
                        if (widgetType.supportsFallbackArtwork) {
                            FallbackArtworkConfig(
                                mediaType = widgetType.mediaType,
                                current = widgetType.fallbackMediaType,
                            ) { onChange(widgetType.copy(fallbackMediaType = it)) }
                        }
                        ImageEffectsConfig(current = widgetType.effects) { onChange(widgetType.copy(effects = it)) }
                    }

                    is WidgetType.CustomImage -> {
                        CustomImageConfig(current = widgetType, onChange = onChange)
                        ScaleModeConfig(current = widgetType.scaleMode) { onChange(widgetType.copy(scaleMode = it)) }
                        if (widgetType.supportsImageTransition && widgetType.allowsFadeTransition) {
                            ImageTransitionPicker(
                                current = widgetType.imageTransitionMode,
                            ) { onChange(widgetType.copy(imageTransitionMode = it)) }
                        }
                        if (widgetType.supportsPanZoom) {
                            PanZoomConfig(
                                enabled = widgetType.panZoomEnabled,
                            ) { onChange(widgetType.copy(panZoomEnabled = it)) }
                        }
                        ImageEffectsConfig(current = widgetType.effects) { onChange(widgetType.copy(effects = it)) }
                    }

                    is WidgetType.ColorBackground ->
                        ColorBackgroundConfig(current = widgetType, onChange = onChange)

                    is WidgetType.GameDescription ->
                        GameDescriptionConfig(current = widgetType, onChange = onChange)

                    is WidgetType.Rating ->
                        RatingConfig(current = widgetType, onChange = onChange)

                    is WidgetType.Video -> {
                        ScaleModeConfig(current = widgetType.scaleMode) { onChange(widgetType.copy(scaleMode = it)) }
                        if (widgetType.supportsPillarbox) {
                            PillarboxConfig(
                                current = widgetType.pillarboxMode,
                            ) { onChange(widgetType.copy(pillarboxMode = it)) }
                        }
                        VideoDelayConfig(
                            delaySeconds = widgetType.delaySeconds,
                        ) { onChange(widgetType.copy(delaySeconds = it)) }
                        VideoAudioConfig(
                            enabled = widgetType.audioEnabled,
                        ) { onChange(widgetType.copy(audioEnabled = it)) }
                        RenderAboveUiConfig(
                            enabled = widgetType.renderAboveUi,
                        ) { onChange(widgetType.copy(renderAboveUi = it)) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
    )
}
