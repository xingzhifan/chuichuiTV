package com.chuichui.video;

import com.chuichui.video.api.MaccmsJson;
import com.chuichui.video.bean.Category;
import com.chuichui.video.bean.Detail;
import com.chuichui.video.bean.Vod;
import com.chuichui.video.bean.VodPage;
import com.google.gson.JsonParser;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 苹果 CMS 形状 JSON → 领域 bean 的共享解析。 */
public class MaccmsJsonTest {

    private static final String LIST_JSON = "{\"list\":[{"
            + "\"vod_id\":\"123\",\"vod_name\":\"示例电视剧\",\"vod_pic\":\"https://p/1.jpg\","
            + "\"vod_remarks\":\"更新至12集\",\"type_name\":\"剧集\",\"vod_year\":\"2024\","
            + "\"vod_play_from\":\"线一$$$线二\","
            + "\"vod_play_url\":\"第01集$https://c1/ep1.m3u8#第02集$https://c1/ep2.m3u8$$$第01集$https://c2/ep1.m3u8\"}]}";

    private static final String CLASS_JSON = "{\"class\":["
            + "{\"type_id\":\"1\",\"type_name\":\"电影\"},"
            + "{\"type_id\":\"2\",\"type_name\":\"剧集\"}]}";

    @Test
    public void parsesCategories() {
        List<Category> out = MaccmsJson.categories(JsonParser.parseString(CLASS_JSON).getAsJsonObject());
        assertEquals(2, out.size());
        assertEquals("1", out.get(0).typeId);
        assertEquals("电影", out.get(0).typeName);
        assertEquals("剧集", out.get(1).typeName);
    }

    @Test
    public void parsesVodsAndDetailLines() {
        List<Vod> vods = MaccmsJson.vods(JsonParser.parseString(LIST_JSON).getAsJsonObject());
        assertEquals(1, vods.size());
        Vod v = vods.get(0);
        assertEquals("123", v.vodId);
        assertEquals("示例电视剧", v.vodName);
        assertEquals("更新至12集", v.vodRemarks);

        Detail d = MaccmsJson.detail(JsonParser.parseString(LIST_JSON).getAsJsonObject());
        assertEquals(2, d.lines.size());
        assertEquals(2, d.lines.get(0).episodes.size());
        assertEquals(1, d.lines.get(1).episodes.size());
        assertEquals("https://c2/ep1.m3u8", d.lines.get(1).episodes.get(0).url);
    }

    @Test
    public void parsesPageMetadataForPagination() {
        // 带 pagecount/total 的分页响应 → 可正确判定「还有没有下一页」。
        String pageJson = "{\"list\":[{\"vod_id\":\"1\",\"vod_name\":\"片1\"}],\"pagecount\":5,\"total\":92}";
        VodPage p = MaccmsJson.vodsPage(JsonParser.parseString(pageJson).getAsJsonObject());
        assertEquals(1, p.vods.size());
        assertEquals(5, p.pageCount);
        assertEquals(92, p.total);
        assertTrue("第1页之后还有更多页", p.hasMore(1));
        assertTrue("第4页之后还有更多页", p.hasMore(4));
        assertFalse("第5页是最后一页", p.hasMore(5));

        // 无 pagecount 时视为不可分页（Page 2 之后不再翻页）。
        VodPage nop = MaccmsJson.vodsPage(JsonParser.parseString(LIST_JSON).getAsJsonObject());
        assertFalse("无 pagecount 视为无更多页", nop.hasMore(1));
    }
}
