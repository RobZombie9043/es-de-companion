package com.esde.companion.data.music

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.esde.companion.domain.model.MusicTrack
import com.esde.companion.domain.repository.MusicPlayerController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File

/**
 * Headless ExoPlayer (audio only, no PlayerView) held for the app's whole process
 * lifetime - unlike VideoOverlayScreen's per-composable player, this one is constructed
 * once in AppContainer and never released, since background music has no "screen" to be
 * scoped to. repeatMode is REPEAT_MODE_OFF (not REPEAT_MODE_ONE like the video overlay)
 * so STATE_ENDED actually fires, which is what drives [observeTrackCompletion].
 *
 * [MusicPlaybackCoordinator][com.esde.companion.domain.music.MusicPlaybackCoordinator] calls
 * these methods from AppContainer's `applicationScope` (Dispatchers.Default), but ExoPlayer
 * requires every call on the thread that created it - so every player mutation below is
 * marshalled onto [mainHandler] regardless of caller thread, rather than pushing the whole
 * applicationScope onto Dispatchers.Main (which would move unrelated log-tailing/state-
 * reduction work off its background dispatcher too).
 */
class ExoMusicPlayerController(context: Context) : MusicPlayerController {
    private val mainHandler = Handler(Looper.getMainLooper())

    private val player =
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }

    private val trackCompletions = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val playbackErrors = MutableSharedFlow<String>(extraBufferCapacity = 1)

    init {
        player.addListener(
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        trackCompletions.tryEmit(Unit)
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    playbackErrors.tryEmit(error.message ?: error.toString())
                }
            },
        )
    }

    override fun playTrack(track: MusicTrack) {
        mainHandler.post {
            player.setMediaItem(MediaItem.fromUri(Uri.fromFile(File(track.filePath))))
            player.prepare()
            player.playWhenReady = true
        }
    }

    override fun pause() {
        mainHandler.post { player.playWhenReady = false }
    }

    override fun resume() {
        mainHandler.post { player.playWhenReady = true }
    }

    override fun setVolume(fraction: Float) {
        mainHandler.post { player.volume = fraction }
    }

    override fun observeTrackCompletion(): Flow<Unit> = trackCompletions.asSharedFlow()

    override fun observePlaybackError(): Flow<String> = playbackErrors.asSharedFlow()
}
