package com.oniksen.playgroundmvi_pattern.intents

sealed class PlayerIntent {
    data object RecoverPaused: PlayerIntent()
    data object RecoverStarted: PlayerIntent()
    data object Prepare: PlayerIntent()
    data object Play: PlayerIntent()
    data object Pause: PlayerIntent()
    data class Rewind(val newPos: Int): PlayerIntent()
}