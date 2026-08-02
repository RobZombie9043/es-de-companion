package com.esde.companion.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.esde.companion.domain.model.ImageEffects
import com.esde.companion.domain.model.ImageTransitionMode
import com.esde.companion.domain.model.LogoTransitionMode
import com.esde.companion.domain.model.NavigationDirection
import com.esde.companion.domain.model.PlacedWidget
import com.esde.companion.domain.model.ScaleMode
import com.esde.companion.domain.model.WidgetContent
import com.esde.companion.ui.main.CrossfadeAsyncImage
import java.io.File

/**
 * Renders [widgets] on a grid derived from the available space (see [gridDimensionsFor]),
 * positioning/sizing each by its saved grid coordinates. [contentByWidgetId] supplies
 * what each widget should currently show - resolved separately (see WidgetContentResolver)
 * so this composable stays purely about layout, not resolution.
 *
 * Overlap between widgets is allowed by design - stacking order follows PlacedWidget.zIndex,
 * not list order, so callers don't need to pre-sort [widgets].
 */
@Composable
fun WidgetCanvas(
    widgets: List<PlacedWidget>,
    contentByWidgetId: Map<String, WidgetContent>,
    imageTransitionMode: ImageTransitionMode,
    logoTransitionMode: LogoTransitionMode,
    navigationDirection: NavigationDirection? = null,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val grid = remember(maxWidth, maxHeight) { gridDimensionsFor(maxWidth, maxHeight) }
        val cellWidth = maxWidth / grid.columns
        val cellHeight = maxHeight / grid.rows

        for (widget in widgets) {
            key(widget.id) {
                val content = contentByWidgetId[widget.id] ?: WidgetContent.Empty
                WidgetContentView(
                    content = content,
                    imageTransitionMode = imageTransitionMode,
                    logoTransitionMode = logoTransitionMode,
                    navigationDirection = navigationDirection,
                    modifier = Modifier
                        .offset(x = cellWidth * widget.gridColumn, y = cellHeight * widget.gridRow)
                        .size(width = cellWidth * widget.columnSpan, height = cellHeight * widget.rowSpan)
                        .zIndex(widget.zIndex.toFloat()),
                )
            }
        }
    }
}

/** Internal, not private - reused as-is by EditWidgetsOverlay's edit-mode preview
 * rendering (see EditWidgetsViewModel.previewContent's kdoc), so both the live screen
 * and edit mode render WidgetContent identically rather than duplicating this logic.
 *
 * Blur/darken (ImageEffects) are applied one layer above the actual Coil image: blur goes
 * on the image's own modifier (it has to be on the thing being drawn), while darken is a
 * separate translucent Box drawn on top inside the same wrapping Box. CrossfadeAsyncImage/
 * AsyncImage themselves stay effect-agnostic.
 *
 * Note: Modifier.blur is RenderEffect-backed and only actually blurs on API 31+; below
 * that it's a harmless no-op, so a configured blur simply won't be visible pre-Android
 * 12 while darken still works everywhere. Not worth a manual BlurMaskFilter fallback for
 * this device-controlled deployment target.
 *
 * [textUserScrollEnabled] only affects [WidgetContent.Text] (GameDescription) - see
 * [ScrollingText]'s kdoc for why EditWidgetsOverlay passes false.
 *
 * [imageTransitionMode]/[logoTransitionMode] are the two global Settings > UI Settings
 * transition styles - which one applies is decided purely by content shape:
 * [WidgetContent.Image.isTransparentOverlay] (custom logos, marquees) and
 * [WidgetContent.SystemLogoAsset] both always use [logoTransitionMode]; opaque
 * [WidgetContent.Image] uses [imageTransitionMode]. See AnimatedLogoImage's kdoc for why
 * logo-style content never gets a fade option.
 *
 * [navigationDirection] is which way the user just navigated (if known - see
 * NavigationDirectionTracker), used only by [LogoTransitionMode.Slide] to enter from the
 * same side. Defaults to null - EditWidgetsOverlay's edit-mode preview has no live
 * AppState to derive it from, so it falls back to AnimatedLogoImage's default direction.
 */
