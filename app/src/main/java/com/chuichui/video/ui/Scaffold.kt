package com.chuichui.video.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.chuichui.video.ui.theme.AccentBlue
import com.chuichui.video.ui.theme.AccentPurple

/** 通用页面顶栏：标题（可伸展）+ 可选尾部动作。全屏页面统一。 */
@Composable
internal fun ScreenTopBar(
    title: String,
    modifier: Modifier = Modifier,
    actions: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        actions?.invoke()
    }
}

/** 蓝紫渐变条：规格为 紫(#5E35B1)→蓝(#2979FF)。尺寸由调用方 Modifier 决定（横条/竖条）。 */
@Composable
internal fun AccentBar(modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(
                Brush.horizontalGradient(listOf(AccentPurple, AccentBlue)),
                RoundedCornerShape(2.dp),
            )
    )
}

/** 共享选择 Chip：高亮当前项（源选择器 / 类型切换复用）。 */
@Composable
internal fun SelectChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        modifier = Modifier
            .padding(end = 8.dp)
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable { onClick() }
    )
}

/** 列表底部「加载更多」提示。 */
@Composable
internal fun LoadMoreRow() {
    Box(
        Modifier.fillMaxWidth().padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("加载中…", color = Color.Gray)
    }
}

/** 网格滚动到底翻页（LazyVerticalGrid 用）：最后可见项接近列表长度即触发，shouldLoad 为真时才调用。 */
@Composable
internal fun AutoLoadMoreGrid(
    gridState: LazyGridState,
    shouldLoad: () -> Boolean,
    onLoadMore: suspend () -> Unit,
) {
    val onLoadMoreState by rememberUpdatedState(onLoadMore)
    val shouldLoadState by rememberUpdatedState(shouldLoad)
    LaunchedEffect(gridState) {
        snapshotFlow {
            val info = gridState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            last to info.totalItemsCount
        }.collect { (last, total) ->
            if (total > 0 && last >= total - 4 && shouldLoadState()) onLoadMoreState()
        }
    }
}