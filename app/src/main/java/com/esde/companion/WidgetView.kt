package com.esde.companion

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import com.bumptech.glide.Glide
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class WidgetView(
    context: Context,
    val widget: OverlayWidget,
    private val onDelete: (WidgetView) -> Unit,
    private val onUpdate: (OverlayWidget) -> Unit
) : RelativeLayout(context) {

    private val imageView: ImageView
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

    private var isDragging = false
    private var isResizing = false
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var initialX = 0f
    private var initialY = 0f
    private var initialWidth = 0f
    private var initialHeight = 0f

    private var isSelected = false
    private var longPressStartTime = 0L
    private val longPressDuration = 500L

    init {
        // Create ImageView for the widget content
        imageView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
        }
        addView(imageView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

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

    private fun loadWidgetImage() {
        val file = File(widget.imagePath)
        if (file.exists()) {
            Glide.with(context)
                .load(file)
                .into(imageView)
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

        // Only draw border and handles when selected
        if (isSelected) {
            // Draw border
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), borderPaint)

            // Draw resize handle (bottom-right corner) - outer circle
            canvas.drawCircle(
                width - handleSize / 2,
                height - handleSize / 2,
                handleSize / 2,
                handlePaint
            )

            // Draw inner circle for better visibility
            val innerPaint = Paint().apply {
                style = Paint.Style.FILL
                color = 0xFFFFFFFF.toInt()
            }
            canvas.drawCircle(
                width - handleSize / 2,
                height - handleSize / 2,
                handleSize / 4,
                innerPaint
            )
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                longPressStartTime = System.currentTimeMillis()
                dragStartX = event.rawX
                dragStartY = event.rawY
                initialX = widget.x
                initialY = widget.y
                initialWidth = widget.width
                initialHeight = widget.height

                // Check if touching resize handle
                val touchX = event.x
                val touchY = event.y
                if (isSelected && isTouchingResizeHandle(touchX, touchY)) {
                    isResizing = true
                } else {
                    isDragging = true
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.rawX - dragStartX
                val deltaY = event.rawY - dragStartY

                if (isResizing) {
                    // Resize the widget
                    widget.width = max(100f, initialWidth + deltaX)
                    widget.height = max(100f, initialHeight + deltaY)
                    updateLayout()
                } else if (isDragging) {
                    // Move the widget
                    widget.x = initialX + deltaX
                    widget.y = initialY + deltaY
                    updateLayout()
                }

                // Cancel long press if moved significantly
                if (abs(deltaX) > 10 || abs(deltaY) > 10) {
                    longPressStartTime = 0
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // Check for long press (to show delete option)
                val pressDuration = System.currentTimeMillis() - longPressStartTime
                val deltaX = event.rawX - dragStartX
                val deltaY = event.rawY - dragStartY
                val wasMoved = abs(deltaX) > 10 || abs(deltaY) > 10

                if (pressDuration >= longPressDuration && !wasMoved) {
                    showDeleteDialog()
                    isDragging = false
                    isResizing = false
                    return true
                }

                // Check for tap (to select/deselect)
                if (!wasMoved && pressDuration < longPressDuration) {
                    isSelected = !isSelected
                    invalidate()
                }

                isDragging = false
                isResizing = false

                // Save widget state
                onUpdate(widget)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun isTouchingResizeHandle(x: Float, y: Float): Boolean {
        val handleCenterX = width - handleSize / 2
        val handleCenterY = height - handleSize / 2
        val distance = Math.sqrt(
            Math.pow((x - handleCenterX).toDouble(), 2.0) +
                    Math.pow((y - handleCenterY).toDouble(), 2.0)
        )
        return distance <= handleSize
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

    fun deselect() {
        isSelected = false
        invalidate()
    }
}