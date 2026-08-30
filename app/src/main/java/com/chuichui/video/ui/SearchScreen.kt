package com.chuichui.video.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.chuichui.video.SourceRepo
import com.chuichui.video.bean.Vod
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** 搜索：输入关键词调用当前选中源搜索，展示结果；点击结果进详情。 */
@Composable
fun SearchScreen(onOpenVod: (vodId: String, name: String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var keyword by remember { mutableStateOf("") }
    var vods by remember { mutableStateOf<List<Vod>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }
    val hasSource = remember { SourceRepo(ctx).load().isNotEmpty() }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    fun runSearch() {
        val kw = keyword.trim()
        if (kw.isEmpty()) return
        searchJob?.cancel()   // 取消在途旧请求，防慢结果覆盖新结果
        loading = true
        searchJob = scope.launch {
            vods = loadFromSource(ctx) { adapter -> adapter.search(kw, 1) }
            loading = false
            searched = true
        }
    }

    ScreenScaffold(
        title = "搜索",
        loading = loading,
        empty = searched && vods.isEmpty(),
        emptyText = when {
            !hasSource -> "未配置可用源\n点首页右上「源」添加"
            searched -> "无结果"
            else -> "输入关键词开始搜索"
        },
        header = {
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
        }
    ) {
        items(vods) { v ->
            ListRow(vodLabel(v)) { onOpenVod(v.vodId, v.vodName) }
        }
    }
}
