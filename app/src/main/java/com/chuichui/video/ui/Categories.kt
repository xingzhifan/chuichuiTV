package com.chuichui.video.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chuichui.video.bean.Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 分类首页：拉取源的「分类」，点击进入该分类的片源列表；含加载/空态占位。 */
@Composable
fun Categories(onOpenCategory: (typeId: String, title: String) -> Unit) {
    val ctx = LocalContext.current
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        categories = loadFromSource(ctx) { adapter -> adapter.home() }
        loading = false
    }
    ScreenScaffold(title = "锤锤影视", loading = loading, empty = categories.isEmpty(), emptyText = "未配置可用源\n请稍后接入采集源") {
        items(categories) { c ->
            ListRow(c.typeName) { onOpenCategory(c.typeId, c.typeName) }
        }
    }
}

/** 通用列表骨架：标题 + 加载/空态 + 内容槽。 */
@Composable
internal fun ScreenScaffold(
    title: String,
    loading: Boolean,
    empty: Boolean,
    emptyText: String,
    content: LazyListScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("加载中…", color = Color.Gray)
            }
            empty -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(emptyText, color = Color.Gray, textAlign = TextAlign.Center)
            }
            else -> LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), content = content)
        }
    }
}

/** 列表行：可点击（宽度撑满、高度自适应）。 */
@Composable
internal fun ListRow(text: String, onClick: () -> Unit) {
    Text(
        text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp, horizontal = 8.dp)
            .clickable { onClick() }
    )
}
