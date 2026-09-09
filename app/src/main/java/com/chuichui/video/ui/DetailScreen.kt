package com.chuichui.video.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.chuichui.video.bean.Detail
import com.chuichui.video.bean.Line
import com.chuichui.video.play.PlayRequest
import com.chuichui.video.ui.theme.ScrimBottom
import com.chuichui.video.ui.theme.SurfaceHigh
import com.chuichui.video.ui.theme.TextSecondary

/** 详情：大封面头图 + 片名/备注/年份 + 线路分组 + 集数选择（虚拟化网格），点击某集进入播放。 */
@Composable
fun DetailScreen(
    vodId: String,
    title: String,
    onPlay: (PlayRequest) -> Unit,
) {
    val ctx = LocalContext.current
    var detail by remember { mutableStateOf<Detail?>(null) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(vodId) {
        detail = loadFromSource(ctx) { adapter -> listOf(adapter.detail(vodId)) }.firstOrNull()
        loading = false
    }

    val d = detail
    val gridState = rememberLazyGridState()

    Box(Modifier.fillMaxSize()) {
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("加载中…", color = TextSecondary)
            }
        } else if (d == null || d.lines.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无可播放的集", color = TextSecondary, textAlign = TextAlign.Center)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 96.dp),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    DetailHeader(d)
                }
                d.lines.forEach { line ->
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        LineHeader(line)
                    }
                    items(line.episodes, key = { it.name }) { ep ->
                        EpisodeChip(
                            epName = ep.name,
                            onClick = { onPlay(PlayRequest(vodId, d.vod.vodName, ep.name, line.name)) },
                        )
                    }
                }
            }
        }
    }
}

/** 大封面头图：全宽 16:9 海报 + 底部渐变遮罩 + 片名/备注/年份叠加。 */
@Composable
private fun DetailHeader(d: Detail) {
    Box(Modifier.fillMaxWidth()) {
        AsyncImage(
            model = d.vod.vodPic,
            contentDescription = d.vod.vodName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(SurfaceHigh),
        )
        Box(
            Modifier
                .matchParentSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, ScrimBottom)))
        )
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text(
                d.vod.vodName,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                buildMeta(d),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
    }
}

/** 线路分组标题：渐变竖条 + 线路名。 */
@Composable
private fun LineHeader(line: Line) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AccentBar(Modifier.width(4.dp).height(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            if (line.name.isNullOrEmpty()) "线路" else line.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** 集数选择块：可点击的圆角小卡片。 */
@Composable
private fun EpisodeChip(epName: String, onClick: () -> Unit) {
    Text(
        epName,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceHigh)
            .clickable { onClick() }
            .padding(vertical = 14.dp, horizontal = 6.dp),
        textAlign = TextAlign.Center,
    )
}

private fun buildMeta(d: Detail): String {
    val v = d.vod
    val parts = mutableListOf<String>()
    v.typeName?.takeIf { it.isNotBlank() }?.let(parts::add)
    v.vodYear?.takeIf { it.isNotBlank() }?.let(parts::add)
    v.vodRemarks?.takeIf { it.isNotBlank() }?.let(parts::add)
    return if (parts.isEmpty()) "" else parts.joinToString(" · ")
}
