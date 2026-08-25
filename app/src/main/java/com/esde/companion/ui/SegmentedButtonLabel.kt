package com.esde.companion.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow

/**
 * Label for a [androidx.compose.material3.SegmentedButton] inside a
 * [androidx.compose.material3.SingleChoiceSegmentedButtonRow] - keeps the row at a
 * stable, single-line height regardless of font scale or label length, degrading a
 * too-long label to an ellipsis instead of wrapping and distorting the row.
 */
@Composable
fun SegmentedButtonLabel(text: String) {
    Text(
        text = text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
