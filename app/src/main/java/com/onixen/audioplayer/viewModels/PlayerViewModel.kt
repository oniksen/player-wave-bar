package com.onixen.audioplayer.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oniksen.playgroundmvi_pattern.intents.PlayerIntent
import com.oniksen.playgroundmvi_pattern.intents.PlayerIntent.*
import com.onixen.audioplayer.states.PlayerState
import com.onixen.audioplayer.model.MediaPlayer
import com.onixen.audioplayer.views.interfaces.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerViewModel: ViewModel() {
    private var playerView: PlayerView? = null
    private var player: MediaPlayer? = null

    private var penultimateState: PlayerState? = null

    private val _playerState: MutableStateFlow<PlayerState> = MutableStateFlow(PlayerState.Stopped)
    val playerState = _playerState.asStateFlow()

    fun attachPlayerView(view: PlayerView, player: MediaPlayer) {
        if (this.playerView != view)
            this.playerView = view
        if (this.player != player)
            this.player = player
    }
    fun sendIntent(intent: PlayerIntent) {
        when (intent) {
            is RecoverPaused -> { recoverPausedTrack() }
            is RecoverStarted -> { recoverStartedTrack() }
            is Prepare -> { getTrackInfo() }
            is Play -> { startAudio() }
            is Pause -> { pauseAudio() }
            is Rewind -> { rewindAudio(intent.newPos) }
        }
    }
    fun getPenultimateState(): PlayerState? {
        return penultimateState
    }
    private fun startAudio() {
        Log.d(TAG, "startAudio()")
        viewModelScope.launch {
            penultimateState = _playerState.value
            val lastState = withContext(Dispatchers.IO) { _playerState.replayCache.last() }
            if (lastState is PlayerState.Stopped || lastState is PlayerState.Prepared) {
                _playerState.emit(PlayerState.Started)
            }
            else {
                _playerState.emit(PlayerState.Resumed)
            }
            player?.startTrack()
        }
    }
    private fun pauseAudio() {
        Log.d(TAG, "pauseAudio()")
        viewModelScope.launch {
            penultimateState = _playerState.value
            player?.pauseTrack()
            _playerState.emit(PlayerState.Paused)
        }
    }
    private fun rewindAudio(newPos: Int) {
        Log.d(TAG, "rewindAudio($newPos)")
        viewModelScope.launch {
            penultimateState = _playerState.value
            val lastState = withContext(Dispatchers.IO) { _playerState.replayCache.last() }
            player?.rewindTrack(newPos)
            if (lastState == PlayerState.Stopped || lastState == PlayerState.Paused) {
                player?.pauseTrack()
            }
        }
    }
    private fun getTrackInfo() {
        Log.d(TAG, "getTrackInfo()")
        viewModelScope.launch {
            penultimateState = _playerState.value
            _playerState.emit(PlayerState.Prepared(player!!.getMetadata()))
        }
    }
    private fun recoverPausedTrack() {
        viewModelScope.launch {
            if (player != null) {
                penultimateState = _playerState.value
                _playerState.emit(
                    PlayerState.RecoveredPaused(
                        currentPos = player!!.getCurrentPos(),
                        metadata = player!!.getMetadata()
                    )
                )
            }
        }
    }
    private fun recoverStartedTrack() {
        viewModelScope.launch {
            if (player != null) {
                penultimateState = _playerState.value
                _playerState.emit(
                    PlayerState.RecoveredStarted(
                        currentPos = player!!.getCurrentPos(),
                        metadata = player!!.getMetadata()
                    )
                )
            }
        }
    }

    companion object {
        private const val TAG = "player_view_model"
    }
}