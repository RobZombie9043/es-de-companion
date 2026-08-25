package com.esde.companion.ui

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

// Material3 1.3.1's SegmentedButtonContentMeasurePolicy hardcodes 8dp between the icon and
// label (SegmentedButton.kt's private `IconSpacing`) and always reserves
// SegmentedButtonDefaults.IconSize even when a button has no visible icon - see this file's
// kdoc for why that space has to be subtracted here rather than relied on upstream.
private val SEGMENTED_BUTTON_ICON_SPACING = 8.dp

/**
 * Label for a [androidx.compose.material3.SegmentedButton] inside a
 * [androidx.compose.material3.SingleChoiceSegmentedButtonRow] - keeps the row at a stable,
 * single-line height regardless of font scale or label length, degrading a too-long label to
 * an ellipsis instead of wrapping and distorting the row.
 *
 * Workaround for Material3 1.3.1's own `SegmentedButton` layout
 * (`SegmentedButtonContentMeasurePolicy`, confirmed against its source), only visible at a
 * large system font scale: the label is measured against the *same* width constraint as the
 * icon, not that width minus the icon's own reserved space, so `maxLines`/`overflow` alone
 * only ellipsizes once the label text *by itself* exceeds the full button width - not once
 * icon + spacing + label together do. On device at a large font scale this meant the tail of
 * the label was hard-clipped by the button's `Surface.clip(shape)` instead of ellipsized. The
 * `Modifier.layout` below reserves that same icon+spacing width itself so the label is
 * measured against the width actually left over for it.
 *
 * The icon and label are both vertically centered against the label's own measured layout-box
 * height - and confirmed on device that this box is *not* a tight fit around the visible
 * glyphs: a font's ascent metric reserves headroom for the tallest glyph/diacritic the font
 * defines, not just what these particular labels ("On", "Auto", "Dim", ...) actually use, so
 * there's real empty space above the visible ink even with Compose 1.7.6's
 * `includeFontPadding` already defaulted to `false` (confirmed against
 * `AndroidTextStyle.android.kt` - that flag only strips Android's legacy *extra* padding on
 * top of the font's own metrics, it doesn't touch this). That headroom is a fixed fraction of
 * font size, so it's invisible at the default system font scale and grows large enough to
 * notice at a large one - matching what showed up on device: every icon (not just one)
 * sitting visibly high once the font scale was increased, correct at the default scale.
 * `lineHeight`/`lineHeightStyle` below asks Compose to report a line box hugging the font's
 * actual (not padded) metrics, centered - `LineHeightStyle` only has any effect once
 * `lineHeight` is set explicitly, hence setting both together.
 *
 * On-device pixel measurement (before/after, at a large system font scale) confirmed this
 * closes most of the gap (e.g. the Theme picker's checkmark went from 1px off to ~2px off,
 * its moon icon from 3.5px to 1px) but doesn't fully zero it for every icon - a few px of
 * *icon-glyph-specific* asymmetry remains on some icons (worst measured: the sun icon, ~7.5px)
 * that's baked into that icon's own drawn bounds within its nominal square viewBox, not
 * something a shared text/layout fix can reach. Deliberately left as-is rather than adding
 * per-icon manual offsets - see this file's git history for the measurements this was judged
 * against.
 */
@Composable
fun SegmentedButtonLabel(text: String) {
    val reservedIconSpaceModifier =
        Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(withIconSpaceReserved(constraints))
            layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
        }
    val tightLineHeightStyle =
        LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.Both)
    val currentTextStyle = LocalTextStyle.current
    val tightlyCenteredStyle =
        currentTextStyle.copy(lineHeight = currentTextStyle.fontSize, lineHeightStyle = tightLineHeightStyle)
    Text(
        text = text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = tightlyCenteredStyle,
        modifier = reservedIconSpaceModifier,
    )
}

private fun Density.withIconSpaceReserved(constraints: Constraints): Constraints {
    if (constraints.maxWidth == Constraints.Infinity) return constraints
    val reservedWidthPx = (SegmentedButtonDefaults.IconSize + SEGMENTED_BUTTON_ICON_SPACING).roundToPx()
    return constraints.copy(minWidth = 0, maxWidth = (constraints.maxWidth - reservedWidthPx).coerceAtLeast(0))
}
