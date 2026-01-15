package com.esde.companion

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import kotlin.math.roundToInt

class GridOverlayView(context: Context, private val gridSize: Float) : View(context) {

    private val gridPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = 0x40FFFFFF  // White with 25% opacity
        isAntiAlias = false
    }

    private val majorGridPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = 0x60FFFFFF  // White with 38% opacity
        isAntiAlias = false
    }

    private val centerLinePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = 0x80FFFFFF.toInt()  // ADD .toInt()
        isAntiAlias = false
    }

    init {
        setWillNotDraw(false)
        // Make this view non-interactive so touches pass through
        isClickable = false
        isFocusable = false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        // Snap center to grid so it aligns with grid lines
        val centerX = ((width / 2f) / gridSize).roundToInt() * gridSize
        val centerY = ((height / 2f) / gridSize).roundToInt() * gridSize

        // Draw vertical lines from center outward
        // Lines to the right
        var x = centerX
        var lineCount = 0
        while (x <= width) {
            val paint = when {
                lineCount == 0 -> centerLinePaint  // Center line
                lineCount % 6 == 0 -> majorGridPaint  // Major lines every 6th
                else -> gridPaint  // Regular lines
            }
            canvas.drawLine(x, 0f, x, height, paint)
            x += gridSize
            lineCount++
        }

        // Lines to the left
        x = centerX - gridSize
        lineCount = 1
        while (x >= 0) {
            val paint = if (lineCount % 6 == 0) majorGridPaint else gridPaint
            canvas.drawLine(x, 0f, x, height, paint)
            x -= gridSize
            lineCount++
        }

        // Draw horizontal lines from center outward
        // Lines downward
        var y = centerY
        lineCount = 0
        while (y <= height) {
            val paint = when {
                lineCount == 0 -> centerLinePaint  // Center line
                lineCount % 6 == 0 -> majorGridPaint  // Major lines every 6th
                else -> gridPaint  // Regular lines
            }
            canvas.drawLine(0f, y, width, y, paint)
            y += gridSize
            lineCount++
        }

        // Lines upward
        y = centerY - gridSize
        lineCount = 1
        while (y >= 0) {
            val paint = if (lineCount % 6 == 0) majorGridPaint else gridPaint
            canvas.drawLine(0f, y, width, y, paint)
            y -= gridSize
            lineCount++
        }
    }
}