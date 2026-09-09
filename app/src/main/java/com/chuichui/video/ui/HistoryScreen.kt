package com.chuichui.video.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chuichui.video.History
import com.chuichui.video.play.PlayRequest
import com.chuichui.video.ui.theme.Surface
import com.chuichui.video.ui.theme.TextSecondary

/** 观看历史：海报占位缩略 + 片名 + 线路/时间；点击按记录重建播放队列（含故障切换能力）。 */
@Composable
fun HistoryScreen(onPlay: (PlayRequest) -> Unit) {
    val ctx = LocalContext.current
    val items = remember { History.Store(ctx).load() }
    val title = "历史"

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(title)
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无观看历史", color = TextSecondary)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                items(items, key = { it.vodId + it.episode + it.line }) { h ->
                    HistoryRow(h) {
                        onPlay(PlayRequest(h.vodId, h.vodName, h.episode, h.line))
                    }
                }
            }
        }
    }
}

/** 历史行：左侧海报占位缩略（当前 History 未存海报地址，用主题色占位），右侧片名 + 线路/时间。 */
@Composable
private fun HistoryRow(h: History, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(64.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp))
                .background(Surface)
        ) {
            AccentBar(Modifier.width(48.dp).height(3.dp).align(Alignment.Center))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                h.vodName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                h.episode +
                    (if (!h.line.isNullOrEmpty()) " · " + h.line else "") +
                    " · " + formatTs(h.ts),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

internal fun formatTs(ts: Long): String {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = ts }
    val p = { n: Int -> n.toString().padStart(2, '0') }
    return "${cal.get(java.util.Calendar.YEAR)}-${p(cal.get(java.util.Calendar.MONTH) + 1)}-${p(cal.get(java.util.Calendar.DAY_OF_MONTH))} ${p(cal.get(java.util.Calendar.HOUR_OF_DAY))}:${p(cal.get(java.util.Calendar.MINUTE))}"
}
