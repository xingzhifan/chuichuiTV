package com.chuichui.video.play

import android.content.Context
import com.chuichui.video.SourceRepo
import com.chuichui.video.api.SourceFactory
import com.chuichui.video.bean.Line
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

/** 聚合层：聚合所有源，为某一集构建按优先级排序的播放候选队列（当前源/当前线路优先，失败源靠后）。 */
object PlaybackResolver {

    suspend fun buildQueue(
        context: Context,
        vodId: String,
        vodName: String,
        preferLine: String,
        episode: String,
    ): PlayQueue = withContext(Dispatchers.IO) {
        val repo = SourceRepo(context)
        val sources = repo.load()
        if (sources.isEmpty()) {
            return@withContext PlayQueue(emptyList(), SourceHealth.GLOBAL)
        }
        val selId = repo.selectedId()
        val current = sources.firstOrNull { it.id == selId } ?: sources.first()
        val others = sources.filter { it.id != current.id }
            .sortedBy { SourceHealth.GLOBAL.failures(it.id) } // 健康度升序：失败多的靠后

        // 当前源：详情重取，优先线路排最前
        val currentCandidates = try {
            val detail = SourceFactory.create(current).detail(vodId)
            val preferred = detail.lines.firstOrNull { it.name == preferLine }
            val rest = detail.lines.filter { it.name != preferLine }
            toCandidates(listOfNotNull(preferred) + rest, current.id, current.name)
        } catch (e: Exception) {
            emptyList()
        }

        // 其他源：按片源名搜索 → 详情 → 同名集候选（并发查询）
        val othersCandidates = others.map { src ->
            async {
                try {
                    val adapter = SourceFactory.create(src)
                    val vods = adapter.search(vodName, 1)
                    val match = vods.firstOrNull { it.vodName == vodName } ?: vods.firstOrNull()
                    if (match == null) emptyList()
                    else toCandidates(SourceFactory.create(src).detail(match.vodId).lines, src.id, src.name)
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }.awaitAll()

        PlayQueue(PlayCandidates.order(currentCandidates, othersCandidates, episode), SourceHealth.GLOBAL)
    }

    private fun toCandidates(lines: List<Line>, sourceId: String, sourceName: String): List<PlayCandidate> =
        lines.flatMap { line ->
            line.episodes.map { ep -> PlayCandidate(sourceId, sourceName, line.name, ep.name, ep.url) }
        }
}
