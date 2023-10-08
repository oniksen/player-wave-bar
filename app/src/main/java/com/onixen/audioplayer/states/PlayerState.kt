package com.onixen.audioplayer.states

import com.onixen.audioplayer.model.data.TrackInfo

sealed class PlayerState {
    class RecoveredPaused(
        val currentPos: Int,
        val metadata: TrackInfo
    ): PlayerState()
    class RecoveredStarted(
        val currentPos: Int,
        val metadata: TrackInfo
    ): PlayerState()
    class Prepared(val info: TrackInfo): PlayerState()
    data object Started: PlayerState()
    data object Resumed: PlayerState()
    data object Paused: PlayerState()
    data object Stopped: PlayerState()
}