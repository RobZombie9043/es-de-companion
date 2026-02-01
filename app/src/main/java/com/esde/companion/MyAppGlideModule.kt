package com.esde.companion

import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule

/**
 * Custom Glide module for ES-DE Companion.
 *
 * This class is required for KSP to generate GlideApp code.
 * Without this, you'll get the "Failed to find GeneratedAppGlideModule" warning.
 */
@GlideModule
class MyAppGlideModule : AppGlideModule() {
    // Empty implementation is fine - Glide will use defaults
    // You can optionally override applyOptions() to customize cache sizes
}