package com.chuichui.video.bean;

import java.util.Collections;
import java.util.List;

/**
 * 一页片源结果：当页列表 + 分页元数据（来自苹果 CMS 的 pagecount/total）。
 * 供 UI 滚动加载判定「还有没有下一页」，替代「空页/不足 N 条」这类不可靠启发式。
 */
public final class VodPage {

    /** 空页：列表为空，但视为「没有更多页」（用于加载失败的兜底，不触发继续翻页）。 */
    public static final VodPage EMPTY = new VodPage(Collections.emptyList(), 0, 0);

    public final List<Vod> vods;
    /** 总页数（服务端 pagecount）；0 表示未知/不可分页。 */
    public final int pageCount;
    /** 总条数（服务端 total）。 */
    public final int total;

    public VodPage(List<Vod> vods, int pageCount, int total) {
        this.vods = vods == null ? Collections.emptyList() : vods;
        this.pageCount = pageCount;
        this.total = total;
    }

    /** 第 pg 页之后是否还有更多页。pageCount 未知（<=0）时视为没有，避免无限翻页。 */
    public boolean hasMore(int pg) {
        return pageCount > 0 && pg < pageCount;
    }
}
