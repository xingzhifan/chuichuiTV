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

private data class PlayRow(val line: String, val episode: String, val url: String)

/** 详情：把所有线路的集拉平为"线路 | 集"，点击某集进入播放（携带源/线路信息供故障切换）。 */
@Composable
fun DetailScreen(
    vodId: String,
    title: String,
    onPlay: (vodId: String, vodName: String, episode: String, line: String) -> Unit,
) {
    val ctx = LocalContext.current
    var rows by remember { mutableStateOf<List<PlayRow>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(vodId) {
        rows = loadFromSource(ctx) { adapter ->
            adapter.detail(vodId).lines.flatMap { line ->
                line.episodes.map { ep ->
                    PlayRow(line.name.ifEmpty { "线路" }, ep.name, ep.url)
                }
            }
        }
        loading = false
    }
    ScreenScaffold(title = title, loading = loading, empty = rows.isEmpty(), emptyText = "暂无可播放的集") {
        items(rows) { r ->
            ListRow(r.line + " | " + r.episode) {
                onPlay(vodId, title, r.episode, r.line)
            }
        }
    }
}
