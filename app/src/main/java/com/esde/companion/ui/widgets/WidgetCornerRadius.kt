package com.esde.companion.ui.widgets

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.esde.companion.domain.model.CornerRadius

/** Maps each preset to a dp value drawn from this app's existing small, reused set of
 * corner-radius constants (SettingsLabelIconShape's 8dp, SettingsItemShape's 16dp, App
 * Dock's 24dp) rather than an arbitrary free-form value, so a rounded widget stays visually
 * consistent with the rest of the app's chrome. */
internal fun CornerRadius.toDp(): Dp =
    when (this) {
        CornerRadius.None -> 0.dp
        CornerRadius.Small -> 8.dp
        CornerRadius.Medium -> 16.dp
        CornerRadius.Large -> 24.dp
    }

/** No-op for [CornerRadius.None] rather than clipping to a zero-radius shape - same
 * "only apply when it does something" reasoning as WidgetCanvas.kt's applyBlurEffect. */
internal fun Modifier.applyCornerRadius(cornerRadius: CornerRadius): Modifier =
    if (cornerRadius == CornerRadius.None) this else clip(RoundedCornerShape(cornerRadius.toDp()))
