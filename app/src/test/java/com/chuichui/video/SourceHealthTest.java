package com.chuichui.video;

import com.chuichui.video.play.SourceHealth;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** 源健康度：失败降级排序 + 成功恢复。 */
public class SourceHealthTest {

    @Test
    public void rankIdsPutsFailedSourcesLast() {
        SourceHealth h = new SourceHealth();
        h.bumpFailure("s1");
        h.bumpFailure("s1");   // s1 失败两次
        h.bumpFailure("s2");   // s2 失败一次
        // s3 未失败

        List<String> ranked = h.rankIds(Arrays.asList("s1", "s2", "s3"));

        assertEquals("s3", ranked.get(0)); // 未失败最前
        assertEquals("s2", ranked.get(1));
        assertEquals("s1", ranked.get(2)); // 失败最多最后
    }

    @Test
    public void markSuccessRestoresHealth() {
        SourceHealth h = new SourceHealth();
        h.bumpFailure("s1");
        h.bumpFailure("s1");
        h.markSuccess("s1");
        assertEquals(0, h.failures("s1"));
    }
}
