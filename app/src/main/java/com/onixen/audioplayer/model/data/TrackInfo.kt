package com.onixen.audioplayer.model.data

import android.graphics.Bitmap

data class TrackInfo(
    val title: String?,
    val artist: String?,
    val album: String?,
    val duration: String?,
    val art: Bitmap?
)
