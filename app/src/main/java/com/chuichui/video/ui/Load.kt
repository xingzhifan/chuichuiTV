package com.chuichui.video.ui

import android.content.Context
import com.chuichui.video.SourceRepo
import com.chuichui.video.api.SourceAdapter
import com.chuichui.video.api.SourceFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 共享加载层：取首个源并创建其适配器，后台线程执行；未配置源或异常一律落空列表。 */
internal suspend fun <T> loadFromSource(context: Context, block: (SourceAdapter) -> List<T>): List<T> =
    withContext(Dispatchers.IO) {
        val sources = SourceRepo(context).load()
        if (sources.isEmpty()) {
            emptyList()
        } else {
            try {
                block(SourceFactory.create(sources.first()))
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
