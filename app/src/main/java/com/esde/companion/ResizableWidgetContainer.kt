package com.esde.companion

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.RelativeLayout  // Changed from FrameLayout
import com.esde.companion.WidgetView

class ResizableWidgetContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {  // Changed from FrameLayout

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // Check if any child widget wants to handle extended touch
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is WidgetView) {
                // Convert touch coordinates to child's coordinate space
                val childX = ev.x - child.left
                val childY = ev.y - child.top

                if (child.isTouchingExtendedCorner(childX, childY)) {
                    // Let the child handle this touch
                    return false
                }
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Forward touches to children that might be in extended zones
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is WidgetView) {
                val childX = event.x - child.left
                val childY = event.y - child.top

                if (child.isTouchingExtendedCorner(childX, childY)) {
                    // Transform event coordinates and dispatch
                    val transformed = MotionEvent.obtain(event)
                    transformed.setLocation(childX, childY)
                    val handled = child.dispatchTouchEvent(transformed)
                    transformed.recycle()
                    return handled
                }
            }
        }
        return super.onTouchEvent(event)
    }
}