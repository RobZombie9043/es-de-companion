package com.esde.companion

import android.graphics.Bitmap
import java.util.UUID

data class OverlayWidget(
    val id: String = UUID.randomUUID().toString(),
    val imageType: ImageType,
    val imagePath: String,
    var x: Float,
    var y: Float,
    var width: Float,
    var height: Float,
    var zIndex: Int = 0,
    val widgetContext: WidgetContext = WidgetContext.GAME
) {
    enum class ImageType {
        MARQUEE,
        BOX_2D,
        BOX_3D,
        MIX_IMAGE,
        BACK_COVER,
        PHYSICAL_MEDIA,
        SCREENSHOT,
        FANART,
        TITLE_SCREEN,
        SYSTEM_LOGO
    }

    enum class WidgetContext {
        GAME,    // Shows in game view
        SYSTEM   // Shows in system view
    }
}