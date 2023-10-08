package com.onixen.audioplayer.model.data

import android.graphics.Bitmap

data class TrackInfoV2(
    val title: String?,
    val artist: String?,
    val album: String?,
    val art: Bitmap?,
    val duration: Int?,
    val currentTime: Int = 0
)
