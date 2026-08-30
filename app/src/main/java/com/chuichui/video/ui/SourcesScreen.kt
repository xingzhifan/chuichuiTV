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

/** 源管理：增删源（采集API / JS 蜘蛛）并持久化；点列表项=设为当前浏览源。 */
@Composable
fun SourcesScreen(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { SourceRepo(ctx) }
    var sources by remember { mutableStateOf(repo.load()) }
    var selected by remember { mutableStateOf(repo.selected()) }
    var name by remember { mutableStateOf("") }
    var api by remember { mutableStateOf("") }
    var isJs by remember { mutableStateOf(false) }

    fun persist(list: List<Source>, sel: Int) {
        val clamped = if (list.isEmpty()) 0 else sel.coerceIn(0, list.size - 1)
        repo.save(list)
        repo.setSelected(clamped)
        sources = list
        selected = clamped
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
            TypeChip("采集API", !isJs) { isJs = false }
            TypeChip("JS蜘蛛", isJs) { isJs = true }
            Spacer(Modifier.weight(1f))
            Button(onClick = {
                if (name.isNotBlank() && api.isNotBlank()) {
                    val list = sources.toMutableList()
                    list.add(Source(name.trim(), api.trim(), if (isJs) Source.Type.JS_SPIDER else Source.Type.MACCMS))
                    persist(list, selected)
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
                            .clickable { persist(sources, idx) }
                    ) {
                        val kind = if (s.type == Source.Type.JS_SPIDER) "JS" else "采集"
                        val mark = if (idx == selected) "✓ 当前浏览：" else ""
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
                                persist(list, selected)
                            }
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TypeChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        modifier = Modifier
            .padding(end = 8.dp)
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable { onClick() }
    )
}
