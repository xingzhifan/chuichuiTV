package com.chuichui.video.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chuichui.video.SourceRepo
import com.chuichui.video.bean.Category
import com.chuichui.video.ui.theme.AccentBlue
import com.chuichui.video.ui.theme.AccentPurple
import com.chuichui.video.ui.theme.SurfaceHigh
import com.chuichui.video.ui.theme.TextSecondary

/** 分类首页：源选择器（多源并存）+ 当前源的分类网格；右上「搜索」「历史」「源」入口。 */
@Composable
fun Categories(
    onOpenCategory: (typeId: String, title: String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val ctx = LocalContext.current
    val repo = remember { SourceRepo(ctx) }
    var sources by remember { mutableStateOf(repo.load()) }
    var selectedId by remember { mutableStateOf(repo.selectedId()) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(selectedId) {
        sources = repo.load()
        if (sources.none { it.id == selectedId }) selectedId = sources.firstOrNull()?.id ?: ""
        repo.setSelectedId(selectedId)
        loading = true
        categories = loadFromSource(ctx) { adapter -> adapter.home() }
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        // 顶栏：标题 + 右上入口
        ScreenTopBar("锤锤影视") {
            TopAction("搜索") { onOpenSearch() }
            TopAction("历史") { onOpenHistory() }
            TopAction("源") { onOpenSettings() }
        }

        // 多源时的源选择器
        if (sources.size > 1) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 12.dp, end = 12.dp, bottom = 6.dp)
            ) {
                sources.forEach { s ->
                    SourceChip(
                        text = s.name,
                        selected = s.id == selectedId,
                        onClick = {
                            repo.setSelectedId(s.id)
                            selectedId = s.id
                        }
                    )
                }
            }
        }

        Box(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text(
                if (sources.size > 1) "分类" else "浏览",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("加载中…", color = TextSecondary)
            }
            categories.isEmpty() -> Box(
                Modifier.fillMaxSize(), contentAlignment = Alignment.Center
            ) {
                Text(
                    "未配置可用源\n点右上「源」添加采集源",
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                )
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 128.dp),
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                items(categories, key = { it.typeId }) { c ->
                    CategoryTile(c.typeName) { onOpenCategory(c.typeId, c.typeName) }
                }
            }
        }
    }
}

/** 顶栏动作文字（搜索/历史/源），点击反馈 + 主题色。 */
@Composable
private fun TopAction(text: String, onClick: () -> Unit) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

/** 源选择 chip：选中时蓝紫渐变高亮。 */
@Composable
private fun SourceChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) {
        Brush.horizontalGradient(listOf(AccentPurple, AccentBlue))
    } else {
        Brush.horizontalGradient(listOf(SurfaceHigh, SurfaceHigh))
    }
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = if (selected) Color.White else TextSecondary,
        modifier = Modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 7.dp)
            .clickable { onClick() },
    )
}

/** 分类瓦片：可点击的圆角卡片，上方渐变竖条 + 居中分类名。 */
@Composable
private fun CategoryTile(name: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceHigh)
            .clickable { onClick() }
            .padding(vertical = 20.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AccentBar(Modifier.width(28.dp).height(3.dp))
        Spacer(Modifier.height(10.dp))
        Text(
            name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}
