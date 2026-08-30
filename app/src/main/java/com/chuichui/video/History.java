package com.chuichui.video;

import android.content.Context;

import com.chuichui.video.data.JsonPref;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

/**
 * 观看历史：本地 JSON 存储（按 片源+集+线路 去重，最新在前，最多 MAX 条）。
 * 重播 = 按记录的 vodId/vodName/episode/line 重建播放候选队列（天然支持故障切换，不依赖可能过期的直链）。
 */
public class History {
    public String vodId;
    public String vodName;
    public String episode;
    public String line;
    public long ts;

    public History() {
    }

    public History(String vodId, String vodName, String episode, String line) {
        this.vodId = vodId;
        this.vodName = vodName;
        this.episode = episode;
        this.line = line;
        this.ts = System.currentTimeMillis();
    }

    public String label() {
        return vodName + " · " + episode;
    }

    public static class Store {
        private static final int MAX = 50;
        private final JsonPref<List<History>> pref;

        public Store(Context c) {
            pref = new JsonPref<>(c, "chui_hist", "items", new TypeToken<List<History>>() {});
        }

        public synchronized void add(History h) {
            List<History> list = load();
            list.removeIf(x ->
                x.vodId != null && x.vodId.equals(h.vodId)
                    && x.episode != null && x.episode.equals(h.episode)
                    && x.line != null && x.line.equals(h.line));
            list.add(0, h);
            if (list.size() > MAX) list = list.subList(0, MAX);
            pref.set(list);
        }

        public synchronized List<History> load() {
            List<History> list = pref.get();
            return list != null ? list : new ArrayList<>();
        }
    }
}
