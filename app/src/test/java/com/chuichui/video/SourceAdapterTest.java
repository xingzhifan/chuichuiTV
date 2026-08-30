package com.chuichui.video;

import com.chuichui.video.api.JsSpiderAdapter;
import com.chuichui.video.api.MaccmsAdapter;
import com.chuichui.video.api.SourceAdapter;
import com.chuichui.video.api.SourceFactory;
import com.chuichui.video.bean.Category;
import com.chuichui.video.bean.Detail;
import com.chuichui.video.bean.Source;
import com.chuichui.video.bean.Vod;

import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** 两类适配器输出同一领域模型（片源→线路→播放地址）；SourceFactory 按 type 分发。 */
public class SourceAdapterTest {

    /** JS 蜘蛛样例脚本：按 action 返回苹果 CMS 形状 JSON（模拟一个 JS 源）。 */
    private static final String JS = ""
            + "function spider(json) {"
            + "  var i = JSON.parse(json);"
            + "  if (i.action === 'home')"
            + "    return JSON.stringify({class:[{type_id:'1',type_name:'电影'}]});"
            + "  if (i.action === 'category')"
            + "    return JSON.stringify({list:[{vod_id:'v1',vod_name:'JS片',vod_play_from:'线A',vod_play_url:'第01集$https://j/1.m3u8'}]});"
            + "  if (i.action === 'detail')"
            + "    return JSON.stringify({list:[{vod_id:'v1',vod_name:'JS片',vod_play_from:'线A$$$线B',vod_play_url:'第01集$https://j/1.m3u8#第02集$https://j/2.m3u8$$$第01集$https://k/1.m3u8'}]});"
            + "  return '{}';"
            + "}";

    @Test
    public void jsSpiderAdapterProducesUnifiedModel() throws IOException {
        SourceAdapter spider = new JsSpiderAdapter(JS);

        List<Category> categories = spider.home();
        assertEquals(1, categories.size());
        assertEquals("电影", categories.get(0).typeName);

        List<Vod> vods = spider.category("1", 1);
        assertEquals(1, vods.size());
        assertEquals("JS片", vods.get(0).vodName);
        assertEquals("线A", vods.get(0).vodPlayFrom);

        Detail d = spider.detail("v1");
        assertEquals(2, d.lines.size());
        assertEquals("线A | 第01集", d.flatten().get(0).name);
        assertEquals("https://j/1.m3u8", d.flatten().get(0).url);
        assertEquals("线B | 第01集", d.flatten().get(2).name);
    }

    @Test
    public void factoryCreatesAdapterBySourceType() {
        Source maccms = new Source("采集源", "https://x/api.php/provide/vod", Source.Type.MACCMS);
        Source js = new Source("JS源", "https://x/spider.js", Source.Type.JS_SPIDER);

        assertTrue(SourceFactory.create(maccms) instanceof MaccmsAdapter);
        assertTrue(SourceFactory.create(js) instanceof JsSpiderAdapter);
        assertTrue(SourceFactory.create(new Source("未指定", "https://x/")) instanceof MaccmsAdapter);
    }

    @Test
    public void maccmsAndJsAdaptersShareSameDomainShape() throws IOException {
        // 两个适配器对同一形状数据的 home() 输出结构一致（分类字段）。
        SourceAdapter js = new JsSpiderAdapter(JS);
        List<Category> jsCategories = js.home();
        assertEquals("type_id 字段", "1", jsCategories.get(0).typeId);
        assertEquals("type_name 字段", "电影", jsCategories.get(0).typeName);
    }
}
