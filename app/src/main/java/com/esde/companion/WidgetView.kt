package com.esde.companion

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.view.MotionEvent
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import com.bumptech.glide.Glide
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * ScrollView that never intercepts touch events - only auto-scrolls programmatically
 */
class AutoScrollOnlyView(context: Context) : android.widget.ScrollView(context) {
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
    val widget: OverlayWidget,
    private val onDelete: (WidgetView) -> Unit,
    private val onUpdate: (OverlayWidget) -> Unit
) : RelativeLayout(context) {

    private val imageView: ImageView
    private val textView: android.widget.TextView
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
        // Create scroll view for text (will be hidden for image widgets)
        scrollView = AutoScrollOnlyView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
            visibility = View.GONE
            isVerticalScrollBarEnabled = false  // Hide scrollbar for cleaner look

            // NEW: Completely disable all touch interaction - auto-scroll only
            isClickable = false
            isFocusable = false
            isFocusableInTouchMode = false

            // Never intercept touch events
            setOnTouchListener { _, _ -> false }
        }

        // Also make TextView non-interactive
        textView = android.widget.TextView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
            textSize = 16f
            setTextColor(android.graphics.Color.WHITE)
            setPadding(20, 20, 20, 20)
            setBackgroundColor(android.graphics.Color.parseColor("#66000000"))

            // Make completely non-interactive
            isClickable = false
            isFocusable = false
            isFocusableInTouchMode = false
        }
        scrollView.addView(textView)

        // Create ImageView for the widget content
        imageView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER  // Keeps image centered and scaled
            // Remove any max dimensions that might constrain scaling
            maxHeight = Int.MAX_VALUE
            maxWidth = Int.MAX_VALUE
        }

        // Add both views (only one will be visible at a time)
        addView(scrollView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(imageView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        val buttonSize = (handleSize * 1.2f).toInt()
        val buttonSpacing = 10  // Space between buttons

        // Create a container for the buttons at the top center
        val buttonContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
        }
        val containerParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        containerParams.addRule(ALIGN_PARENT_TOP)
        containerParams.addRule(CENTER_HORIZONTAL)
        containerParams.topMargin = 0

        // Create settings button (cog icon)
        settingsButton = ImageButton(context).apply {
            setImageResource(android.R.drawable.ic_menu_preferences)
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
            setImageResource(android.R.drawable.ic_menu_delete)
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

        android.util.Log.d("WidgetView", "Settings and delete buttons created in container")

        // Make this view clickable and focusable
        isClickable = true
        isFocusable = true

        // Load image based on widget data
        loadWidgetImage()

        // Set initial position and size
        updateLayout()

        // Enable drawing for border and handles
        setWillNotDraw(false)
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

        // Only draw border and handles when selected (and not locked)
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
                        return true
                    }
                }

                // Not touching handle - this is a drag
                isDragging = true
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.rawX - dragStartX
                val deltaY = event.rawY - dragStartY

                // CHANGED: Request parent disallow immediately when resizing starts
                if (isResizing) {
                    parent.requestDisallowInterceptTouchEvent(true)
                }

                // CHANGED: Lower threshold for detecting movement
                if (abs(deltaX) > 5 || abs(deltaY) > 5) {  // Reduced from 10 to 5
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
                val wasMoved = abs(deltaX) > 5 || abs(deltaY) > 5

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

                // Save widget state
                onUpdate(widget)
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
        if (widget.imageType == OverlayWidget.ImageType.GAME_DESCRIPTION) {
            imageView.visibility = View.GONE

            val description = widget.imagePath
            if (description.isNotEmpty()) {
                // Show scrollView with background when there's text
                scrollView.visibility = View.VISIBLE
                textView.text = description
                textView.setBackgroundColor(android.graphics.Color.parseColor("#4D000000"))  // Show background
                android.util.Log.d("WidgetView", "Game description loaded: ${description.take(100)}...")

                // Start auto-scrolling after a short delay
                postDelayed({
                    startAutoScroll()
                }, 2000)
            } else {
                // Hide scrollView completely when there's no text
                scrollView.visibility = View.GONE
                textView.text = ""
                android.util.Log.d("WidgetView", "No description available - hiding widget")
            }
            return
        }

        // Existing code for image widgets - hide text view, show image view
        scrollView.visibility = View.GONE
        imageView.visibility = View.VISIBLE

        if (widget.imagePath.isEmpty()) {
            // Only show text fallback for MARQUEE type
            if (widget.imageType == OverlayWidget.ImageType.MARQUEE) {
                android.util.Log.d("WidgetView", "Empty marquee image path, showing text fallback")
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
                    android.util.Log.d("WidgetView", "Marquee text fallback displayed: $displayText")
                } else {
                    Glide.with(context).clear(imageView)
                    imageView.setImageDrawable(null)
                }
            } else {
                // For non-marquee types, just clear the image
                android.util.Log.d("WidgetView", "Empty image path for non-marquee, clearing image")
                Glide.with(context).clear(imageView)
                imageView.setImageDrawable(null)
            }
            return
        }

        if (widget.imagePath.startsWith("builtin://")) {
            // Load built-in system logo from assets
            val systemName = widget.imagePath.removePrefix("builtin://")
            android.util.Log.d("WidgetView", "Loading built-in system logo for: $systemName")

            val mainActivity = context as? MainActivity
            if (mainActivity != null) {
                // Pass widget dimensions to properly scale SVG
                val drawable = mainActivity.loadSystemLogoFromAssets(
                    systemName,
                    widget.width.toInt(),
                    widget.height.toInt()
                )
                if (drawable != null) {
                    imageView.scaleType = ImageView.ScaleType.FIT_CENTER
                    imageView.setImageDrawable(drawable)
                    android.util.Log.d("WidgetView", "Built-in system logo loaded successfully")
                } else {
                    android.util.Log.e("WidgetView", "Failed to load built-in system logo")
                    imageView.setImageDrawable(null)
                }
            }
        } else {
            // Load from file (custom logo path)
            val file = File(widget.imagePath)
            if (file.exists()) {
                // ✨ UPDATED SECTION - Better scaling for images
                Glide.with(context)
                    .load(file)
                    .override(widget.width.toInt(), widget.height.toInt())  // ✅ Scale to container size
                    .fitCenter()
                    .into(imageView)
                android.util.Log.d("WidgetView", "Loaded custom logo file with full scaling: ${widget.imagePath}")
            } else {
                // Only show text fallback for MARQUEE type
                if (widget.imageType == OverlayWidget.ImageType.MARQUEE) {
                    android.util.Log.d("WidgetView", "Marquee file doesn't exist: ${widget.imagePath}, showing text fallback")
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
                        android.util.Log.d("WidgetView", "Marquee text fallback displayed for missing file: $displayText")
                    } else {
                        Glide.with(context).clear(imageView)
                        imageView.setImageDrawable(null)
                    }
                } else {
                    // For non-marquee types, just clear the image
                    android.util.Log.d("WidgetView", "Logo file doesn't exist: ${widget.imagePath}, clearing image")
                    Glide.with(context).clear(imageView)
                    imageView.setImageDrawable(null)
                }
            }
        }

        // At the very end of loadWidgetImage() method
        postDelayed({
            android.util.Log.d("WidgetView", "═══ DIAGNOSTIC INFO ═══")
            android.util.Log.d("WidgetView", "Widget container: ${width}x${height}")
            android.util.Log.d("WidgetView", "Widget data size: ${widget.width}x${widget.height}")
            android.util.Log.d("WidgetView", "ImageView size: ${imageView.width}x${imageView.height}")
            android.util.Log.d("WidgetView", "ImageView scaleType: ${imageView.scaleType}")
            android.util.Log.d("WidgetView", "ImageView layoutParams: ${imageView.layoutParams.width}x${imageView.layoutParams.height}")

            val drawable = imageView.drawable
            if (drawable != null) {
                android.util.Log.d("WidgetView", "Drawable intrinsic: ${drawable.intrinsicWidth}x${drawable.intrinsicHeight}")
                android.util.Log.d("WidgetView", "Drawable bounds: ${drawable.bounds}")
            }
            android.util.Log.d("WidgetView", "═══ END DIAGNOSTIC ═══")
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
        android.util.Log.d("WidgetView", "updateDeleteButtonVisibility: shouldShow=$shouldShow, isWidgetSelected=$isWidgetSelected, isLocked=$isLocked")

        deleteButton.visibility = if (shouldShow) VISIBLE else GONE
        settingsButton.visibility = if (shouldShow) VISIBLE else GONE

        android.util.Log.d("WidgetView", "Delete button visibility: ${deleteButton.visibility}, Settings button visibility: ${settingsButton.visibility}")
    }

    private fun showDeleteDialog() {
        android.app.AlertDialog.Builder(context)
            .setTitle("Delete Widget")
            .setMessage("Remove this overlay widget?")
            .setPositiveButton("Delete") { _, _ ->
                onDelete(this)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLayerMenu() {
        val options = arrayOf(
            "Move Forward",
            "Move Backward",
            "─────────────",
            "Delete"
        )

        // Get the widget name from imagePath since we can't directly access ImageType here
        // Parse the builtin path or filename to determine widget type
        val widgetName = when {
            widget.imageType == OverlayWidget.ImageType.GAME_DESCRIPTION -> "Game Description"

            widget.imagePath.contains("marquees", ignoreCase = true) -> "Marquee"
            widget.imagePath.contains("covers", ignoreCase = true) -> "2D Box"
            widget.imagePath.contains("3dboxes", ignoreCase = true) -> "3D Box"
            widget.imagePath.contains("miximages", ignoreCase = true) -> "Mix Image"
            widget.imagePath.contains("backcovers", ignoreCase = true) -> "Back Cover"
            widget.imagePath.contains("physicalmedia", ignoreCase = true) -> "Physical Media"
            widget.imagePath.contains("screenshots", ignoreCase = true) -> "Screenshot"
            widget.imagePath.contains("fanart", ignoreCase = true) -> "Fanart"
            widget.imagePath.contains("titlescreens", ignoreCase = true) -> "Title Screen"
            widget.imagePath.contains("systemlogo", ignoreCase = true) ||
                    widget.imagePath.contains("system", ignoreCase = true) -> "System Logo"
            widget.imagePath.startsWith("builtin://") -> {
                // Extract name from builtin path
                widget.imagePath.removePrefix("builtin://")
                    .split("/").last()
                    .replace("_", " ")
                    .split(" ")
                    .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            }
            else -> "Widget"
        }

        // Get current zIndex from parent view
        val currentZIndex = (parent as? android.view.ViewGroup)?.indexOfChild(this) ?: 0

        android.app.AlertDialog.Builder(context)
            .setTitle("$widgetName (zIndex: $currentZIndex)")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> {
                        moveWidgetForward()
                        // Reopen the dialog after moving
                        postDelayed({ showLayerMenu() }, 100)
                    }
                    1 -> {
                        moveWidgetBackward()
                        // Reopen the dialog after moving
                        postDelayed({ showLayerMenu() }, 100)
                    }
                    2 -> {} // Separator
                    3 -> showDeleteDialog()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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
        Glide.with(context).clear(imageView)
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