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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chuichui.video.SourceRepo
import com.chuichui.video.bean.Vod
import com.chuichui.video.ui.theme.TextSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** 搜索：输入关键词调用当前选中源搜索，结果海报网格 + 滚动到底自动翻页；点击结果进详情。 */
@Composable
fun SearchScreen(onOpenVod: (vodId: String, name: String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var keyword by remember { mutableStateOf("") }
    var vods by remember { mutableStateOf<List<Vod>>(emptyList()) }
    var page by remember { mutableStateOf(1) }
    var hasMore by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }
    val hasSource = remember { SourceRepo(ctx).load().isNotEmpty() }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    val gridState = rememberLazyGridState()

    fun runSearch() {
        val kw = keyword.trim()
        if (kw.isEmpty()) return
        searchJob?.cancel()
        loading = true
        page = 1
        searchJob = scope.launch {
            val r = loadVodPageFromSource(ctx, 1) { adapter -> adapter.search(kw, 1) }
            vods = r.vods
            hasMore = r.hasMore(1)
            loading = false
            searched = true
        }
    }

    fun loadNext() {
        if (loading || !hasMore) return
        val kw = keyword.trim()
        if (kw.isEmpty()) return
        loading = true
        val next = page + 1
        searchJob = scope.launch {
            val r = loadVodPageFromSource(ctx, next) { adapter -> adapter.search(kw, next) }
            vods = vods + r.vods
            page = next
            hasMore = r.hasMore(next)
            loading = false
        }
    }

    AutoLoadMoreGrid(gridState, shouldLoad = { hasMore && !loading }, onLoadMore = { loadNext() })

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar("搜索")
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                label = { Text("关键词") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { runSearch() }),
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { runSearch() },
                modifier = Modifier.padding(start = 8.dp)
            ) { Text("搜索") }
        }

        when {
            loading && vods.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("加载中…", color = TextSecondary)
            }
            searched && vods.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("无结果", color = TextSecondary, textAlign = TextAlign.Center)
            }
            !searched -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (hasSource) "输入关键词开始搜索" else "未配置可用源\n点首页右上「源」添加",
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                )
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 110.dp),
                state = gridState,
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                items(vods, key = { it.vodId }) { v ->
                    PosterCard(
                        vod = v,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                            .clickable { onOpenVod(v.vodId, v.vodName) },
                    )
                }
                if (loading) item { LoadMoreRow() }
            }
        }
    }
}
