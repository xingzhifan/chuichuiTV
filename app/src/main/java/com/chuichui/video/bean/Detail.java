package com.chuichui.video.bean;

import java.util.ArrayList;
import java.util.List;

/**
 * 片源详情（Detail）：一个片源 + 其多线路、每线路的集。
 * 根据苹果 CMS 的 vod_play_from($$$分隔线路组) 与 vod_play_url($$$分隔组、#分隔集、$分隔 名+url) 拆解。
 */
public class Detail {
    public Vod vod;
    public List<Line> lines = new ArrayList<>();

    public Detail() {
    }

    public Detail(Vod vod) {
        this.vod = vod;
        parse();
    }

    private void parse() {
        if (vod == null || vod.vodPlayFrom == null || vod.vodPlayUrl == null) return;
        String[] from = vod.vodPlayFrom.split("\\$\\$\\$");
        String[] groups = vod.vodPlayUrl.split("\\$\\$\\$");
        for (int i = 0; i < from.length; i++) {
            Line line = new Line();
            line.name = from[i].trim();
            String group = i < groups.length ? groups[i] : "";
            for (String ep : group.split("#")) {
                if (ep.isEmpty()) continue;
                int idx = ep.indexOf('$');
                if (idx < 0) line.episodes.add(new Episode(ep, ep));
                else line.episodes.add(new Episode(ep.substring(0, idx), ep.substring(idx + 1)));
            }
            lines.add(line);
        }
    }
}
