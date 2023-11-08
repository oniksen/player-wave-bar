package com.onixen.audioplayer.states

sealed class PlayerState {
    data class Started(val currentPos: Int): PlayerState()
    data class Paused(val currentPos: Int?): PlayerState()
    data object Stoped: PlayerState()
}