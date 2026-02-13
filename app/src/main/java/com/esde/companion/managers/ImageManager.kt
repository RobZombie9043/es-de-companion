package com.esde.companion.managers

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import java.io.File
import android.graphics.RenderEffect
import android.graphics.Shader
import com.esde.companion.data.Widget

/**
 * Centralized image loading manager using Glide.
 *
 * Handles:
 * - Game background images (with blur, scale, fade)
 * - System logos (SVG support)
 * - Widget images (all types)
 * - Custom backgrounds
 * - Cache invalidation
 *
 * Benefits:
 * - Consistent image loading behavior
 * - Centralized cache management
 * - Easy to swap image loading library
 * - Testable image loading logic
 */
class ImageManager(
    private val context: Context,
    private val prefsManager: PreferencesManager
) {

    companion object {
        private const val TAG = "ImageManager"
    }

    // Track the last loaded image path to skip animation when same image is loaded
    private var lastLoadedImagePath: String? = null

    // ========== GAME BACKGROUND LOADING ==========

    /**
     * Load game background image with optional blur and scale animation.
     *
     * @param imageView Target ImageView
     * @param imagePath Full path to image file
     * @param applyBlur Whether to apply blur effect
     * @param applyTransition Whether to apply fade/scale animation
     * @param onLoaded Callback when image loads successfully
     * @param onFailed Callback when image fails to load
     */
    fun loadGameBackground(
        imageView: ImageView,
        imagePath: String,
        applyBlur: Boolean = true,
        applyTransition: Boolean = true,
        onLoaded: (() -> Unit)? = null,
        onFailed: (() -> Unit)? = null
    ) {
        val imageFile = File(imagePath)

        if (!imageFile.exists()) {
            android.util.Log.w(TAG, "Image file does not exist: $imagePath")
            onFailed?.invoke()
            return
        }

        // Check if same image is already loaded
        val isSameImage = lastLoadedImagePath == imagePath
        if (!isSameImage) {
            lastLoadedImagePath = imagePath
        }

        // Skip animation if it's the same image being reloaded
        val shouldAnimate = applyTransition && !isSameImage

        var request = Glide.with(context)
            .load(imageFile)
            .signature(getFileBasedSignature(imageFile))

        // Determine if blur should be applied
        // Note: Blur is applied to ImageView after load, not as Glide transformation
        val shouldApplyBlur = applyBlur && prefsManager.blurLevel > 0

        // Apply transition animation if enabled (and not same image)
        if (shouldAnimate) {
            val animationStyle = prefsManager.animationStyle

            val duration = if (animationStyle == "custom") {
                prefsManager.animationDuration
            } else {
                PreferencesManager.PRESET_ANIMATION_DURATION
            }

            val scaleAmount = if (animationStyle == "custom") {
                prefsManager.animationScale / 100f
            } else {
                PreferencesManager.PRESET_ANIMATION_SCALE / 100f
            }

            when (animationStyle) {
                "fade" -> {
                    // Fade only - no scale
                    request = request
                        .transition(
                            com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
                                .withCrossFade(duration)
                        )
                        .listener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable>,
                                isFirstResource: Boolean
                            ): Boolean {
                                onFailed?.invoke()
                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable,
                                model: Any,
                                target: Target<Drawable>,
                                dataSource: DataSource,
                                isFirstResource: Boolean
                            ): Boolean {
                                // Apply blur after image loads
                                if (shouldApplyBlur) {
                                    applyBlur(imageView, prefsManager.blurLevel)
                                } else {
                                    removeBlur(imageView)
                                }
                                onLoaded?.invoke()
                                return false
                            }
                        })
                }
                "scale_fade", "custom" -> {
                    // Scale + Fade animation (used for both "scale_fade" preset and "custom")
                    request = request.listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            onFailed?.invoke()
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable>,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            // Reset any previous animations
                            imageView.animate().cancel()

                            // Start from scaled down + transparent
                            imageView.alpha = 0f
                            imageView.scaleX = scaleAmount
                            imageView.scaleY = scaleAmount

                            // Animate to full size + opaque
                            imageView.animate()
                                .alpha(1f)
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(duration.toLong())
                                .setInterpolator(android.view.animation.DecelerateInterpolator())
                                .withEndAction {
                                    // Apply blur after animation completes
                                    if (shouldApplyBlur) {
                                        applyBlur(imageView, prefsManager.blurLevel)
                                    } else {
                                        removeBlur(imageView)
                                    }
                                    onLoaded?.invoke()
                                }
                                .start()

                            return false  // Let Glide handle the drawable setting
                        }
                    })
                }
                "none" -> {
                    // No animation - instant display (ensure no residual animation state)
                    request = request.listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            onFailed?.invoke()
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable>,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            // Ensure imageView is at normal state (no animation artifacts)
                            imageView.alpha = 1f
                            imageView.scaleX = 1f
                            imageView.scaleY = 1f

                            // Apply blur
                            if (shouldApplyBlur) {
                                applyBlur(imageView, prefsManager.blurLevel)
                            } else {
                                removeBlur(imageView)
                            }

                            onLoaded?.invoke()
                            return false
                        }
                    })
                }
                else -> {
                    // Fallback for any unrecognized style - no animation
                    android.util.Log.w(TAG, "Unknown animation style: $animationStyle, using no animation")
                    request = request.listener(createSimpleListener(onLoaded, onFailed))
                }
            }
        } else {
            // CRITICAL: Disable ALL Glide transitions when same image
            request = request
                .dontAnimate()  // This is the key - tells Glide to skip all transitions
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        onFailed?.invoke()
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        // Cancel any ongoing animations
                        imageView.animate().cancel()

                        // Ensure imageView is at normal state (no scale/alpha artifacts)
                        imageView.alpha = 1f
                        imageView.scaleX = 1f
                        imageView.scaleY = 1f

                        // Apply blur
                        if (shouldApplyBlur) {
                            applyBlur(imageView, prefsManager.blurLevel)
                        } else {
                            removeBlur(imageView)
                        }

                        onLoaded?.invoke()
                        return false
                    }
                })
        }

        request.into(imageView)
    }

    // ========== SYSTEM LOGO LOADING ==========

    fun loadLargeImage(
        imageView: ImageView,
        imagePath: String,
        maxWidth: Int = 1920,
        maxHeight: Int = 1080,
        onLoaded: (() -> Unit)? = null,
        onFailed: (() -> Unit)? = null
    ) {
        val imageFile = File(imagePath)

        if (!imageFile.exists()) {
            onFailed?.invoke()
            return
        }

        Glide.with(context)
            .load(imageFile)
            .override(maxWidth, maxHeight)  // Glide will downsample automatically
            .signature(getFileBasedSignature(imageFile))
            .listener(createSimpleListener(onLoaded, onFailed))
            .into(imageView)
    }

    // ========== WIDGET IMAGE LOADING ==========

    /**
     * Load widget image (game artwork).
     *
     * @param imageView Target ImageView
     * @param imagePath Path to image file
     * @param scaleType Scale type (FIT or CROP)
     * @param onLoaded Callback when loaded
     * @param onFailed Callback when failed
     */
    fun loadWidgetImage(
        imageView: ImageView,
        imagePath: String,
        scaleType: Widget.ScaleType,
        onLoaded: (() -> Unit)? = null,
        onFailed: (() -> Unit)? = null
    ) {
        if (imagePath.isEmpty()) {
            android.util.Log.d(TAG, "Empty image path for widget")
            onFailed?.invoke()
            return
        }

        val imageFile = File(imagePath)

        if (!imageFile.exists()) {
            android.util.Log.w(TAG, "Widget image does not exist: $imagePath")
            onFailed?.invoke()
            return
        }

        // Check if this is an animated format
        val extension = imagePath.substringAfterLast('.', "").lowercase()
        val isAnimatedFormat = extension in listOf("gif", "webp")

        android.util.Log.d(TAG, "Loading widget image: $imagePath")
        if (isAnimatedFormat) {
            android.util.Log.d(TAG, "Animated format detected - using NONE disk cache strategy")
        }

        // Apply scale type
        when (scaleType) {
            Widget.ScaleType.FIT -> {
                imageView.scaleType = ImageView.ScaleType.FIT_CENTER
            }
            Widget.ScaleType.CROP -> {
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            }
        }

        // Build Glide request with appropriate cache strategy
        // Note: Widgets don't animate - only backgrounds animate
        val glideRequest = Glide.with(context)
            .load(imageFile)
            .signature(getFileBasedSignature(imageFile))
            .listener(createSimpleListener(onLoaded, onFailed))

        // Use different cache strategy for animated formats
        // AnimatedImageDrawable cannot be encoded to disk cache
        if (isAnimatedFormat) {
            glideRequest
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                .into(imageView)
        } else {
            glideRequest
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                .into(imageView)
        }
    }

    // ========== CACHE MANAGEMENT ==========

    /**
     * Get a cache signature based on file metadata.
     * This allows Glide to detect when a file has been modified on disk,
     * even if the filename remains the same.
     *
     * @param file The image file to create a signature for
     * @return ObjectKey combining file's last modified time and size
     */
    private fun getFileBasedSignature(file: File): com.bumptech.glide.signature.ObjectKey {
        return if (file.exists()) {
            // Combine last modified timestamp and file size for unique signature
            val signatureKey = "${file.lastModified()}_${file.length()}"
            com.bumptech.glide.signature.ObjectKey(signatureKey)
        } else {
            // File doesn't exist - use fallback signature
            com.bumptech.glide.signature.ObjectKey("missing_file")
        }
    }

    // ========== HELPER METHODS ==========

    /**
     * Create simple request listener with callbacks.
     */
    private fun createSimpleListener(
        onLoaded: (() -> Unit)?,
        onFailed: (() -> Unit)?
    ): RequestListener<Drawable> {
        return object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>,
                isFirstResource: Boolean
            ): Boolean {
                onFailed?.invoke()
                return false
            }

            override fun onResourceReady(
                resource: Drawable,
                model: Any,
                target: Target<Drawable>,
                dataSource: DataSource,
                isFirstResource: Boolean
            ): Boolean {
                onLoaded?.invoke()
                return false
            }
        }
    }

    /**
     * Apply blur effect using RenderEffect. GPU-accelerated, no resource management needed.
     *
     * @param imageView The ImageView to apply blur to
     * @param blurRadius Blur radius (1-25)
     */
    private fun applyBlur(imageView: ImageView, blurRadius: Int) {
        val radius = blurRadius.coerceIn(1, 25).toFloat()

        // Create blur effect using RenderEffect
        val blurEffect = RenderEffect.createBlurEffect(
            radius,
            radius,
            Shader.TileMode.CLAMP
        )

        // Apply to ImageView (GPU-accelerated)
        imageView.setRenderEffect(blurEffect)

        android.util.Log.d(TAG, "Applied modern blur effect with radius: $radius")
    }

    /**
     * Remove blur effect from ImageView.
     *
     * @param imageView The ImageView to remove blur from
     */
    private fun removeBlur(imageView: ImageView) {
        imageView.setRenderEffect(null)
    }

    /**
     * Cleanup resources.
     * No longer needed with RenderEffect (stateless), but kept for API compatibility.
     */
    fun cleanup() {
        // No cleanup needed with RenderEffect
    }
}