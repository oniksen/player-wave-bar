package com.onixen.audioplayer.extentions

import android.content.Context
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