package com.onixen.audioplayer.states

import com.onixen.audioplayer.model.data.TrackInfoV2

sealed class PlayerStateV2 {
    data class Started(val currentPos: Int): PlayerStateV2()
    data class Paused(val currentPos: Int?): PlayerStateV2()
}