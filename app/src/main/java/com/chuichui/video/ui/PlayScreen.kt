package com.chuichui.video.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/** 播放页：Media3 ExoPlayer 播放某集的播放地址（m3u8/mp4），全屏。 */
@Composable
fun PlayScreen(url: String) {
    val context = LocalContext.current
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    DisposableEffect(url) {
        val p = ExoPlayer.Builder(context).build()
        player = p
        if (url.isNotEmpty()) {
            p.setMediaItem(MediaItem.fromUri(url))
            p.prepare()
            p.setPlayWhenReady(true)
        }
        onDispose {
            p.release()
            player = null
        }
    }
    AndroidView(
        factory = { ctx -> PlayerView(ctx) },
        update = { it.player = player },
        modifier = Modifier.fillMaxSize()
    )
}
