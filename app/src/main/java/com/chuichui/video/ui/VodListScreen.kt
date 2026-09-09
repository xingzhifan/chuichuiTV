package com.chuichui.video.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chuichui.video.bean.Vod
import com.chuichui.video.ui.theme.TextSecondary

/** 片源列表：某分类下的片源，海报网格 + 滚动到底自动翻页（虚拟化）；点击进入详情。 */
@Composable
fun VodListScreen(typeId: String, title: String, onOpenVod: (vodId: String, name: String) -> Unit) {
    val ctx = LocalContext.current
    val state = remember(typeId) { PagedVodListState { pg -> loadVodPageFromSource(ctx, pg) { it.category(typeId, pg) } } }
    val gridState = rememberLazyGridState()

    LaunchedEffect(state) { state.loadFirst() }
    AutoLoadMoreGrid(gridState, shouldLoad = { state.hasMore }, onLoadMore = { state.loadNext() })

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(title)

        when {
            state.loading && state.vods.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("加载中…", color = TextSecondary)
            }
            state.vods.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("该分类暂无片源", color = TextSecondary, textAlign = TextAlign.Center)
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 110.dp),
                state = gridState,
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                items(state.vods, key = { it.vodId }) { v ->
                    PosterCard(
                        vod = v,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                            .clickable { onOpenVod(v.vodId, v.vodName) },
                    )
                }
                if (state.loading) item { LoadMoreRow() }
            }
        }
    }
}

internal fun vodLabel(vod: Vod): String =
    vod.vodName + (if (!vod.vodRemarks.isNullOrEmpty()) "  [" + vod.vodRemarks + "]" else "")
