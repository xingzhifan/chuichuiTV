package com.chuichui.video.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.chuichui.video.History
import com.chuichui.video.play.PlayRequest

/** 观看历史：列出最近观看，点击按记录重建播放队列（含故障切换能力）。 */
@Composable
fun HistoryScreen(onPlay: (PlayRequest) -> Unit) {
    val ctx = LocalContext.current
    val items = remember { History.Store(ctx).load() }
    ScreenScaffold(title = "历史", loading = false, empty = items.isEmpty(), emptyText = "暂无观看历史") {
        items(items) { h ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable { onPlay(PlayRequest(h.vodId, h.vodName, h.episode, h.line)) }
                    .padding(vertical = 10.dp, horizontal = 8.dp)
            ) {
                Text(h.label())
                Text(
                    h.line + " · " + formatTs(h.ts),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

internal fun formatTs(ts: Long): String {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = ts }
    val p = { n: Int -> n.toString().padStart(2, '0') }
    return "${cal.get(java.util.Calendar.YEAR)}-${p(cal.get(java.util.Calendar.MONTH) + 1)}-${p(cal.get(java.util.Calendar.DAY_OF_MONTH))} ${p(cal.get(java.util.Calendar.HOUR_OF_DAY))}:${p(cal.get(java.util.Calendar.MINUTE))}"
}
