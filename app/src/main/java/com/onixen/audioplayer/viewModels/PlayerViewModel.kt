package com.onixen.audioplayer.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oniksen.playgroundmvi_pattern.intents.PlayerIntent
import com.oniksen.playgroundmvi_pattern.intents.PlayerIntent.*
import com.oniksen.playgroundmvi_pattern.states.PlayerState
import com.onixen.audioplayer.model.MediaPlayer
import com.onixen.audioplayer.views.interfaces.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerViewModel: ViewModel() {
    private lateinit var playerView: PlayerView
    private lateinit var player: MediaPlayer

    private val _playerState: MutableStateFlow<PlayerState> = MutableStateFlow(PlayerState.Stop)
    val playerState = _playerState.asStateFlow()

    fun attachPlayerView(view: PlayerView, player: MediaPlayer) {
        this.playerView = view
        this.player = player
    }
    fun sendIntent(intent: PlayerIntent) {
        when (intent) {
            is Recover -> { recoverTrack() }
            is Prepare -> { getTrackInfo() }
            is Play -> { startAudio() }
            is Pause -> { pauseAudio() }
            is Rewind -> { rewindAudio(intent.newPos) }
        }
    }
    private fun startAudio() {
        Log.d(TAG, "startAudio()")
        viewModelScope.launch {
            val lastState = withContext(Dispatchers.IO) { _playerState.replayCache.last() }
            if (lastState is PlayerState.Stop || lastState is PlayerState.Prepared) {
                _playerState.emit(PlayerState.Start)
            }
            else {
                _playerState.emit(PlayerState.Resume)
            }
            player.startTrack()
        }
    }
    private fun pauseAudio() {
        Log.d(TAG, "pauseAudio()")
        viewModelScope.launch {
            player.pauseTrack()
            _playerState.emit(PlayerState.Paused)
        }
    }
    private fun rewindAudio(newPos: Int) {
        Log.d(TAG, "rewindAudio($newPos)")
        viewModelScope.launch {
            val lastState = withContext(Dispatchers.IO) { _playerState.replayCache.last() }
            player.rewindTrack(newPos)
            if (lastState == PlayerState.Stop || lastState == PlayerState.Paused) {
                player.pauseTrack()
            }
        }
    }
    private fun getTrackInfo() {
        Log.d(TAG, "getTrackInfo()")
        viewModelScope.launch {
            _playerState.emit(PlayerState.Prepared(player.getMetadata()))
        }
    }
    private fun recoverTrack() {
        viewModelScope.launch {
            _playerState.emit(PlayerState.Recovered(
                currentPos = player.getCurrentPos(),
                metadata = player.getMetadata()
            ))
        }
    }

    companion object {
        private const val TAG = "player_view_model"
    }
}