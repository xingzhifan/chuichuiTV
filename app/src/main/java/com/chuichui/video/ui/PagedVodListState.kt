package com.chuichui.video.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.chuichui.video.bean.Vod
import com.chuichui.video.bean.VodPage

/**
 * 分页列表状态：持有累计 vods + 翻页游标 + 加载态，供分类/搜索列表的滚动加载复用。
 * 判据来自服务端 pagecount（无则视为不可分页），避免「空页/不足条数」启发式。
 */
class PagedVodListState(
    private val load: suspend (page: Int) -> VodPage,
) {
    var vods by mutableStateOf<List<Vod>>(emptyList()); private set
    var page by mutableStateOf(1); private set
    var loading by mutableStateOf(true); private set
    var hasMore by mutableStateOf(true); private set

    /** 首次加载（或重新进入：分类切换/新搜索）——重置到第 1 页。 */
    suspend fun loadFirst() {
        loading = true
        page = 1
        val r = load(1)
        vods = r.vods
        hasMore = r.hasMore(1)
        loading = false
    }

    /** 追加下一页；没有更多页或已在加载中则直接跳过。 */
    suspend fun loadNext() {
        if (loading || !hasMore) return
        loading = true
        val next = page + 1
        val r = load(next)
        vods = vods + r.vods
        page = next
        hasMore = r.hasMore(next)
        loading = false
    }
}
