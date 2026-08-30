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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import com.chuichui.video.SourceRepo
import com.chuichui.video.bean.Source
import com.chuichui.video.subscription.SubscriptionFetcher
import com.chuichui.video.subscription.SubscriptionImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    var showImport by remember { mutableStateOf(false) }

    fun persist(list: List<Source>, selId: String) {
        repo.save(list)
        // 选中 id 不在列表里（被删/列表为空/覆盖后）时回退到首个源
        val effective = if (list.any { it.id == selId }) selId else (list.firstOrNull()?.id ?: "")
        repo.setSelectedId(effective)
        sources = list
        selectedId = effective
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("源管理", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Text(
                "批量导入",
                modifier = Modifier.clickable { showImport = true }.padding(8.dp),
                color = MaterialTheme.colorScheme.primary
            )
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

    if (showImport) {
        BatchImportDialog(
            onDismiss = { showImport = false },
            onImport = { result ->
                // 导入成功：持久化新列表并刷新（覆盖时选中回退到首个由 persist 处理）
                if (result.error == null) {
                    persist(result.sources, selectedId)
                }
            }
        )
    }
}

/** 批量导入对话框：粘贴 JSON 或填订阅 URL 二选一，按 api 去重，可选覆盖/追加。 */
@Composable
private fun BatchImportDialog(
    onDismiss: () -> Unit,
    onImport: (SubscriptionImporter.Result) -> Unit,
) {
    val ctx = LocalContext.current
    val repo = remember { SourceRepo(ctx) }
    val scope = rememberCoroutineScope()
    var byUrl by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    var overwrite by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf<String?>(null) }

    fun runImport() {
        val text = input.trim()
        if (text.isEmpty()) return
        busy = true
        report = null
        scope.launch {
            val existing = repo.load()
            val result = try {
                if (byUrl) {
                    val json = withContext(Dispatchers.IO) { SubscriptionFetcher.fetch(text) }
                    SubscriptionImporter.fromJson(json, existing, overwrite)
                } else {
                    SubscriptionImporter.fromJson(text, existing, overwrite)
                }
            } catch (e: Exception) {
                SubscriptionImporter.Result.failure(
                    "拉取订阅失败：" + (e.message ?: e.javaClass.simpleName)
                )
            }
            busy = false
            report = buildReport(result)
            if (result.error == null && result.sources.isNotEmpty()) onImport(result)
        }
    }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("批量导入源") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SelectChip("粘贴 JSON", !byUrl) { byUrl = false }
                    SelectChip("订阅 URL", byUrl) { byUrl = true }
                    Spacer(Modifier.weight(1f))
                    Text(
                        if (overwrite) "覆盖" else "追加",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { overwrite = !overwrite }.padding(8.dp)
                    )
                }
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text(if (byUrl) "订阅地址 https://…" else "[{\"name\":…,\"api\":…,\"type\":\"maccms|js\"}]") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                report?.let {
                    Text(it, color = if (it.startsWith("成功")) Color(0xFF2E7D32) else Color(0xFFB71C1C), modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { runImport() }, enabled = !busy && input.isNotBlank()) {
                Text(if (busy) "导入中…" else "导入")
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }, enabled = !busy) { Text("关闭") }
        }
    )
}

/** 生成导入结果的一句话说明。 */
private fun buildReport(r: SubscriptionImporter.Result): String {
    if (r.error != null) return "失败：" + r.error
    val extra = StringBuilder()
    extra.append("，当前共 ").append(r.sources.size).append(" 个源")
    if (r.imported > 0) extra.append("，新增 ").append(r.imported)
    if (r.skippedDupes > 0) extra.append("，跳过重复 ").append(r.skippedDupes)
    if (r.invalid > 0) extra.append("，忽略无效 ").append(r.invalid)
    return "成功：$extra"
}
