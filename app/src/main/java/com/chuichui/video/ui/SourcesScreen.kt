package com.chuichui.video.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.chuichui.video.SourceRepo
import com.chuichui.video.bean.Source

/** 源管理：增删源（采集API / JS 蜘蛛）并持久化；点列表项=设为当前浏览源（按稳定 id）。 */
@Composable
fun SourcesScreen(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { SourceRepo(ctx) }
    var sources by remember { mutableStateOf(repo.load()) }
    var selectedId by remember { mutableStateOf(repo.selectedId()) }
    var name by remember { mutableStateOf("") }
    var api by remember { mutableStateOf("") }
    var isJs by remember { mutableStateOf(false) }

    fun persist(list: List<Source>, selId: String) {
        repo.save(list)
        // 选中 id 不在列表里（被删/列表为空）时回退到首个源
        val effective = if (list.any { it.id == selId }) selId else (list.firstOrNull()?.id ?: "")
        repo.setSelectedId(effective)
        sources = list
        selectedId = effective
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("源管理", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Text(
                "完成",
                modifier = Modifier.clickable { onClose() }.padding(8.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("名称") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = api,
            onValueChange = { api = it },
            label = { Text(if (isJs) "JS 蜘蛛脚本地址/内容" else "苹果CMS 采集 API 地址") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
            SelectChip("采集API", !isJs) { isJs = false }
            SelectChip("JS蜘蛛", isJs) { isJs = true }
            Spacer(Modifier.weight(1f))
            Button(onClick = {
                if (name.isNotBlank() && api.isNotBlank()) {
                    val src = Source(name.trim(), api.trim(), if (isJs) Source.Type.JS_SPIDER else Source.Type.MACCMS)
                    val list = sources.toMutableList().apply { add(src) }
                    persist(list, src.id) // 新加的源设为当前浏览源
                    name = ""
                    api = ""
                }
            }) { Text("添加") }
        }

        if (sources.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                Text("还没有源，先在上方添加一个", color = Color.Gray)
            }
        }

        LazyColumn {
            itemsIndexed(sources) { idx, s ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        Modifier
                            .weight(1f)
                            .clickable { persist(sources, s.id) }
                    ) {
                        val kind = if (s.type == Source.Type.JS_SPIDER) "JS" else "采集"
                        val mark = if (s.id == selectedId) "✓ 当前浏览：" else ""
                        Text(mark + s.name + "（" + kind + "）")
                        Text(s.api, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Text(
                        "删除",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .clickable {
                                val list = sources.toMutableList()
                                list.removeAt(idx)
                                persist(list, selectedId)
                            }
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}
