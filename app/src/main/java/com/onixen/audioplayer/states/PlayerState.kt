package com.oniksen.playgroundmvi_pattern.states

import com.onixen.audioplayer.model.data.TrackInfo

sealed class PlayerState {
    class Recovered(
        val currentPos: Int,
        val metadata: TrackInfo
    ): PlayerState()
    class Prepared(val info: TrackInfo): PlayerState()
    data object Start : PlayerState()
    data object Resume: PlayerState()
    data object Paused: PlayerState()
    data object Stop: PlayerState()
}