package com.chuichui.video.ui

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.chuichui.video.bean.Vod

/** 片源列表：某分类下的片源，滚动到底自动翻页（虚拟化+滚动加载）；点击进入详情。 */
@Composable
fun VodListScreen(typeId: String, title: String, onOpenVod: (vodId: String, name: String) -> Unit) {
    val ctx = LocalContext.current
    val state = remember(typeId) { PagedVodListState { pg -> loadVodPageFromSource(ctx, pg) { it.category(typeId, pg) } } }
    val listState = rememberLazyListState()

    LaunchedEffect(state) { state.loadFirst() }
    AutoLoadMore(listState, shouldLoad = { state.hasMore }, onLoadMore = { state.loadNext() })

    ScreenScaffold(
        title = title,
        loading = state.loading && state.vods.isEmpty(),
        empty = !state.loading && state.vods.isEmpty(),
        emptyText = "该分类暂无片源",
        listState = listState,
        bottom = { if (state.loading && state.vods.isNotEmpty()) LoadMoreRow() },
    ) {
        items(state.vods) { v ->
            ListRow(vodLabel(v)) { onOpenVod(v.vodId, v.vodName) }
        }
    }
}

internal fun vodLabel(vod: Vod): String =
    vod.vodName + (if (!vod.vodRemarks.isNullOrEmpty()) "  [" + vod.vodRemarks + "]" else "")
