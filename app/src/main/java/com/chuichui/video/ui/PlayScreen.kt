package com.chuichui.video.ui

import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/** 播放页：Media3 ExoPlayer 播放某集的播放地址（m3u8/mp4），全屏。 */
@Composable
fun PlayScreen(url: String, title: String) {
    val context = LocalContext.current
    val player = remember { ExoPlayer.Builder(context).build() }
    DisposableEffect(url) {
        if (url.isNotEmpty()) {
            player.setMediaItem(MediaItem.fromUri(url))
            player.prepare()
            player.setPlayWhenReady(true)
        }
        onDispose { player.release() }
    }
    AndroidView(
        factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = true } },
        modifier = Modifier.fillMaxSize()
    )
}
