package com.esde.companion.managers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.caverock.androidsvg.SVG
import java.io.File
import android.renderscript.RenderScript
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.ScriptIntrinsicBlur

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

        // Animation durations
        private const val FADE_DURATION = 300

        // Cache signature for invalidation
        private var cacheVersion = 1
    }

    private val renderScript: RenderScript by lazy {
        RenderScript.create(context)
    }

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

        android.util.Log.d(TAG, "Loading game background: $imagePath")

        var request = Glide.with(context)
            .load(imageFile)
            .signature(getCacheSignature())

        // Apply blur if enabled
        if (applyBlur && prefsManager.blurLevel > 0) {
            request = request.transform(BlurTransformation(context, prefsManager.blurLevel))
        }

        // Apply transition animation if enabled
        if (applyTransition) {
            val animationStyle = prefsManager.animationStyle
            val duration = prefsManager.animationDuration
            val scale = prefsManager.animationScale / 100f + 1f

            when (animationStyle) {
                "fade" -> {
                    request = request.transition(
                        com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
                            .withCrossFade(duration)
                    )
                }
                "slide" -> {
                    // Custom slide animation (implemented separately)
                    request = request.transition(
                        com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
                            .withCrossFade(duration)
                    )
                }
                "scale" -> {
                    // Scale animation handled by listener
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
                            // Animate scale
                            imageView.scaleX = scale
                            imageView.scaleY = scale
                            imageView.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(duration.toLong())
                                .start()

                            onLoaded?.invoke()
                            return false
                        }
                    })
                }
                else -> {
                    // No animation
                }
            }
        } else {
            // No transition - still need listener for callbacks
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
                    onLoaded?.invoke()
                    return false
                }
            })
        }

        request.into(imageView)
    }

    // ========== SYSTEM LOGO LOADING ==========

    /**
     * Load system logo (supports SVG).
     *
     * @param imageView Target ImageView
     * @param logoPath Path to logo file (or "builtin://systemname" for assets)
     * @param onLoaded Callback when logo loads
     * @param onFailed Callback when logo fails
     */
    fun loadSystemLogo(
        imageView: ImageView,
        logoPath: String,
        onLoaded: (() -> Unit)? = null,
        onFailed: (() -> Unit)? = null
    ) {
        android.util.Log.d(TAG, "Loading system logo: $logoPath")

        if (logoPath.startsWith("builtin://")) {
            // Load from assets
            val systemName = logoPath.removePrefix("builtin://")
            loadLogoFromAssets(imageView, systemName, onLoaded, onFailed)
        } else {
            // Load from file
            val logoFile = File(logoPath)

            if (!logoFile.exists()) {
                android.util.Log.w(TAG, "Logo file does not exist: $logoPath")
                onFailed?.invoke()
                return
            }

            // Check if SVG
            if (logoPath.endsWith(".svg", ignoreCase = true)) {
                loadSvgLogo(imageView, logoFile, onLoaded, onFailed)
            } else {
                // Regular image
                Glide.with(context)
                    .load(logoFile)
                    .signature(getCacheSignature())
                    .listener(createSimpleListener(onLoaded, onFailed))
                    .into(imageView)
            }
        }
    }

    /**
     * Load logo from assets folder.
     */
    private fun loadLogoFromAssets(
        imageView: ImageView,
        systemName: String,
        onLoaded: (() -> Unit)?,
        onFailed: (() -> Unit)?
    ) {
        try {
            val assetManager = context.assets
            val extensions = listOf("svg", "png", "jpg")

            for (ext in extensions) {
                val assetPath = "system_logos/$systemName.$ext"

                try {
                    val inputStream = assetManager.open(assetPath)

                    if (ext == "svg") {
                        // Load SVG from assets
                        val svg = SVG.getFromInputStream(inputStream)
                        val bitmap = svgToBitmap(svg, imageView.width, imageView.height)
                        imageView.setImageBitmap(bitmap)
                        onLoaded?.invoke()
                    } else {
                        // Load regular image
                        Glide.with(context)
                            .load(inputStream)
                            .listener(createSimpleListener(onLoaded, onFailed))
                            .into(imageView)
                    }
                    return
                } catch (e: Exception) {
                    // Try next extension
                    continue
                }
            }

            // No logo found in assets
            android.util.Log.w(TAG, "No logo found in assets for: $systemName")
            onFailed?.invoke()

        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error loading logo from assets", e)
            onFailed?.invoke()
        }
    }

    /**
     * Load SVG logo from file.
     */
    private fun loadSvgLogo(
        imageView: ImageView,
        svgFile: File,
        onLoaded: (() -> Unit)?,
        onFailed: (() -> Unit)?
    ) {
        try {
            val svg = SVG.getFromInputStream(svgFile.inputStream())

            // Wait for view to be measured
            imageView.post {
                val width = imageView.width
                val height = imageView.height

                if (width > 0 && height > 0) {
                    val bitmap = svgToBitmap(svg, width, height)
                    imageView.setImageBitmap(bitmap)
                    onLoaded?.invoke()
                } else {
                    android.util.Log.w(TAG, "ImageView not measured yet")
                    onFailed?.invoke()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error loading SVG logo", e)
            onFailed?.invoke()
        }
    }

    /**
     * Convert SVG to Bitmap with proper scaling.
     */
    private fun svgToBitmap(svg: SVG, targetWidth: Int, targetHeight: Int): Bitmap {
        val svgWidth = svg.documentWidth
        val svgHeight = svg.documentHeight

        val scale = if (svgWidth > 0 && svgHeight > 0) {
            minOf(
                targetWidth / svgWidth,
                targetHeight / svgHeight
            )
        } else {
            1f
        }

        val scaledWidth = (svgWidth * scale).toInt()
        val scaledHeight = (svgHeight * scale).toInt()

        val bitmap = Bitmap.createBitmap(
            scaledWidth.coerceAtLeast(1),
            scaledHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )

        val canvas = android.graphics.Canvas(bitmap)
        canvas.scale(scale, scale)
        svg.renderToCanvas(canvas)

        return bitmap
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
        scaleType: com.esde.companion.OverlayWidget.ScaleType,
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
            com.esde.companion.OverlayWidget.ScaleType.FIT -> {
                imageView.scaleType = ImageView.ScaleType.FIT_CENTER
            }
            com.esde.companion.OverlayWidget.ScaleType.CROP -> {
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            }
        }

        // Build Glide request with appropriate cache strategy
        val glideRequest = Glide.with(context)
            .load(imageFile)
            .signature(getCacheSignature())
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
     * Invalidate image cache.
     * Call this when media files change or user changes media path.
     */
    fun invalidateCache() {
        android.util.Log.d(TAG, "Invalidating image cache")
        cacheVersion++
        prefsManager.lastImageCacheVersion = cacheVersion

        // Clear Glide memory cache
        Glide.get(context).clearMemory()

        // Clear disk cache on background thread
        Thread {
            Glide.get(context).clearDiskCache()
        }.start()
    }

    /**
     * Get current cache signature for Glide.
     */
    private fun getCacheSignature(): com.bumptech.glide.signature.ObjectKey {
        return com.bumptech.glide.signature.ObjectKey(cacheVersion.toString())
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
     * Apply blur effect to bitmap using RenderScript.
     */
    private fun applyBlur(bitmap: Bitmap, blurRadius: Int): Bitmap {
        val output = Bitmap.createBitmap(bitmap)

        val input = Allocation.createFromBitmap(renderScript, bitmap)
        val outputAlloc = Allocation.createFromBitmap(renderScript, output)

        val script = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript))
        script.setRadius(blurRadius.coerceIn(1, 25).toFloat())
        script.setInput(input)
        script.forEach(outputAlloc)

        outputAlloc.copyTo(output)

        input.destroy()
        outputAlloc.destroy()
        script.destroy()

        return output
    }

    /**
     * Cleanup resources.
     */
    fun cleanup() {
        renderScript.destroy()
    }
}

/**
 * Custom blur transformation for Glide.
 */
class BlurTransformation(
    private val context: Context,
    private val blurRadius: Int
) : com.bumptech.glide.load.resource.bitmap.BitmapTransformation() {

    override fun transform(
        pool: com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        val renderScript = RenderScript.create(context)

        val output = Bitmap.createBitmap(toTransform)
        val input = Allocation.createFromBitmap(renderScript, toTransform)
        val outputAlloc = Allocation.createFromBitmap(renderScript, output)

        val script = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript))
        script.setRadius(blurRadius.coerceIn(1, 25).toFloat())
        script.setInput(input)
        script.forEach(outputAlloc)

        outputAlloc.copyTo(output)

        input.destroy()
        outputAlloc.destroy()
        script.destroy()
        renderScript.destroy()

        return output
    }

    override fun updateDiskCacheKey(messageDigest: java.security.MessageDigest) {
        messageDigest.update("blur_$blurRadius".toByteArray())
    }
}