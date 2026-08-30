package com.chuichui.video.ui

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.chuichui.video.SourceRepo
import com.chuichui.video.bean.Episode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 详情：把所有线路的集拉平为"线路 | 集"，点击某集进入播放。换线路=选不同线路前缀的集。 */
@Composable
fun DetailScreen(vodId: String, title: String, onPlay: (url: String, label: String) -> Unit) {
    val ctx = LocalContext.current
    var episodes by remember { mutableStateOf<List<Episode>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(vodId) {
        episodes = loadFromSource(ctx) { adapter -> adapter.detail(vodId).flatten() }
        loading = false
    }
    ScreenScaffold(title = title, loading = loading, empty = episodes.isEmpty(), emptyText = "暂无可播放的集") {
        items(episodes) { ep ->
            ListRow(ep.name) { onPlay(ep.url, ep.name) }
        }
    }
}
