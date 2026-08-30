package com.chuichui.video.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
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

/** 播放页：Media3 ExoPlayer 播放（m3u8/mp4），自动横屏 + 沉浸式（隐藏系统栏），退出还原。 */
@Composable
fun PlayScreen(url: String) {
    val context = LocalContext.current
    val activity = context as? Activity
    val window = activity?.window

    // 进入：强制横屏（跟随传感器选正/反横向）；退出：还原系统默认方向。
    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // 沉浸式：隐藏系统栏，滑动临时呼出（API 分支：R+ 用 WindowInsetsController，低版本用 systemUiVisibility）。
    DisposableEffect(window) {
        val w = window
        if (w != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                w.setDecorFitsSystemWindows(false)
                w.insetsController?.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                w.insetsController?.hide(WindowInsets.Type.systemBars())
            } else {
                @Suppress("DEPRECATION")
                w.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    )
            }
        }
        onDispose {
            if (w != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                @Suppress("DEPRECATION")
                w.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
    }

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
        factory = { ctx ->
            PlayerView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { it.player = player },
        modifier = Modifier.fillMaxSize()
    )
}
