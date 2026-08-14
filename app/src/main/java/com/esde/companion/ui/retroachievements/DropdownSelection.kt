package com.esde.companion.ui.retroachievements

/**
 * A dropdown's current value plus the callback to change it - bundling both together means a
 * chip's rendered label can never desync from what it actually controls, since callers pass the
 * pair as one unit instead of a value and a setter that could drift apart independently.
 */
internal data class DropdownSelection<T>(val value: T, val onValueChanged: (T) -> Unit)

/** One selectable value in a dropdown, alongside its display label. */
internal data class DropdownOption<T>(val value: T, val label: String)
