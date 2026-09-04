package com.esde.companion.ui.widgets

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.esde.companion.domain.model.AchievementSummaryWidgetState
import com.esde.companion.domain.model.ImageEffects
import com.esde.companion.domain.model.ImageTransitionMode
import com.esde.companion.domain.model.NavigationDirection
import com.esde.companion.domain.model.PlacedWidget
import com.esde.companion.domain.model.ScaleMode
import com.esde.companion.domain.model.WidgetContent
import com.esde.companion.domain.model.WidgetType
import com.esde.companion.domain.model.cornerRadius
import com.esde.companion.domain.model.glintEnabled
import com.esde.companion.domain.model.imageTransitionActive
import com.esde.companion.domain.model.isLogoStyle
import com.esde.companion.domain.model.logoTransitionMode
import com.esde.companion.domain.model.panZoomActive
import com.esde.companion.ui.main.CrossfadeAsyncImage
import com.esde.companion.ui.theme.LocalIsDarkTheme
import com.esde.companion.ui.video.VideoPlaybackEvent
import java.io.File
import kotlin.math.roundToInt

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
    navigationDirection: NavigationDirection? = null,
    onVideoPlaybackEvent: (VideoPlaybackEvent) -> Unit = {},
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
                    widgetType = widget.widgetType,
                    navigationDirection = navigationDirection,
                    displayOptions = WidgetContentDisplayOptions(onVideoPlaybackEvent = onVideoPlaybackEvent),
                    modifier =
                        Modifier
                            .offset(x = cellWidth * widget.gridColumn, y = cellHeight * widget.gridRow)
                            .size(width = cellWidth * widget.columnSpan, height = cellHeight * widget.rowSpan)
                            .zIndex(widget.zIndex.toFloat()),
                )
            }
        }
    }
}

/**
 * [textUserScrollEnabled]/[onVideoPlaybackEvent] bundled into one type purely to keep
 * [WidgetContentView]'s own parameter count under this project's [LongParameterList]
 * limit, same reasoning as e.g. `SelfHealConfig` - both are optional auxiliary knobs a
 * caller only needs to override for one specific widget-content branch ([WidgetContent.Text]
 * and [WidgetContent.Video] respectively), not a single logically-related pair otherwise.
 */
data class WidgetContentDisplayOptions(
    val textUserScrollEnabled: Boolean = true,
    val onVideoPlaybackEvent: (VideoPlaybackEvent) -> Unit = {},
)

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
 * Transition/glint config is per-widget (Configure Widget dialog), not a global Settings
 * toggle - [WidgetType.imageTransitionMode]/[WidgetType.logoTransitionMode]/
 * [WidgetType.glintEnabled] read straight off [widgetType], since each placed widget
 * carries its own values. Which one applies is decided purely by content shape:
 * [WidgetContent.Image.isTransparentOverlay] (custom logos, marquees) and
 * [WidgetContent.SystemLogoAsset] both always use [WidgetType.logoTransitionMode]; opaque
 * [WidgetContent.Image] uses [WidgetType.imageTransitionActive] - which forces an instant
 * (no-fade) transition regardless of the widget's stored mode in two cases:
 * [WidgetType.forcesInstantImageTransition] widget types (box-art-style game media), and
 * any widget whose configured [ScaleMode] is [ScaleMode.Fit] ("Contain") - a letterboxed
 * image swap reads as a hard content change rather than a scene transition, so it always
 * cuts rather than fades even when a [ScaleMode.Fill] ("Cover") widget elsewhere is fading.
 * See AnimatedLogoImage's kdoc for why logo-style content never gets a fade option, and
 * [WidgetType.allowsFadeTransition]/[WidgetType.supportsImageTransition] for how the
 * Configure dialog decides whether to offer Fade at all.
 *
 * [WidgetType.panZoomActive] (per-widget Configure dialog toggle, see
 * [WidgetType.supportsPanZoom]) drives a continuous ambient zoom/pan on top of the same
 * opaque [WidgetContent.Image] rendering - see PanZoomImage.kt's `rememberPanZoomModifier`.
 * It composes with the crossfade above it in the same Modifier chain rather than as a
 * separate layer, so a fading-in/out image pans/zooms as a single rigid unit.
 *
 * [WidgetType.glintEnabled] only reaches logo-style content, since it's read solely at
 * the single AnimatedLogoImage call site below; the non-logo-style `is WidgetContent.Image`
 * branch never reads it.
 *
 * [navigationDirection] is which way the user just navigated (if known - see
 * NavigationDirectionTracker), used only by [WidgetType.logoTransitionMode] being
 * [LogoTransitionMode.Slide] to enter from the same side. Defaults to null -
 * EditWidgetsOverlay's edit-mode preview has no live AppState to derive it from, so it
 * falls back to AnimatedLogoImage's default direction.
 *
 * [widgetType] decides *how* [content] is rendered, not just what content means -
 * specifically, [WidgetType.isLogoStyle] widgets (system logo, game/system marquees)
 * always route through a single AnimatedLogoImage call site below, regardless of whether
 * [content] currently resolves to [WidgetContent.Empty] or a real path. Branching on
 * `content`'s own type instead (as this used to) put the "nothing found" and "found" cases
 * in different `when` branches, which are different composition groups - Compose would
 * discard and recreate AnimatedLogoImage on every found/not-found flip, silently losing
 * its in-flight transition state and skipping the enter animation the next time a logo
 * did resolve. Keying the branch on the stable [widgetType] instead of the content that
 * changes as you browse keeps one composable instance alive across that flip.
 */
