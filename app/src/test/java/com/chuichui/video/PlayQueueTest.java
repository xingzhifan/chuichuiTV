package com.chuichui.video;

import com.chuichui.video.play.PlayCandidate;
import com.chuichui.video.play.PlayQueue;
import com.chuichui.video.play.PlayCandidates;
import com.chuichui.video.play.SourceHealth;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** 抗源失效核心逻辑：候选全局排序、失败推进、源健康降级。 */
public class PlayQueueTest {

    private static PlayCandidate c(String srcId, String srcName, String line, String ep, String url) {
        return new PlayCandidate(srcId, srcName, line, ep, url);
    }

    @Test
    public void orderPrefersCurrentLineThenCurrentSourceThenOthers() {
        List<PlayCandidate> current = Arrays.asList(
            c("s1", "源一", "线1", "第01集", "u1"),   // 当前线路
            c("s1", "源一", "线2", "第01集", "u2"),   // 当前源其他线路
            c("s1", "源一", "线1", "第02集", "u3"));  // 不同集（不参与同名匹配）

        Map<String, List<PlayCandidate>> others = new LinkedHashMap<>();
        others.put("s2", Collections.singletonList(c("s2", "源二", "线A", "第01集", "u4")));
        others.put("s3", Collections.singletonList(c("s3", "源三", "线B", "花絮", "u5"))); // 无同名集

        List<PlayCandidate> out = PlayCandidates.order(
            current, new ArrayList<>(others.values()), "第01集");

        // 期望：所点 → 同源其他线路同名 → 跨源同名(健康序) → 同源不同集(最后手段) → 无同名源首个
        assertEquals(5, out.size());
        assertEquals("u1", out.get(0).url);   // 所点：线1/第01集
        assertEquals("u2", out.get(1).url);   // 同源其他线路：线2/第01集
        assertEquals("u4", out.get(2).url);   // 跨源同名：s2/第01集
        assertEquals("u3", out.get(3).url);   // 同源不同集（最后手段）
        assertEquals("u5", out.get(4).url);   // s3 无同名集的首个（最末）
    }

    @Test
    public void fallsBackToFirstCandidatesWhenNothingMatches() {
        List<PlayCandidate> current = Collections.singletonList(c("s1", "源一", "线1", "第01集", "u1"));
        List<List<PlayCandidate>> others = Collections.singletonList(
            Collections.singletonList(c("s2", "源二", "线A", "正片", "u4")));

        List<PlayCandidate> out = PlayCandidates.order(current, others, "不存在的集");

        assertEquals(2, out.size()); // 全无同名 → 兜底：当前源候选 + 其他源首个
        assertEquals("u1", out.get(0).url);
        assertEquals("u4", out.get(1).url);
    }

    @Test
    public void queueAdvancesAndDegradesFailedSource() {
        SourceHealth health = new SourceHealth();
        List<PlayCandidate> cands = Arrays.asList(
            c("s1", "源一", "线1", "第01集", "u1"),
            c("s2", "源二", "线A", "第01集", "u2"));
        PlayQueue q = new PlayQueue(cands, health);

        assertEquals("u1", q.current().url);
        q.markFailedAndAdvance();                       // s1 失败 → 降级并切下一候选
        assertEquals("s2", q.current().sourceId);
        assertEquals(1, health.failures("s1"));
        assertTrue("失败源排名应劣于未失败源", health.failures("s1") > health.failures("s2"));

        q.markFailedAndAdvance();                       // s2 也失败 → 耗尽
        assertTrue(q.exhausted());
        assertNull(q.current());
        assertEquals(1, health.failures("s2"));
    }

    @Test
    public void matchesEpisodesAcrossNamingFormats() {
        List<PlayCandidate> current = Collections.singletonList(c("s1", "源一", "线1", "第01集", "u1"));
        List<List<PlayCandidate>> others = Collections.singletonList(
            Collections.singletonList(c("s2", "源二", "线A", "01", "u2")));   // 集名格式不同但数字一致

        List<PlayCandidate> out = PlayCandidates.order(current, others, "第01集");

        assertEquals(2, out.size());
        assertEquals("u1", out.get(0).url);   // 当前源同名在前
        assertEquals("u2", out.get(1).url);   // 跨源数字归一匹配次之
    }

    @Test
    public void dedupesSameUrlCandidates() {
        List<PlayCandidate> current = Arrays.asList(
            c("s1", "源一", "线1", "第01集", "same.mp4"),
            c("s1", "源一", "线2", "第01集", "same.mp4")); // 两线路同地址 → 去重
        List<List<PlayCandidate>> others = Collections.emptyList();

        List<PlayCandidate> out = PlayCandidates.order(current, others, "第01集");
        assertEquals(1, out.size());
    }
}
