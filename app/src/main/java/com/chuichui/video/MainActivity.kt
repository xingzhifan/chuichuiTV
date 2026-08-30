package com.chuichui.video

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chuichui.video.api.Maccms
import com.chuichui.video.bean.Site
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { Browse() } }
    }
}

/** 主源类别表：拉取第一个源的分类；含加载/空态占位。 */
@Composable
fun Browse() {
    val ctx = LocalContext.current
    var sites by remember { mutableStateOf<List<Site>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        sites = withContext(Dispatchers.IO) {
            val api = try { SourceRepo(ctx).load().first().api } catch (e: Exception) { "" }
            try { if (api.isEmpty()) emptyList() else Maccms(api).home() } catch (e: Exception) { emptyList() }
        }
        loading = false
    }
    when {
        loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("加载中…", color = Color.Gray)
        }
        sites.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "未配置可用源\n请稍后接入采集源",
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
        else -> LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
            items(sites) { s ->
                Text(s.typeName, modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp))
            }
        }
    }
}
