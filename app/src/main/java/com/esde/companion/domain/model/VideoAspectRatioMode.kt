package com.esde.companion.domain.model

/**
 * How game video playback fills the screen - see Settings > UI Settings > Video Playback.
 * Contain scales the video to fit entirely within the view, preserving its aspect ratio
 * with letter/pillarboxing where needed. Cover scales the video to fill the entire view,
 * preserving aspect ratio but cropping whatever doesn't fit. Neither ever stretches/squashes
 * the video off its native aspect ratio.
 */
enum class VideoAspectRatioMode {
    Contain,
    Cover,
}
