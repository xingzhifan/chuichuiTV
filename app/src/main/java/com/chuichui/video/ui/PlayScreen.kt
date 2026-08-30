package com.chuichui.video.ui

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.chuichui.video.play.PlayQueue
import com.chuichui.video.play.PlaybackResolver

/** 播放页：聚合多源候选、自动故障切换（失败/超时切下一候选）、失败源降级；自动横屏 + 沉浸式。 */
@Composable
fun PlayScreen(vodId: String, vodName: String, episode: String, line: String) {
    val context = LocalContext.current
    val activity = context as? Activity
    val window = activity?.window

    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

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

    var queue by remember { mutableStateOf<PlayQueue?>(null) }
    var status by remember { mutableStateOf("正在聚合各源播放候选…") }
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var exhausted by remember { mutableStateOf(false) }

    LaunchedEffect(vodId, vodName, episode, line) {
        queue = PlaybackResolver.buildQueue(context, vodId, vodName, line, episode)
    }

    queue?.let { q ->
        DisposableEffect(q) {
            val failover = Failover(context, q, onStatus = { status = it }, onExhausted = { exhausted = true })
            failover.start()
            onDispose { failover.release() }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        player?.let { p ->
            AndroidView(
                factory = { ctx -> PlayerView(ctx) },
                update = { it.player = p },
                modifier = Modifier.fillMaxSize()
            )
        }
        if (!exhausted) {
            Text(
                status,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(Color(0x88000000))
                    .padding(4.dp)
            )
        } else {
            Text(
                "所有候选均失败\n请返回换片源或稍后再试",
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

/** 故障切换控制器：播放当前候选；播放错误或超时未就绪 → 降级并切下一候选。 */
private class Failover(
    context: Context,
    private val queue: PlayQueue,
    private val onStatus: (String) -> Unit,
    private val onExhausted: () -> Unit,
) : Player.Listener {

    companion object {
        private const val WATCHDOG_MS = 12_000L
    }

    private val player = ExoPlayer.Builder(context).build()
    private val handler = Handler(Looper.getMainLooper())
    private var ready = false

    fun start() {
        player.addListener(this)
        playCurrent()
    }

    fun release() {
        handler.removeCallbacksAndMessages(null)
        player.release()
    }

    private fun playCurrent() {
        val c = queue.current()
        if (c == null) {
            onExhausted()
            return
        }
        ready = false
        onStatus("候选 ${queue.position() + 1}/${queue.size()}：${c.label()}")
        player.setMediaItem(MediaItem.fromUri(c.url))
        player.prepare()
        player.setPlayWhenReady(true)
        handler.postDelayed({ if (!ready) fail() }, WATCHDOG_MS)
    }

    private fun fail() {
        queue.markFailedAndAdvance()
        if (queue.exhausted()) {
            onStatus("所有候选均失败")
            onExhausted()
        } else {
            playCurrent()
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        fail()
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_READY) {
            ready = true
            handler.removeCallbacksAndMessages(null)
        }
    }
}