@Composable
internal fun WidgetContentView(
    content: WidgetContent,
    widgetType: WidgetType,
    navigationDirection: NavigationDirection? = null,
    modifier: Modifier = Modifier,
    displayOptions: WidgetContentDisplayOptions = WidgetContentDisplayOptions(),
) {
    if (widgetType.isLogoStyle) {
        val model: Any? =
            when (content) {
                is WidgetContent.Image -> if (content.isAsset) content.path else File(content.path)
                is WidgetContent.SystemLogoAsset -> content.assetPath
                else -> null
            }
        val scaleMode =
            when (content) {
                is WidgetContent.Image -> content.scaleMode
                is WidgetContent.SystemLogoAsset -> content.scaleMode
                else -> ScaleMode.Fit
            }
        val effects =
            when (content) {
                is WidgetContent.Image -> content.effects
                is WidgetContent.SystemLogoAsset -> content.effects
                else -> ImageEffects()
            }

        Box(modifier = modifier) {
            AnimatedLogoImage(
                model = model,
                contentDescription = null,
                contentScale = scaleMode.toContentScale(),
                mode = widgetType.logoTransitionMode,
                direction = navigationDirection,
                glintEnabled = widgetType.glintEnabled,
                modifier = Modifier.fillMaxSize().applyBlurEffect(effects),
            )
            DarkenOverlay(effects = effects)
            if (content is WidgetContent.NameFallback) {
                Text(
                    text = content.text,
                    color = if (LocalIsDarkTheme.current) Color.White else Color.Black,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).padding(8.dp),
                )
            }
        }
        return
    }

    when (content) {
        WidgetContent.Empty -> Unit

        is WidgetContent.Color -> {
            // Animated - a color/alpha change previously snapped instantly.
            val targetColor = Color(content.colorArgb).copy(alpha = content.alpha)
            val animatedColor by animateColorAsState(targetValue = targetColor, label = "widgetColor")
            Box(
                modifier =
                    modifier
                        .applyCornerRadius(widgetType.cornerRadius)
                        .background(animatedColor),
            )
        }

        is WidgetContent.Image ->
            Box(modifier = modifier.applyCornerRadius(widgetType.cornerRadius)) {
                val isDarkTheme = LocalIsDarkTheme.current
                val model = if (content.isAsset) fallbackBackgroundAssetPath(isDarkTheme) else File(content.path)
                CrossfadeAsyncImage(
                    model = model,
                    contentDescription = null,
                    contentScale = content.scaleMode.toContentScale(),
                    durationMillis = widgetType.imageTransitionActive.toDurationMillis(),
                    modifier =
                        Modifier.fillMaxSize()
                            .applyBlurEffect(content.effects)
                            .then(rememberPanZoomModifier(enabled = widgetType.panZoomActive, model = model)),
                )
                DarkenOverlay(effects = content.effects)
            }

        // unreachable: SystemLogoAsset only occurs for SystemLogo widgetType, which isLogoStyle handles above
        is WidgetContent.SystemLogoAsset -> Unit

        // unreachable: only ever produced for isLogoStyle widget types, handled above
        is WidgetContent.NameFallback -> Unit

        is WidgetContent.Text ->
            Box(
                modifier =
                    modifier
                        .applyCornerRadius(widgetType.cornerRadius)
                        .background(
                            Color(content.backgroundColorArgb).copy(alpha = content.backgroundAlpha),
                        ),
            ) {
                ScrollingText(
                    text = content.text,
                    fontSizeSp = content.fontSizeSp,
                    textColorArgb = content.textColorArgb,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = displayOptions.textUserScrollEnabled,
                )
            }

        is WidgetContent.Rating ->
            Box(
                modifier =
                    modifier
                        .applyCornerRadius(widgetType.cornerRadius)
                        .background(
                            Color(content.backgroundColorArgb).copy(alpha = content.backgroundAlpha),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                RatingStars(
                    starCount = content.starCount,
                    filledColorArgb = content.filledColorArgb,
                    outlineColorArgb = content.outlineColorArgb,
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                )
            }

        is WidgetContent.Video ->
            // renderAboveUi widgets are rendered instead by WidgetOverlay's
            // TopLayerVideoWidgets, above FABs/App Dock/App Drawer - see
            // WidgetType.Video.renderAboveUi's kdoc. Skipped here entirely (not just
            // moved) so this widget only ever owns one ExoPlayer pool/composable.
            if (!content.renderAboveUi) {
                WidgetVideoContent(
                    content = content,
                    onPlaybackEvent = displayOptions.onVideoPlaybackEvent,
                    modifier = modifier,
                )
            }

        is WidgetContent.AchievementSummary ->
            Box(
                modifier =
                    modifier
                        .applyCornerRadius(widgetType.cornerRadius)
                        .background(
                            Color(content.backgroundColorArgb).copy(alpha = content.backgroundAlpha),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                AchievementSummaryWidgetBody(content)
            }
    }
}

/** [BoxScope] receiver so the refresh indicator below can [Modifier.align] itself within the
 * enclosing Box (see CLAUDE.md's Known Gotchas on align() needing a direct-child BoxScope). */
@Composable
private fun BoxScope.AchievementSummaryWidgetBody(content: WidgetContent.AchievementSummary) {
    val textColor = Color(content.textColorArgb)
    when (val state = content.state) {
        AchievementSummaryWidgetState.Loading -> CircularProgressIndicator(color = textColor)

        AchievementSummaryWidgetState.Unavailable ->
            Text(
                text = "No Achievements",
                color = textColor,
                fontSize = content.fontSizeSp.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(8.dp),
            )

        is AchievementSummaryWidgetState.Loaded -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(8.dp),
            ) {
                Text(
                    text = "${state.unlockedCount} / ${state.totalCount} Achievements",
                    color = textColor,
                    fontSize = content.fontSizeSp.sp,
                    textAlign = TextAlign.Center,
                )
                val statsText =
                    "${state.earnedPoints} / ${state.totalPoints} pts · " +
                        "${state.completionPercent.roundToInt()}%"
                Text(
                    text = statsText,
                    color = textColor,
                    fontSize = content.fontSizeSp.sp,
                    textAlign = TextAlign.Center,
                )
            }
            // A stale cached value shown immediately while a background refresh runs - see
            // WidgetContent.AchievementSummary's kdoc.
            if (state.isRefreshing) {
                val indicatorModifier =
                    Modifier.align(Alignment.TopEnd).padding(4.dp).size(ACHIEVEMENT_REFRESH_INDICATOR_SIZE)
                CircularProgressIndicator(
                    modifier = indicatorModifier,
                    strokeWidth = ACHIEVEMENT_REFRESH_INDICATOR_STROKE_WIDTH,
                    color = textColor,
                )
            }
        }
    }
}

