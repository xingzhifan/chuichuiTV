package com.chuichui.video.ui

import android.content.Context
import com.chuichui.video.SourceRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 共享加载层：取首个源的 api，在后台线程执行；未配置源或异常一律落空列表。 */
internal suspend fun <T> loadFromSource(context: Context, block: (api: String) -> List<T>): List<T> =
    withContext(Dispatchers.IO) {
        val api = SourceRepo(context).firstApi()
        try {
            if (api.isEmpty()) emptyList() else block(api)
        } catch (e: Exception) {
            emptyList()
        }
    }
