package com.esde.companion.ui.video

/**
 * Reported via VideoOverlayScreen's `onPlaybackEvent` - a single sealed callback (rather
 * than a separate isPlaying/started/error lambda per concern) keeps the composable's own
 * parameter count down. [PlayingChanged] is the pre-existing ducking signal (see
 * VideoPlaybackStateRepository); [Started]/[Error] feed the opt-in debug log (see
 * AppContainer's logVideoPlaybackStarted/logVideoPlaybackError).
 */
sealed class VideoPlaybackEvent {
    data class PlayingChanged(val isPlaying: Boolean) : VideoPlaybackEvent()

    data object Started : VideoPlaybackEvent()

    data class Error(val message: String) : VideoPlaybackEvent()
}