/** Max blur radius a fully-scaled-up (blurAmount = 1f) ImageEffects maps to. */
private val IMAGE_EFFECTS_MAX_BLUR = 24.dp

private val ACHIEVEMENT_REFRESH_INDICATOR_SIZE = 14.dp
private val ACHIEVEMENT_REFRESH_INDICATOR_STROKE_WIDTH = 2.dp

private fun Modifier.applyBlurEffect(effects: ImageEffects): Modifier =
    if (effects.blurAmount > 0f) blur(IMAGE_EFFECTS_MAX_BLUR * effects.blurAmount) else this

@Composable
private fun DarkenOverlay(effects: ImageEffects) {
    if (effects.darkenAmount <= 0f) return
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = effects.darkenAmount)),
    )
}

private fun ScaleMode.toContentScale(): ContentScale =
    when (this) {
        ScaleMode.Fit -> ContentScale.Fit
        ScaleMode.Fill -> ContentScale.Crop
    }

/** Fade duration CrossfadeAsyncImage should use for this mode. durationMillis of 0 makes
 * it snap instead of animate (see its kdoc), which is how ImageTransitionMode.None avoids
 * a visible transition without losing the flash-safe pre-decode. */
private fun ImageTransitionMode.toDurationMillis(): Int =
    when (this) {
        ImageTransitionMode.None -> 0
        ImageTransitionMode.Fade -> IMAGE_TRANSITION_DURATION_MILLIS
    }

private const val IMAGE_TRANSITION_DURATION_MILLIS = 500
