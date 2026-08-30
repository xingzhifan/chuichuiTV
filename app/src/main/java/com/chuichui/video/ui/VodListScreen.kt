package com.chuichui.video.ui

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.chuichui.video.SourceRepo
import com.chuichui.video.bean.Vod

/** 片源列表：某分类下的片源，点击进入详情。 */
@Composable
fun VodListScreen(typeId: String, title: String, onOpenVod: (vodId: String, name: String) -> Unit) {
    val ctx = LocalContext.current
    var vods by remember { mutableStateOf<List<Vod>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(typeId) {
        vods = loadFromSource(ctx) { adapter -> adapter.category(typeId, 1) }
        loading = false
    }
    ScreenScaffold(title = title, loading = loading, empty = vods.isEmpty(), emptyText = "该分类暂无片源") {
        items(vods) { v ->
            ListRow(vodLabel(v)) { onOpenVod(v.vodId, v.vodName) }
        }
    }
}

internal fun vodLabel(vod: Vod): String =
    vod.vodName + (if (!vod.vodRemarks.isNullOrEmpty()) "  [" + vod.vodRemarks + "]" else "")
