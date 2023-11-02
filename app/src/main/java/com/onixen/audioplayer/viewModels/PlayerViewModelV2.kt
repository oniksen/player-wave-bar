package com.onixen.audioplayer.viewModels

import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oniksen.playgroundmvi_pattern.intents.PlayerIntent
import com.onixen.audioplayer.model.data.TrackInfoV2
import com.onixen.audioplayer.states.PlayerStateV2
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerViewModelV2: ViewModel() {
    private var player: MediaPlayer? = null
    private var metadata: TrackInfoV2? = null

    private val _playerState: MutableStateFlow<PlayerStateV2?> = MutableStateFlow(null)
    val playerState = _playerState.asStateFlow()

    fun attachTrackData(player: MediaPlayer, metadata: TrackInfoV2) {
        if (this.player != player)
            this.player = player
        if (this.metadata != metadata)
            this.metadata = metadata
    }
    fun fetchPlayerInfo(): TrackInfoV2? {
        return metadata
    }
    fun sendIntent(intent: PlayerIntent) {
        when (intent) {
            is PlayerIntent.Play -> { startAudio() }
            is PlayerIntent.Pause -> { pauseAudio() }
            is PlayerIntent.Rewind -> { rewindAudio(ms = intent.newPos) }
            else -> {
                Log.w(TAG, "sendIntent: unknown player intent.") }
        }
    }

    fun prepareView(): TrackInfoV2 {
        val updated = metadata?.copy(currentTime = player?.currentPosition!!)!!
        Log.i(TAG, "prepareView: $updated")
        return updated
    }

    /**
     * A function that starts a track or resumes it depending on the current state.
     * */
    private fun startAudio() {
        Log.i(TAG, "startAudio()")
        viewModelScope.launch {
            // Метод запуска должен срабатывать только если трек в данный момент остановлен.
            if (player?.isPlaying == false) {
                player?.start()
                _playerState.emit(PlayerStateV2.Started(player?.currentPosition!!))
            }
        }
    }
    private fun pauseAudio() {
        Log.i(TAG, "pauseAudio()")
        viewModelScope.launch {
            if (player?.isPlaying == true) {
                player?.pause()
                _playerState.emit(PlayerStateV2.Paused)
            }
        }
    }
    private fun rewindAudio(ms: Int) {
        Log.d(TAG, "rewindAudio: new audio track position = $ms")
        viewModelScope.launch {
            if (player?.isPlaying == true) {
                player?.seekTo(ms)
            } else {
                player?.seekTo(ms)
                player?.pause()
            }
        }
    }

    companion object {
        private const val TAG = "player_view_model_v2"
    }
}