@Composable
internal fun WidgetContentView(
    content: WidgetContent,
    imageTransitionMode: ImageTransitionMode = ImageTransitionMode.None,
    logoTransitionMode: LogoTransitionMode = LogoTransitionMode.None,
    navigationDirection: NavigationDirection? = null,
    modifier: Modifier = Modifier,
    textUserScrollEnabled: Boolean = true,
) {
    when (content) {
        WidgetContent.Empty -> Unit

        is WidgetContent.Color ->
            Box(modifier = modifier.background(Color(content.colorArgb).copy(alpha = content.alpha)))

        is WidgetContent.Image ->
            Box(modifier = modifier) {
                val model = if (content.isAsset) content.path else File(content.path)
                if (content.isTransparentOverlay) {
                    AnimatedLogoImage(
                        model = model,
                        contentDescription = null,
                        contentScale = content.scaleMode.toContentScale(),
                        mode = logoTransitionMode,
                        direction = navigationDirection,
                        modifier = Modifier.fillMaxSize().applyBlurEffect(content.effects),
                    )
                } else {
                    val (durationMillis, scaleFrom) = imageTransitionMode.toDurationAndScale()
                    CrossfadeAsyncImage(
                        model = model,
                        contentDescription = null,
                        contentScale = content.scaleMode.toContentScale(),
                        durationMillis = durationMillis,
                        scaleFrom = scaleFrom,
                        modifier = Modifier.fillMaxSize().applyBlurEffect(content.effects),
                    )
                }
                DarkenOverlay(effects = content.effects)
            }

        is WidgetContent.SystemLogoAsset ->
            Box(modifier = modifier) {
                AnimatedLogoImage(
                    model = content.assetPath,
                    contentDescription = null,
                    contentScale = content.scaleMode.toContentScale(),
                    mode = logoTransitionMode,
                    direction = navigationDirection,
                    modifier = Modifier.fillMaxSize().applyBlurEffect(content.effects),
                )
                DarkenOverlay(effects = content.effects)
            }

        is WidgetContent.Text ->
            Box(
                modifier = modifier.background(
                    Color(content.backgroundColorArgb).copy(alpha = content.backgroundAlpha),
                ),
            ) {
                ScrollingText(
                    text = content.text,
                    fontSizeSp = content.fontSizeSp,
                    textColorArgb = content.textColorArgb,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = textUserScrollEnabled,
                )
            }
    }
}

/** Max blur radius a fully-scaled-up (blurAmount = 1f) ImageEffects maps to. */
private val IMAGE_EFFECTS_MAX_BLUR = 24.dp

private fun Modifier.applyBlurEffect(effects: ImageEffects): Modifier =
    if (effects.blurAmount > 0f) blur(IMAGE_EFFECTS_MAX_BLUR * effects.blurAmount) else this

@Composable
private fun DarkenOverlay(effects: ImageEffects) {
    if (effects.darkenAmount <= 0f) return
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = effects.darkenAmount)),
    )
}

private fun ScaleMode.toContentScale(): ContentScale = when (this) {
    ScaleMode.Fit -> ContentScale.Fit
    ScaleMode.Fill -> ContentScale.Crop
}

/** Fade duration + scale-start fraction CrossfadeAsyncImage should use for this mode.
 * durationMillis of 0 makes it snap instead of animate (see its kdoc), which is how
 * ImageTransitionMode.None avoids a visible transition without losing the flash-safe
 * pre-decode. Not yet user-customizable - see ImageTransitionMode's kdoc. */
private fun ImageTransitionMode.toDurationAndScale(): Pair<Int, Float?> = when (this) {
    ImageTransitionMode.None -> 0 to null
    ImageTransitionMode.Fade -> IMAGE_TRANSITION_DURATION_MILLIS to null
    ImageTransitionMode.FadeScale -> IMAGE_TRANSITION_DURATION_MILLIS to IMAGE_TRANSITION_SCALE_FROM
}

private const val IMAGE_TRANSITION_DURATION_MILLIS = 500
private const val IMAGE_TRANSITION_SCALE_FROM = 0.95f