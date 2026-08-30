package com.chuichui.video.ui

import android.content.Context
import com.chuichui.video.SourceRepo
import com.chuichui.video.api.SourceAdapter
import com.chuichui.video.api.SourceFactory
import com.chuichui.video.bean.VodPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 共享加载层：取「当前选中源」（按稳定 id，缺省回退首个）的适配器，后台线程执行；未配置源或异常一律落空列表。 */
internal suspend fun <T> loadFromSource(context: Context, block: (SourceAdapter) -> List<T>): List<T> =
    withContext(Dispatchers.IO) {
        val adapter = currentAdapter(context) ?: return@withContext emptyList()
        try {
            block(adapter)
        } catch (e: Exception) {
            emptyList()
        }
    }

/** 分页加载层：取当前选中源适配器，拉第 pg 页（含分页元数据）。未配置源或异常返回空页（VodPage.EMPTY，不再翻页）。 */
internal suspend fun loadVodPageFromSource(context: Context, pg: Int, block: (SourceAdapter) -> VodPage): VodPage =
    withContext(Dispatchers.IO) {
        val adapter = currentAdapter(context) ?: return@withContext VodPage.EMPTY
        try {
            block(adapter)
        } catch (e: Exception) {
            VodPage.EMPTY
        }
    }

/** 解析当前选中源（未配置返回 null）。 */
private fun currentAdapter(context: Context): SourceAdapter? {
    val repo = SourceRepo(context)
    val sources = repo.load()
    if (sources.isEmpty()) return null
    val selId = repo.selectedId()
    val source = sources.firstOrNull { it.id == selId } ?: sources.first()
    return SourceFactory.create(source)
}
