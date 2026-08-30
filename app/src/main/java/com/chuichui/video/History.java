package com.chuichui.video;

import android.content.Context;

import com.chuichui.video.data.JsonPref;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

/** 观看历史：本地 JSON 存储（MVP，不联网）。 */
public class History {
    public String title;
    public String url;
    public long ts;

    public History() {
    }

    public History(String title, String url) {
        this.title = title;
        this.url = url;
        this.ts = System.currentTimeMillis();
    }

    public static class Store {
        private static final int MAX = 50;
        private final JsonPref<List<History>> pref;

        public Store(Context c) {
            pref = new JsonPref<>(c, "chui_hist", "items", new TypeToken<List<History>>() {});
        }

        public synchronized void add(String title, String url) {
            List<History> list = load();
            list.removeIf(h -> h.url != null && h.url.equals(url));
            list.add(0, new History(title, url));
            if (list.size() > MAX) list = list.subList(0, MAX);
            pref.set(list);
        }

        public synchronized List<History> load() {
            List<History> list = pref.get();
            return list != null ? list : new ArrayList<>();
        }
    }
}
