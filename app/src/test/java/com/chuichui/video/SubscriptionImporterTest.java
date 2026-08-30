package com.chuichui.video;

import com.chuichui.video.bean.Source;
import com.chuichui.video.subscription.SubscriptionImporter;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** 订阅批量导入：JSON 解析 / 按 api 去重 / 覆盖与追加 / 无效项统计。 */
public class SubscriptionImporterTest {

    private static final String JSON =
            "[{\"name\":\"采集站\",\"api\":\"https://a/api.php/provide/vod\",\"type\":\"maccms\"},"
            + "{\"name\":\"JS站\",\"api\":\"https://b/spider.js\",\"type\":\"js\"},"
            + "{\"name\":\"缺api\",\"type\":\"maccms\"}]";

    @Test
    public void parsesJsonIntoEntries() {
        SubscriptionImporter.Result r = SubscriptionImporter.fromJson(JSON, new ArrayList<Source>(), false);
        assertNotNull(r);
        assertEquals("无错误", null, r.error);
        assertEquals(2, r.sources.size());
        assertEquals("采集站", r.sources.get(0).name);
        assertEquals(Source.Type.JS_SPIDER, r.sources.get(1).type);
        // 缺 api 的那条计为 invalid
        assertEquals(1, r.invalid);
    }

    @Test
    public void appendModeDedupesByApiAgainstExisting() {
        List<Source> existing = new ArrayList<>();
        existing.add(new Source("已有", "https://a/api.php/provide/vod", Source.Type.MACCMS));

        SubscriptionImporter.Result r = SubscriptionImporter.fromJson(JSON, existing, false);
        // 已有 a 站 + 新增 b 站；a 站那条 skip；缺 api 那条 invalid
        assertEquals(2, r.sources.size());
        assertEquals("已有", r.sources.get(0).name);   // 现有源保留且在前
        assertEquals("JS站", r.sources.get(1).name);   // 新增追加其后
        assertEquals(1, r.imported);
        assertEquals(1, r.skippedDupes);
        assertEquals(1, r.invalid);
    }

    @Test
    public void overwriteModeReplacesAll() {
        List<Source> existing = new ArrayList<>();
        existing.add(new Source("旧源", "https://old/api.php/provide/vod", Source.Type.MACCMS));

        SubscriptionImporter.Result r = SubscriptionImporter.fromJson(JSON, existing, true);
        // 覆盖：以导入的 2 条为最终列表，旧源被替换
        assertEquals(2, r.sources.size());
        assertEquals("采集站", r.sources.get(0).name);
        assertEquals(2, r.imported);
        assertEquals(0, r.skippedDupes);
        assertEquals(1, r.invalid);
    }

    @Test
    public void invalidJsonReturnsError() {
        SubscriptionImporter.Result r = SubscriptionImporter.fromJson("not-json", new ArrayList<Source>(), false);
        assertNotNull(r.error);
        assertTrue(r.sources.isEmpty());
    }
}
