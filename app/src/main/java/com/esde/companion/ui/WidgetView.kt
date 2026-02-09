package com.esde.companion.ui

import android.R
import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.MotionEvent
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import com.esde.companion.MainActivity
import com.esde.companion.data.Widget
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import com.esde.companion.managers.ImageManager
import com.google.android.material.button.MaterialButton

/**
 * ScrollView that never intercepts touch events - only auto-scrolls programmatically
 */
class AutoScrollOnlyView(context: Context) : ScrollView(context) {
    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        // Never intercept - let parent handle all touches
        return false
    }

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        // Never handle touches - only programmatic scrolling allowed
        return false
    }
}
class WidgetView(
    context: Context,
    val widget: Widget,
    private val onDelete: (WidgetView) -> Unit,
    private val onUpdate: (Widget) -> Unit,
    private val imageManager: ImageManager
) : RelativeLayout(context) {

    private val imageView: ImageView
    private val textView: TextView
    private val scrollView: AutoScrollOnlyView  // CHANGED
    private val deleteButton: ImageButton
    private val settingsButton: ImageButton

    private val borderPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = 0xFF4CAF50.toInt()
    }
    private val handlePaint = Paint().apply {
        style = Paint.Style.FILL
        color = 0xFF4CAF50.toInt()
    }
    private val handleSize = 60f
    private val handleHitZone = 200f  // ADDED: Much larger invisible hit area

    private var isDragging = false
    private var isResizing = false
    private var resizeCorner = ResizeCorner.NONE
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var initialX = 0f
    private var initialY = 0f
    private var initialWidth = 0f
    private var initialHeight = 0f

    var isWidgetSelected = false
    private var isLocked = false

    // Snap to grid settings
    private var snapToGrid = false
    private var gridSize = 50f
    private var scrollJob: Runnable? = null
    private val scrollSpeed = 1  // pixels per frame
    private val scrollDelay = 30L  // milliseconds between scroll updates

    enum class ResizeCorner {
        NONE,
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

    init {
        // Create scroll view for text (will be hidden for image widgets)
        scrollView = AutoScrollOnlyView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
            visibility = GONE
            isVerticalScrollBarEnabled = false  // Hide scrollbar for cleaner look

            // NEW: Completely disable all touch interaction - auto-scroll only
            isClickable = false
            isFocusable = false
            isFocusableInTouchMode = false

            // Never intercept touch events
            setOnTouchListener { _, _ -> false }
        }

        // Also make TextView non-interactive
        textView = TextView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(20, 20, 20, 20)
            setBackgroundColor(Color.parseColor("#66000000"))

            // Make completely non-interactive
            isClickable = false
            isFocusable = false
            isFocusableInTouchMode = false
        }
        scrollView.addView(textView)

        // Create ImageView for the widget content
        imageView = ImageView(context).apply {
            // Scale type will be set dynamically in loadWidgetImage()
            // Remove any max dimensions that might constrain scaling
            maxHeight = Int.MAX_VALUE
            maxWidth = Int.MAX_VALUE
        }

        // Add both views (only one will be visible at a time)
        addView(scrollView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(imageView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        // Make sure onDraw (which draws handles) happens after child views
        setWillNotDraw(false)

        val buttonSize = (handleSize * 1.2f).toInt()
        val buttonSpacing = 10  // Space between buttons

        // Create a container for the buttons at the top center
        val buttonContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val containerParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        containerParams.addRule(ALIGN_PARENT_TOP)
        containerParams.addRule(CENTER_HORIZONTAL)
        containerParams.topMargin = 0

        // Create settings button (cog icon)
        settingsButton = ImageButton(context).apply {
            setImageResource(R.drawable.ic_menu_preferences)
            setBackgroundColor(0xFF2196F3.toInt())  // Blue background
            visibility = GONE
            setOnClickListener {
                showLayerMenu()
            }
        }
        val settingsButtonParams = LinearLayout.LayoutParams(buttonSize, buttonSize)
        settingsButtonParams.rightMargin = buttonSpacing / 2
        buttonContainer.addView(settingsButton, settingsButtonParams)

        // Create delete button (trash icon)
        deleteButton = ImageButton(context).apply {
            setImageResource(R.drawable.ic_menu_delete)
            setBackgroundColor(0xFFFF5252.toInt())  // Red background
            visibility = GONE
            setOnClickListener {
                showDeleteDialog()
            }
        }
        val deleteButtonParams = LinearLayout.LayoutParams(buttonSize, buttonSize)
        deleteButtonParams.leftMargin = buttonSpacing / 2
        buttonContainer.addView(deleteButton, deleteButtonParams)

        addView(buttonContainer, containerParams)

        Log.d("WidgetView", "Settings and delete buttons created in container")

        // Make this view clickable but not focusable (touch-only, no D-pad focus border)
        isClickable = true
        isFocusable = false
        isFocusableInTouchMode = false

        // Load image based on widget data
        loadWidgetImage()

        // Set initial position and size
        updateLayout()

        // Apply initial background opacity for Game Description
        if (widget.imageType == Widget.ImageType.GAME_DESCRIPTION) {
            val alpha = (widget.backgroundOpacity * 255).toInt().coerceIn(0, 255)
            scrollView.setBackgroundColor(Color.argb(alpha, 0, 0, 0))
            textView.setBackgroundColor(Color.TRANSPARENT)

            if (alpha == 0) {
                this.setBackgroundColor(Color.TRANSPARENT)
            }
        }
    }

    private fun updateLayout() {
        val params = layoutParams as? LayoutParams ?: LayoutParams(
            widget.width.toInt(),
            widget.height.toInt()
        )
        params.width = widget.width.toInt()
        params.height = widget.height.toInt()
        params.leftMargin = widget.x.toInt()
        params.topMargin = widget.y.toInt()
        layoutParams = params
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Nothing here - we'll use dispatchDraw instead
    }

    override fun dispatchDraw(canvas: Canvas) {
        // First draw all child views (images, etc.)
        super.dispatchDraw(canvas)

        // Then draw border and handles ON TOP when selected (and not locked)
        if (isWidgetSelected && !isLocked) {
            // Draw green border
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), borderPaint)

            // Draw L-shaped corner handles
            val handleLength = handleSize * 1.5f  // CHANGED: Longer handles
            val handleThickness = 16f  // CHANGED: Much thicker (was 8f)

            val handlePaintThick = Paint().apply {
                style = Paint.Style.STROKE
                strokeWidth = handleThickness
                color = 0xFFFFFFFF.toInt()  // CHANGED: White color (was green)
                strokeCap = Paint.Cap.ROUND
            }

            // Top-left corner (⌐ shape)
            canvas.drawLine(0f, handleLength, 0f, 0f, handlePaintThick)  // Vertical
            canvas.drawLine(0f, 0f, handleLength, 0f, handlePaintThick)  // Horizontal

            // Top-right corner (¬ shape)
            canvas.drawLine(width.toFloat(), 0f, width - handleLength, 0f, handlePaintThick)  // Horizontal
            canvas.drawLine(width.toFloat(), 0f, width.toFloat(), handleLength, handlePaintThick)  // Vertical

            // Bottom-left corner (L shape)
            canvas.drawLine(0f, height.toFloat(), 0f, height - handleLength, handlePaintThick)  // Vertical
            canvas.drawLine(0f, height.toFloat(), handleLength, height.toFloat(), handlePaintThick)  // Horizontal

            // Bottom-right corner (⌙ shape)
            canvas.drawLine(width.toFloat(), height - handleLength, width.toFloat(), height.toFloat(), handlePaintThick)  // Vertical
            canvas.drawLine(width - handleLength, height.toFloat(), width.toFloat(), height.toFloat(), handlePaintThick)  // Horizontal
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // If locked, don't allow any interaction
        if (isLocked) {
            return false
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                dragStartX = event.rawX
                dragStartY = event.rawY
                initialX = widget.x
                initialY = widget.y
                initialWidth = widget.width
                initialHeight = widget.height

                // Check if touching any resize handle
                val touchX = event.x
                val touchY = event.y
                if (isWidgetSelected) {
                    resizeCorner = getTouchedResizeCorner(touchX, touchY)
                    if (resizeCorner != ResizeCorner.NONE) {
                        isResizing = true
                        parent.requestDisallowInterceptTouchEvent(true)

                        // ========== START: Block long-press when resizing ==========
                        // Cancel long-press immediately - resizing often involves pauses
                        val mainActivity = context as? MainActivity
                        mainActivity?.cancelLongPress()
                        android.util.Log.d("WidgetView", "Resize handle touched - blocking long-press menu")
                        // ========== END: Block long-press when resizing ==========

                        return true
                    }
                }

                // Not touching handle - this is a drag or selection tap
                // Allow long-press to work normally (menu access for unselected widgets)
                isDragging = true
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.rawX - dragStartX
                val deltaY = event.rawY - dragStartY

                // ========== START: Use Android standard touch slop ==========
                // Declare touchSlop once and use it for both checks
                val touchSlop = android.view.ViewConfiguration.get(context).scaledTouchSlop
                // ========== END: Use Android standard touch slop ==========

                // CHANGED: Request parent disallow immediately when resizing starts
                if (isResizing) {
                    parent.requestDisallowInterceptTouchEvent(true)
                }

                // Use consistent touchSlop threshold for detecting movement
                if (abs(deltaX) > touchSlop || abs(deltaY) > touchSlop) {
                    if (isResizing || (isWidgetSelected && isDragging)) {
                        parent.requestDisallowInterceptTouchEvent(true)
                    }
                }

                if (isResizing) {
                    // Resize based on which corner is being dragged
                    resizeFromCorner(resizeCorner, deltaX, deltaY)
                    updateLayout()
                } else if (isDragging && isWidgetSelected) {
                    // Move the widget only if selected
                    val newX = initialX + deltaX
                    val newY = initialY + deltaY

                    widget.x = if (snapToGrid) snapXToGrid(newX) else newX
                    widget.y = if (snapToGrid) snapYToGrid(newY) else newY
                    updateLayout()
                }

                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent.requestDisallowInterceptTouchEvent(false)

                val deltaX = event.rawX - dragStartX
                val deltaY = event.rawY - dragStartY
                val touchSlop = android.view.ViewConfiguration.get(context).scaledTouchSlop
                val wasMoved = abs(deltaX) > touchSlop || abs(deltaY) > touchSlop
                val wasResized = isResizing  // Track if we were resizing

                // Check for tap (to select/deselect)
                if (!wasMoved && !isResizing) {
                    if (isWidgetSelected) {
                        // Already selected - deselect this one
                        isWidgetSelected = false
                        updateDeleteButtonVisibility()
                        invalidate()
                    } else {
                        // Not selected - deselect all others first, then select this one
                        val mainActivity = context as? MainActivity
                        mainActivity?.deselectAllWidgets()

                        isWidgetSelected = true
                        updateDeleteButtonVisibility()
                        invalidate()
                    }
                }

                // Apply final snap on release if enabled
                if (snapToGrid && (isDragging || isResizing)) {
                    widget.x = snapXToGrid(widget.x)
                    widget.y = snapYToGrid(widget.y)
                    widget.width = snapToGridValue(widget.width)
                    widget.height = snapToGridValue(widget.height)
                    updateLayout()
                }

                isDragging = false
                isResizing = false
                resizeCorner = ResizeCorner.NONE

                // Convert current absolute positions to percentages
                val displayMetrics = context.resources.displayMetrics
                widget.toPercentages(displayMetrics.widthPixels, displayMetrics.heightPixels)

                // Save widget state
                onUpdate(widget)

                // ADDED: Reload image after resize to fit new dimensions
                if (wasResized) {
                    loadWidgetImage()
                }

                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // If locked, don't intercept
        if (isLocked) {
            return false
        }

        // If selected, check if touching extended corner hit zones (including outside bounds)
        if (isWidgetSelected && ev.action == MotionEvent.ACTION_DOWN) {
            val touchX = ev.x
            val touchY = ev.y

            // Check extended hit zones that go outside the widget bounds
            if (isTouchingExtendedCorner(touchX, touchY)) {
                return true  // Intercept this touch
            }
        }

        return false
    }

    private fun loadWidgetImage() {
        // NEW: Handle text-based widgets (game description) FIRST, before checking if path is empty
        if (widget.imageType == Widget.ImageType.GAME_DESCRIPTION) {
            imageView.visibility = GONE

            val description = widget.imagePath
            if (description.isNotEmpty()) {
                // Show scrollView with background when there's text
                scrollView.visibility = VISIBLE
                textView.text = description
                textView.setBackgroundColor(Color.parseColor("#4D000000"))  // Show background
                Log.d("WidgetView", "Game description loaded: ${description.take(100)}...")

                // Start auto-scrolling after a short delay
                postDelayed({
                    startAutoScroll()
                }, 2000)
            } else {
                // Hide scrollView completely when there's no text
                scrollView.visibility = GONE
                textView.text = ""
                Log.d("WidgetView", "No description available - hiding widget")
            }
            return
        }

        // Existing code for image widgets - hide text view, show image view
        scrollView.visibility = GONE
        imageView.visibility = VISIBLE

        if (widget.imagePath.isEmpty()) {
            // Only show text fallback for MARQUEE type
            if (widget.imageType == Widget.ImageType.MARQUEE) {
                Log.d("WidgetView", "Empty marquee image path, showing text fallback")
                val mainActivity = context as? MainActivity
                if (mainActivity != null) {
                    val displayText = extractGameNameFromWidget()
                    val fallbackDrawable = mainActivity.createMarqueeTextFallback(
                        gameName = displayText,
                        width = widget.width.toInt(),
                        height = widget.height.toInt()
                    )
                    imageView.scaleType = ImageView.ScaleType.FIT_CENTER
                    imageView.setImageDrawable(fallbackDrawable)
                    Log.d("WidgetView", "Marquee text fallback displayed: $displayText")
                } else {
                    imageView.setImageDrawable(null)
                }
            } else {
                // For non-marquee types, just clear the image
                Log.d("WidgetView", "Empty image path for non-marquee, clearing image")
                imageView.setImageDrawable(null)
            }
            return
        }

        if (widget.imagePath.startsWith("builtin://")) {
            // Load built-in system logo from assets or custom logos
            val systemName = widget.imagePath.removePrefix("builtin://")
            Log.d("WidgetView", "Loading system logo for: $systemName")

            val mainActivity = context as? MainActivity
            if (mainActivity != null) {
                // First, check if there's a custom logo file
                val logoPath = mainActivity.findSystemLogo(systemName)
                Log.d("WidgetView", "Logo path found: $logoPath")

                if (logoPath != null && !logoPath.startsWith("builtin://")) {
                    // Custom logo exists - check if it's a bitmap format that needs ImageManager
                    val logoFile = File(logoPath)
                    val extension = logoFile.extension.lowercase()

                    if (extension in listOf("png", "jpg", "jpeg", "webp", "gif")) {
                        // Bitmap-based custom logo - use ImageManager for animation support
                        Log.d("WidgetView", "Loading bitmap custom logo via ImageManager: ${logoFile.name}")

                        if (logoFile.exists()) {
                            val effectiveScaleType = widget.scaleType ?: Widget.ScaleType.FIT

                            // Load with ImageManager (handles both animated and static images)
                            imageManager.loadWidgetImage(
                                imageView = imageView,
                                imagePath = logoFile.absolutePath,
                                scaleType = effectiveScaleType,
                                onLoaded = {
                                    Log.d("WidgetView", "Custom logo loaded: ${logoFile.name}")
                                },
                                onFailed = {
                                    Log.w("WidgetView", "Failed to load custom logo: ${logoFile.absolutePath}")
                                }
                            )
                        } else {
                            Log.e("WidgetView", "Custom logo file not found: $logoPath")
                            imageView.setImageDrawable(null)
                        }
                    } else {
                        // SVG custom logo - use loadSystemLogoFromAssets
                        Log.d("WidgetView", "Loading SVG custom logo via drawable")
                        val drawable = mainActivity.loadSystemLogoFromAssets(
                            systemName,
                            widget.width.toInt(),
                            widget.height.toInt()
                        )

                        if (drawable != null) {
                            imageView.scaleType = when (widget.scaleType ?: Widget.ScaleType.FIT) {
                                Widget.ScaleType.FIT -> ImageView.ScaleType.FIT_CENTER
                                Widget.ScaleType.CROP -> ImageView.ScaleType.CENTER_CROP
                            }
                            imageView.setImageDrawable(drawable)
                            Log.d("WidgetView", "SVG custom logo loaded successfully")
                        } else {
                            Log.e("WidgetView", "Failed to load SVG custom logo")
                            imageView.setImageDrawable(null)
                        }
                    }
                    // ========== START: Prevent fall-through to generic file loading ==========
                    return  // Exit after handling custom logo
                    // ========== END: Prevent fall-through ==========
                } else {
                    // No custom logo - use built-in SVG from assets
                    Log.d("WidgetView", "Loading built-in SVG logo from assets")
                    val drawable = mainActivity.loadSystemLogoFromAssets(
                        systemName,
                        widget.width.toInt(),
                        widget.height.toInt()
                    )

                    if (drawable != null) {
                        imageView.scaleType = when (widget.scaleType ?: Widget.ScaleType.FIT) {
                            Widget.ScaleType.FIT -> ImageView.ScaleType.FIT_CENTER
                            Widget.ScaleType.CROP -> ImageView.ScaleType.CENTER_CROP
                        }
                        imageView.setImageDrawable(drawable)
                        Log.d("WidgetView", "Built-in SVG logo loaded successfully")
                    } else {
                        Log.e("WidgetView", "Failed to load built-in logo")
                        imageView.setImageDrawable(null)
                    }
                    // ========== START: Prevent fall-through to generic file loading ==========
                    return  // Exit after handling built-in logo
                    // ========== END: Prevent fall-through ==========
                }
            }
            // ========== START: Prevent fall-through if MainActivity is null ==========
            return  // Exit after handling system logo (even if MainActivity was null)
            // ========== END: Prevent fall-through ==========
        } else {
            // Load from file (custom logo path)
            val file = File(widget.imagePath)
            if (file.exists()) {
                // Set scale type based on widget preference (handle null for migration)
                val effectiveScaleType = widget.scaleType ?: Widget.ScaleType.FIT

                // Load with ImageManager (handles animated and static images)
                imageManager.loadWidgetImage(
                    imageView = imageView,
                    imagePath = file.absolutePath,
                    scaleType = effectiveScaleType,
                    onLoaded = {
                        Log.d("WidgetView", "Loaded custom logo file: ${widget.imagePath}")
                    },
                    onFailed = {
                        Log.w("WidgetView", "Failed to load custom logo file: ${widget.imagePath}")
                    }
                )
            } else {
                // Only show text fallback for MARQUEE type
                if (widget.imageType == Widget.ImageType.MARQUEE) {
                    Log.d("WidgetView", "Marquee file doesn't exist: ${widget.imagePath}, showing text fallback")
                    val mainActivity = context as? MainActivity
                    if (mainActivity != null) {
                        val displayText = extractGameNameFromWidget()
                        val fallbackDrawable = mainActivity.createMarqueeTextFallback(
                            gameName = displayText,
                            width = widget.width.toInt(),
                            height = widget.height.toInt()
                        )
                        // Set scale type based on widget preference (handle null for migration)
                        imageView.scaleType = when (widget.scaleType ?: Widget.ScaleType.FIT) {
                            Widget.ScaleType.FIT -> ImageView.ScaleType.FIT_CENTER
                            Widget.ScaleType.CROP -> ImageView.ScaleType.CENTER_CROP
                        }
                        imageView.setImageDrawable(fallbackDrawable)
                        Log.d("WidgetView", "Marquee text fallback displayed for missing file: $displayText")
                    } else {
                        imageView.setImageDrawable(null)
                    }
                } else {
                    // For non-marquee types, just clear the image
                    Log.d("WidgetView", "Logo file doesn't exist: ${widget.imagePath}, clearing image")
                    imageView.setImageDrawable(null)
                }
            }
        }

        // At the very end of loadWidgetImage() method
        postDelayed({
            Log.d("WidgetView", "═══ DIAGNOSTIC INFO ═══")
            Log.d("WidgetView", "Widget container: ${width}x${height}")
            Log.d("WidgetView", "Widget data size: ${widget.width}x${widget.height}")
            Log.d("WidgetView", "ImageView size: ${imageView.width}x${imageView.height}")
            Log.d("WidgetView", "ImageView scaleType: ${imageView.scaleType}")
            Log.d("WidgetView", "ImageView layoutParams: ${imageView.layoutParams.width}x${imageView.layoutParams.height}")

            val drawable = imageView.drawable
            if (drawable != null) {
                Log.d("WidgetView", "Drawable intrinsic: ${drawable.intrinsicWidth}x${drawable.intrinsicHeight}")
                Log.d("WidgetView", "Drawable bounds: ${drawable.bounds}")
            }
            Log.d("WidgetView", "═══ END DIAGNOSTIC ═══")
        }, 100)
    }

    private fun extractGameNameFromWidget(): String {
        // Try to extract game name from widget ID or fallback to "Marquee"
        return when {
            widget.id.isNotEmpty() && widget.id != "widget_${widget.imageType}" -> {
                // Widget ID might contain game name
                widget.id.replace("widget_", "")
                    .replace("_", " ")
                    .trim()
            }
            else -> "Marquee"
        }
    }

    fun isTouchingExtendedCorner(x: Float, y: Float): Boolean {
        val extend = handleHitZone / 2  // Half the hit zone extends outside

        // Top-left extended zone
        if (x >= -extend && x <= handleHitZone - extend &&
            y >= -extend && y <= handleHitZone - extend) {
            return true
        }
        // Top-right extended zone
        if (x >= width - handleHitZone + extend && x <= width + extend &&
            y >= -extend && y <= handleHitZone - extend) {
            return true
        }
        // Bottom-left extended zone
        if (x >= -extend && x <= handleHitZone - extend &&
            y >= height - handleHitZone + extend && y <= height + extend) {
            return true
        }
        // Bottom-right extended zone
        if (x >= width - handleHitZone + extend && x <= width + extend &&
            y >= height - handleHitZone + extend && y <= height + extend) {
            return true
        }

        return false
    }

    private fun getTouchedResizeCorner(x: Float, y: Float): ResizeCorner {
        val extend = handleHitZone / 2  // Half extends outside

        // Check top-left (extended outside)
        if (x >= -extend && x <= handleHitZone - extend &&
            y >= -extend && y <= handleHitZone - extend) {
            return ResizeCorner.TOP_LEFT
        }
        // Check top-right (extended outside)
        if (x >= width - handleHitZone + extend && x <= width + extend &&
            y >= -extend && y <= handleHitZone - extend) {
            return ResizeCorner.TOP_RIGHT
        }
        // Check bottom-left (extended outside)
        if (x >= -extend && x <= handleHitZone - extend &&
            y >= height - handleHitZone + extend && y <= height + extend) {
            return ResizeCorner.BOTTOM_LEFT
        }
        // Check bottom-right (extended outside)
        if (x >= width - handleHitZone + extend && x <= width + extend &&
            y >= height - handleHitZone + extend && y <= height + extend) {
            return ResizeCorner.BOTTOM_RIGHT
        }

        return ResizeCorner.NONE
    }

    private fun resizeFromCorner(corner: ResizeCorner, deltaX: Float, deltaY: Float) {
        when (corner) {
            ResizeCorner.TOP_LEFT -> {
                val newWidth = max(100f, initialWidth - deltaX)
                val newHeight = max(100f, initialHeight - deltaY)
                val widthDiff = initialWidth - newWidth
                val heightDiff = initialHeight - newHeight

                widget.width = if (snapToGrid) snapToGridValue(newWidth) else newWidth
                widget.height = if (snapToGrid) snapToGridValue(newHeight) else newHeight
                widget.x = if (snapToGrid) snapXToGrid(initialX + widthDiff) else initialX + widthDiff
                widget.y = if (snapToGrid) snapYToGrid(initialY + heightDiff) else initialY + heightDiff
            }
            ResizeCorner.TOP_RIGHT -> {
                val newWidth = max(100f, initialWidth + deltaX)
                val newHeight = max(100f, initialHeight - deltaY)
                val heightDiff = initialHeight - newHeight

                widget.width = if (snapToGrid) snapToGridValue(newWidth) else newWidth
                widget.height = if (snapToGrid) snapToGridValue(newHeight) else newHeight
                widget.y = if (snapToGrid) snapYToGrid(initialY + heightDiff) else initialY + heightDiff
            }
            ResizeCorner.BOTTOM_LEFT -> {
                val newWidth = max(100f, initialWidth - deltaX)
                val newHeight = max(100f, initialHeight + deltaY)
                val widthDiff = initialWidth - newWidth

                widget.width = if (snapToGrid) snapToGridValue(newWidth) else newWidth
                widget.height = if (snapToGrid) snapToGridValue(newHeight) else newHeight
                widget.x = if (snapToGrid) snapXToGrid(initialX + widthDiff) else initialX + widthDiff
            }
            ResizeCorner.BOTTOM_RIGHT -> {
                val newWidth = max(100f, initialWidth + deltaX)
                val newHeight = max(100f, initialHeight + deltaY)

                widget.width = if (snapToGrid) snapToGridValue(newWidth) else newWidth
                widget.height = if (snapToGrid) snapToGridValue(newHeight) else newHeight
            }
            ResizeCorner.NONE -> {}
        }
    }

    private fun snapXToGrid(x: Float): Float {
        val displayMetrics = context.resources.displayMetrics
        val screenCenterX = displayMetrics.widthPixels / 2f
        // Snap the center itself to grid
        val snappedCenterX = (screenCenterX / gridSize).roundToInt() * gridSize
        val distanceFromCenter = x - snappedCenterX
        val snappedDistance = (distanceFromCenter / gridSize).roundToInt() * gridSize
        return snappedCenterX + snappedDistance
    }

    private fun snapYToGrid(y: Float): Float {
        val displayMetrics = context.resources.displayMetrics
        val screenCenterY = displayMetrics.heightPixels / 2f
        // Snap the center itself to grid
        val snappedCenterY = (screenCenterY / gridSize).roundToInt() * gridSize
        val distanceFromCenter = y - snappedCenterY
        val snappedDistance = (distanceFromCenter / gridSize).roundToInt() * gridSize
        return snappedCenterY + snappedDistance
    }

    fun setLocked(locked: Boolean) {
        isLocked = locked
        if (locked) {
            isWidgetSelected = false
            updateDeleteButtonVisibility()
        }
        invalidate()
    }

    fun setBackgroundOpacity(opacity: Float) {
        Log.d("WidgetView", "setBackgroundOpacity called for widget type: ${widget.imageType}, opacity: $opacity")

        // Only apply to THIS widget if it's a Game Description
        if (widget.imageType != Widget.ImageType.GAME_DESCRIPTION) {
            Log.d("WidgetView", "Not a Game Description widget, ignoring opacity change")
            return
        }

        widget.backgroundOpacity = opacity

        // Apply opacity to the text background
        val alpha = (opacity * 255).toInt().coerceIn(0, 255)

        // Set background on scrollView
        scrollView.setBackgroundColor(Color.argb(alpha, 0, 0, 0))
        textView.setBackgroundColor(Color.TRANSPARENT)

        // Also set the parent container background if needed
        if (alpha == 0) {
            // At 0%, make the container transparent (but only for this specific widget view)
            this.setBackgroundColor(Color.TRANSPARENT)
        }

        Log.d("WidgetView", "About to save all widgets")

        // ========== START: Update percentages before saving ==========
        val displayMetrics = context.resources.displayMetrics
        widget.toPercentages(displayMetrics.widthPixels, displayMetrics.heightPixels)
        // ========== END: Update percentages before saving ==========

        // Save all widgets
        val mainActivity = context as? MainActivity
        mainActivity?.saveAllWidgets()
    }

    private fun snapToGridValue(value: Float): Float {
        return (value / gridSize).roundToInt() * gridSize
    }

    fun setSnapToGrid(snap: Boolean, size: Float) {
        snapToGrid = snap
        gridSize = size

        if (snap) {
            widget.x = snapXToGrid(widget.x)
            widget.y = snapYToGrid(widget.y)
            widget.width = snapToGridValue(widget.width)
            widget.height = snapToGridValue(widget.height)
            updateLayout()
            onUpdate(widget)
        }
    }

    fun deselect() {
        isWidgetSelected = false
        updateDeleteButtonVisibility()
        invalidate()
    }

    private fun updateDeleteButtonVisibility() {
        val shouldShow = isWidgetSelected && !isLocked
        Log.d("WidgetView", "updateDeleteButtonVisibility: shouldShow=$shouldShow, isWidgetSelected=$isWidgetSelected, isLocked=$isLocked")

        deleteButton.visibility = if (shouldShow) VISIBLE else GONE
        settingsButton.visibility = if (shouldShow) VISIBLE else GONE

        Log.d("WidgetView", "Delete button visibility: ${deleteButton.visibility}, Settings button visibility: ${settingsButton.visibility}")
    }

    private fun showDeleteDialog() {
        AlertDialog.Builder(context)
            .setTitle("Delete Widget")
            .setMessage("Remove this overlay widget?")
            .setPositiveButton("Delete") { _, _ ->
                onDelete(this)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLayerMenu() {
        val widgetName = when (widget.imageType) {
            Widget.ImageType.MARQUEE -> "Marquee"
            Widget.ImageType.BOX_2D -> "2D Box"
            Widget.ImageType.BOX_3D -> "3D Box"
            Widget.ImageType.MIX_IMAGE -> "Mix Image"
            Widget.ImageType.BACK_COVER -> "Back Cover"
            Widget.ImageType.PHYSICAL_MEDIA -> "Physical Media"
            Widget.ImageType.SCREENSHOT -> "Screenshot"
            Widget.ImageType.FANART -> "Fanart"
            Widget.ImageType.TITLE_SCREEN -> "Title Screen"
            Widget.ImageType.GAME_DESCRIPTION -> "Game Description"
            Widget.ImageType.SYSTEM_LOGO -> "System Logo"
        }

        // Inflate the custom dialog view
        val dialogView = LayoutInflater.from(context)
            .inflate(com.esde.companion.R.layout.dialog_widget_settings, null)

        // Get references to views
        val dialogWidgetName = dialogView.findViewById<TextView>(com.esde.companion.R.id.dialogWidgetName)
        val dialogWidgetZIndex = dialogView.findViewById<TextView>(com.esde.companion.R.id.dialogWidgetZIndex)
        val btnMoveForward = dialogView.findViewById<MaterialButton>(com.esde.companion.R.id.btnMoveForward)
        val btnMoveBackward = dialogView.findViewById<MaterialButton>(com.esde.companion.R.id.btnMoveBackward)
        val btnDeleteWidget = dialogView.findViewById<MaterialButton>(com.esde.companion.R.id.btnDeleteWidget)

        // Get opacity control references
        val opacityControlSection = dialogView.findViewById<LinearLayout>(com.esde.companion.R.id.opacityControlSection)
        val opacitySeekBar = dialogView.findViewById<SeekBar>(com.esde.companion.R.id.opacitySeekBar)
        val opacityText = dialogView.findViewById<TextView>(com.esde.companion.R.id.opacityText)

        // Scale type control references
        val scaleTypeControlSection = dialogView.findViewById<LinearLayout>(com.esde.companion.R.id.scaleTypeControlSection)
        val scaleTypeDivider = dialogView.findViewById<View>(com.esde.companion.R.id.scaleTypeDivider)
        val btnScaleFit = dialogView.findViewById<MaterialButton>(com.esde.companion.R.id.btnScaleFit)
        val btnScaleCrop = dialogView.findViewById<MaterialButton>(com.esde.companion.R.id.btnScaleCrop)

        // Set widget name (without zIndex)
        dialogWidgetName.text = widgetName

        // Set zIndex info below Layer Controls
        val currentZIndex = widget.zIndex
        dialogWidgetZIndex.text = "Current zIndex: $currentZIndex"

        // Show scale type control for all image widgets (NOT for Game Description)
        if (widget.imageType != Widget.ImageType.GAME_DESCRIPTION) {
            scaleTypeControlSection.visibility = VISIBLE
            scaleTypeDivider.visibility = VISIBLE

            // Update button styles based on current scale type (handle null for migration)
            fun updateScaleTypeButtons() {
                val currentScaleType = widget.scaleType ?: Widget.ScaleType.FIT
                if (currentScaleType == Widget.ScaleType.FIT) {
                    btnScaleFit.backgroundTintList = ColorStateList.valueOf(0xFF03DAC6.toInt())
                    btnScaleCrop.backgroundTintList = ColorStateList.valueOf(0xFF666666.toInt())
                } else {
                    btnScaleFit.backgroundTintList = ColorStateList.valueOf(0xFF666666.toInt())
                    btnScaleCrop.backgroundTintList = ColorStateList.valueOf(0xFF03DAC6.toInt())
                }
            }

            updateScaleTypeButtons()

            // Scale type button listeners
            btnScaleFit.setOnClickListener {
                widget.scaleType = Widget.ScaleType.FIT
                updateScaleTypeButtons()
                loadWidgetImage()  // Reload image with new scale type
                onUpdate(widget)   // Save the change
            }

            btnScaleCrop.setOnClickListener {
                widget.scaleType = Widget.ScaleType.CROP
                updateScaleTypeButtons()
                loadWidgetImage()  // Reload image with new scale type
                onUpdate(widget)   // Save the change
            }
        } else {
            scaleTypeControlSection.visibility = GONE
            scaleTypeDivider.visibility = GONE
        }

        // Show opacity control only for Game Description
        if (widget.imageType == Widget.ImageType.GAME_DESCRIPTION) {
            opacityControlSection.visibility = VISIBLE

            // Set initial opacity value (convert from 0.0-1.0 to 0-20 steps)
            val currentStep = (widget.backgroundOpacity * 20).toInt()
            opacitySeekBar.progress = currentStep
            val currentOpacity = currentStep * 5
            opacityText.text = "$currentOpacity%"

            // Opacity slider listener (5% increments)
            opacitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val opacityPercent = progress * 5  // Convert step to percentage
                    opacityText.text = "$opacityPercent%"
                    val opacity = progress / 20f  // Convert step to 0.0-1.0 range
                    setBackgroundOpacity(opacity)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        } else {
            opacityControlSection.visibility = GONE
        }

        // Create the dialog
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Button click listeners
        btnMoveForward.setOnClickListener {
            moveWidgetForward()
            dialog.dismiss()
            // Reopen the dialog after a short delay to show updated zIndex
            postDelayed({ showLayerMenu() }, 100)
        }

        btnMoveBackward.setOnClickListener {
            moveWidgetBackward()
            dialog.dismiss()
            // Reopen the dialog after a short delay to show updated zIndex
            postDelayed({ showLayerMenu() }, 100)
        }

        btnDeleteWidget.setOnClickListener {
            dialog.dismiss()
            showDeleteDialog()
        }

        dialog.show()
    }

    private fun moveWidgetForward() {  // CHANGED name
        val mainActivity = context as? MainActivity
        mainActivity?.moveWidgetForward(this)
    }

    private fun moveWidgetBackward() {  // CHANGED name
        val mainActivity = context as? MainActivity
        mainActivity?.moveWidgetBackward(this)
    }

    fun clearImage() {
        imageView.setImageDrawable(null)
    }

    private fun startAutoScroll() {
        stopAutoScroll()  // Stop any existing scroll

        scrollJob = object : Runnable {
            override fun run() {
                val maxScroll = textView.height - scrollView.height
                if (maxScroll > 0) {
                    val currentScroll = scrollView.scrollY

                    // Scroll down
                    if (currentScroll < maxScroll) {
                        scrollView.scrollTo(0, currentScroll + scrollSpeed)
                        postDelayed(this, scrollDelay)
                    } else {
                        // Reached bottom, pause then reset
                        postDelayed({
                            scrollView.scrollTo(0, 0)
                            // Restart scrolling after pause
                            postDelayed(this, 2000)
                        }, 2000)
                    }
                }
            }
        }

        post(scrollJob!!)
    }

    private fun stopAutoScroll() {
        scrollJob?.let {
            removeCallbacks(it)
            scrollJob = null
        }
    }

    // Update onDetachedFromWindow to stop scrolling
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAutoScroll()
    }
}