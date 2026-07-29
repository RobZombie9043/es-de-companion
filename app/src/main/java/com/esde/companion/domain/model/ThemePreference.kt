package com.esde.companion.domain.model

/**
 * User-selectable display mode. Auto follows the system dark/light setting; Light and
 * Dark force one regardless of system setting. This is intentionally just a mode, not a
 * color palette - domain has no knowledge of actual colors (those are Compose/Material3
 * types and belong in ui/theme).
 */
enum class ThemePreference {
    Auto,
    Light,
    Dark,
}