package com.chuichui.video.api;

import com.chuichui.video.bean.Category;
import com.chuichui.video.bean.Detail;
import com.chuichui.video.bean.Vod;

import java.io.IOException;
import java.util.List;

/**
 * 源适配器：把一种源协议归一化为领域模型（分类→片源→线路→播放地址）。
 * 实现：{@link MaccmsAdapter}（苹果 CMS 采集 API）、{@link JsSpiderAdapter}（JS 蜘蛛）。
 */
public interface SourceAdapter {

    /** 分类列表（浏览入口）。 */
    List<Category> home() throws IOException;

    /** 某分类下的片源列表（第 pg 页）。 */
    List<Vod> category(String typeId, int pg) throws IOException;

    /** 关键词搜索片源。 */
    List<Vod> search(String kw, int pg) throws IOException;

    /** 片源详情：片源 + 多线路 + 每线路的集。 */
    Detail detail(String vodId) throws IOException;
}
