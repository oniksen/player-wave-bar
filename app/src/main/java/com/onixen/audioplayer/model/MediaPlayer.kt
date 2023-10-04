package com.onixen.audioplayer.model

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import com.onixen.audioplayer.model.data.TrackInfo

class MediaPlayer(private val context: Context, private val resId: Int) {
    private val player = MediaPlayer.create(context, resId)

    fun startTrack() {
        player.start()
    }
    fun pauseTrack() {
        player.pause()
    }
    fun rewindTrack(newPos: Int) {
        player.seekTo(newPos)
        player.start()
    }
    fun getMetadata(): TrackInfo {
        val uri = Uri.parse("android.resource://" + context.packageName + "/" + resId)
        val metadataRetriever = MediaMetadataRetriever()
        metadataRetriever.setDataSource(context, uri)

        val trackArt = metadataRetriever.embeddedPicture?.let {
            BitmapFactory.decodeByteArray(it, 0, it.size)
        }

        return TrackInfo(
            title = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
            artist = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
            album = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
            duration = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION),
            art = trackArt
        )
    }
    fun getCurrentPos(): Int {
        return player.currentPosition
    }
}