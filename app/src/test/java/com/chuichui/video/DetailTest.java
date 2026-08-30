package com.chuichui.video;

import com.chuichui.video.bean.Detail;
import com.chuichui.video.bean.Episode;
import com.chuichui.video.bean.Line;
import com.chuichui.video.bean.Vod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * 验证源适配层的核心归一化逻辑：苹果 CMS 的 vod_play_from/$$$ 线路组 + vod_play_url/###/$ 拆解。
 */
public class DetailTest {

    @Test
    public void splitsLinesAndEpisodesCorrectly() {
        Vod vod = new Vod();
        vod.vodId = "123";
        vod.vodName = "示例电视剧";
        vod.vodPlayFrom = "线一$$$线二";
        vod.vodPlayUrl = "第01集$https://c1/ep1.m3u8#第02集$https://c1/ep2.m3u8$$$第01集$https://c2/ep1.m3u8";

        Detail d = new Detail(vod);

        assertEquals(2, d.lines.size());
        Line l0 = d.lines.get(0);
        assertEquals("线一", l0.name);
        assertEquals(2, l0.episodes.size());
        assertEquals("第01集", l0.episodes.get(0).name);
        assertEquals("https://c1/ep1.m3u8", l0.episodes.get(0).url);
        assertEquals("第02集", l0.episodes.get(1).name);
        assertEquals("https://c1/ep2.m3u8", l0.episodes.get(1).url);

        Line l1 = d.lines.get(1);
        assertEquals("线二", l1.name);
        assertEquals(1, l1.episodes.size());
        assertEquals("第01集", l1.episodes.get(0).name);
        assertEquals("https://c2/ep1.m3u8", l1.episodes.get(0).url);
    }

    @Test
    public void handlesEmptyPlayUrl() {
        Vod vod = new Vod();
        vod.vodPlayFrom = "线一";
        vod.vodPlayUrl = "";
        Detail d = new Detail(vod);
        assertEquals(1, d.lines.size());
        assertEquals(0, d.lines.get(0).episodes.size());
    }
}
