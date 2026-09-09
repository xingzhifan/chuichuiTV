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
import com.chuichui.video.category.CategoryBlockerRepo
import com.chuichui.video.category.CategoryFilter
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
    val allowTerms = remember { CategoryBlockerRepo(ctx).load() }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    val gridState = rememberLazyGridState()

    /** 加载某次搜索的一个「有效页」：逐页过滤白名单，整页滤空则顺延下一页（到 pageCount 为止）。
     *  返回 (过滤后片源, 是否还有更多页, 实际使用的末页号)。 */
    suspend fun loadFiltered(kw: String, startPage: Int): Triple<List<Vod>, Boolean, Int> {
        var pg = startPage
        while (true) {
            val r = loadVodPageFromSource(ctx, pg) { adapter -> adapter.search(kw, pg) }
            val kept = r.vods.filter { CategoryFilter.isAllowed(it.typeName, allowTerms) }
            val more = r.hasMore(pg)
            if (kept.isNotEmpty() || !more) return Triple(kept, more, pg)
            pg++
        }
    }

    fun runSearch() {
        val kw = keyword.trim()
        if (kw.isEmpty()) return
        searchJob?.cancel()
        loading = true
        searchJob = scope.launch {
            val (list, more, lastPg) = loadFiltered(kw, 1)
            vods = list
            page = lastPg
            hasMore = more
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
            val (list, more, lastPg) = loadFiltered(kw, next)
            vods = vods + list
            page = lastPg
            hasMore = more
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
