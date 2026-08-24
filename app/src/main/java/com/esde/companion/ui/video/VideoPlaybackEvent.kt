package com.esde.companion.ui.video

/**
 * Reported via WidgetVideoContent's `onPlaybackEvent` - a single sealed callback (rather
 * than a separate isPlaying/started/error lambda per concern) keeps the composable's own
 * parameter count down. [PlayingChanged] is the pre-existing ducking signal (see
 * VideoPlaybackStateRepository); [Started]/[Error] feed the opt-in debug log (see
 * AppContainer's logVideoPlaybackStarted/logVideoPlaybackError) - both carry [path] since,
 * once video is a placeable widget, a single canvas-wide callback can no longer infer which
 * widget's video the event belongs to purely from call-site context the way MainActivity's
 * old single full-screen overlay could.
 */
sealed class VideoPlaybackEvent {
    data class PlayingChanged(val isPlaying: Boolean) : VideoPlaybackEvent()

    data class Started(val path: String) : VideoPlaybackEvent()

    data class Error(val path: String, val message: String) : VideoPlaybackEvent()
}
