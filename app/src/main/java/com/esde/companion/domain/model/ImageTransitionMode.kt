package com.esde.companion.domain.model

/**
 * User-selectable transition style for general/opaque image widgets (fanart,
 * screenshots, custom system images, etc.) - see [WidgetContent.Image] and its
 * `isTransparentOverlay` flag for the distinction from logo-style content, which
 * uses [LogoTransitionMode] instead.
 */
enum class ImageTransitionMode {
    None,
    Fade,
}
