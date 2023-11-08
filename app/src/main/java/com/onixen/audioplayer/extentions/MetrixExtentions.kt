package com.onixen.audioplayer.extentions

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.util.concurrent.Flow
import kotlin.time.Duration.Companion.milliseconds

fun Int.msToUserFriendlyStr(): String {
    val ms = this.milliseconds
    val minutes = ms.inWholeMinutes
    val seconds = ms.inWholeSeconds - (minutes * 60)
    return "$minutes:$seconds"
}
fun Int.dp(context: Context): Int {
    return (this * context.resources.displayMetrics.density).toInt()
}
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "audio_uri_list")