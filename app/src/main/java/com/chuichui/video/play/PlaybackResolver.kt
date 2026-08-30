package com.chuichui.video.play;

import android.content.Context
import com.chuichui.video.SourceRepo
import com.chuichui.video.api.SourceFactory
import com.chuichui.video.bean.Line
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

/** 播放请求：定位某一集所需的全部信息（消除裸 String 参数束）。 */
data class PlayRequest(
    val vodId: String,
    val vodName: String,
    val episode: String,
    val line: String,
)

/** 聚合层：聚合所有源，为某一集构建按优先级排序的播放候选队列（当前源/当前线路优先，失败源靠后）。 */
object PlaybackResolver {

    suspend fun buildQueue(context: Context, request: PlayRequest): PlayQueue =
        withContext(Dispatchers.IO) {
            val repo = SourceRepo(context)
            val sources = repo.load()
            if (sources.isEmpty()) {
                return@withContext PlayQueue(emptyList(), SourceHealth.GLOBAL)
            }
            val selId = repo.selectedId()
            val current = sources.firstOrNull { it.id == selId } ?: sources.first()
            val others = SourceHealth.GLOBAL.rankIds(
                sources.filter { it.id != current.id }.map { it.id }
            ).mapNotNull { id -> sources.firstOrNull { it.id == id } }

            // 当前源：所点(线路,集)最先 → 本线其余集 → 其他线路（全部保留：同源其他线路是同一内容的不同流）
            val currentCandidates = try {
                val detail = SourceFactory.create(current).detail(request.vodId)
                val candidates = mutableListOf<PlayCandidate>()
                val preferredLine = detail.lines.firstOrNull { it.name == request.line }
                preferredLine?.let { line ->
                    line.episodes.sortedByDescending { it.name == request.episode }.forEach { ep ->
                        candidates.add(PlayCandidate(current.id, current.name, line.name, ep.name, ep.url))
                    }
                }
                detail.lines.filter { it.name != request.line }.forEach { line ->
                    line.episodes.forEach { ep ->
                        candidates.add(PlayCandidate(current.id, current.name, line.name, ep.name, ep.url))
                    }
                }
                candidates
            } catch (e: Exception) {
                emptyList()
            }

            // 其他源：严格按片源名搜索 → 详情 → 同名集候选（并发查询；无同名不聚合，避免选错片）
            val othersCandidates = others.map { src ->
                async {
                    val adapter = SourceFactory.create(src)
                    try {
                        val vods = adapter.search(request.vodName, 1).vods
                        val match = vods.firstOrNull { it.vodName == request.vodName }
                            ?: return@async emptyList<PlayCandidate>()
                        toCandidates(adapter.detail(match.vodId).lines, src.id, src.name)
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
            }.awaitAll()

            PlayQueue(PlayCandidates.order(currentCandidates, othersCandidates, request.episode), SourceHealth.GLOBAL)
        }

    private fun toCandidates(lines: List<Line>, sourceId: String, sourceName: String): List<PlayCandidate> =
        lines.flatMap { line ->
            line.episodes.map { ep -> PlayCandidate(sourceId, sourceName, line.name, ep.name, ep.url) }
        }
}
