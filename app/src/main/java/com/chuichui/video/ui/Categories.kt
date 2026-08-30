package com.chuichui.video.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chuichui.video.SourceRepo
import com.chuichui.video.bean.Category

/** 分类首页：源选择器（多源并存）+ 当前源的分类列表；右上「源」进入源管理。 */
@Composable
fun Categories(
    onOpenCategory: (typeId: String, title: String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val ctx = LocalContext.current
    val repo = remember { SourceRepo(ctx) }
    var sources by remember { mutableStateOf(repo.load()) }
    var selected by remember {
        mutableStateOf(repo.selected().coerceIn(0, (sources.size - 1).coerceAtLeast(0)))
    }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(selected) {
        loading = true
        categories = loadFromSource(ctx) { adapter -> adapter.home() }
        loading = false
    }

    ScreenScaffold(
        title = "锤锤影视",
        loading = loading,
        empty = categories.isEmpty(),
        emptyText = "未配置可用源\n点右上「源」添加采集源",
        actions = {
            Text(
                "源",
                modifier = Modifier
                    .clickable { onOpenSettings() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        },
        header = {
            if (sources.size > 1) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 12.dp, end = 12.dp, bottom = 4.dp)
                ) {
                    sources.forEachIndexed { idx, s ->
                        val isSel = idx == selected
                        Text(
                            s.name,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .background(
                                    if (isSel) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .clickable {
                                    repo.setSelected(idx)
                                    selected = idx
                                }
                        )
                    }
                }
            }
        }
    ) {
        items(categories) { c ->
            ListRow(c.typeName) { onOpenCategory(c.typeId, c.typeName) }
        }
    }
}

/** 通用列表骨架：标题(+动作/头部插槽) + 加载/空态 + 内容槽。 */
@Composable
internal fun ScreenScaffold(
    title: String,
    loading: Boolean,
    empty: Boolean,
    emptyText: String,
    modifier: Modifier = Modifier,
    actions: (@Composable () -> Unit)? = null,
    header: (@Composable () -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().then(modifier)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            actions?.invoke()
        }
        header?.invoke()
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
