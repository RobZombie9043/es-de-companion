package com.esde.companion.domain.model

/**
 * User-selectable transition style for logo-style/transparent overlay widgets
 * (system logos, game marquees). Deliberately has no Fade option - cross-fading
 * two overlapping transparent images causes a visible double-exposure, so these
 * are pure transform animations with a hard content swap instead (see
 * AnimatedLogoImage).
 */
enum class LogoTransitionMode {
    None,
    Slide,
    Scale,
}
