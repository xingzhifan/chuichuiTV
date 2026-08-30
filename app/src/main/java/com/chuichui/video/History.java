package com.chuichui.video;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

/** 观看历史：本地 SharedPreferences + JSON 存储（MVP，不联网）。 */
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
        private static final String PREF = "chui_hist";
        private static final String KEY = "items";
        private final SharedPreferences sp;
        private final Gson gson = new Gson();

        public Store(Context c) {
            sp = c.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
        }

        public synchronized void add(String title, String url) {
            List<History> list = load();
            list.removeIf(h -> h.url != null && h.url.equals(url));
            list.add(0, new History(title, url));
            if (list.size() > 50) list = list.subList(0, 50);
            sp.edit().putString(KEY, gson.toJson(list)).apply();
        }

        public synchronized List<History> load() {
            String s = sp.getString(KEY, null);
            if (s == null) return new ArrayList<>();
            try {
                List<History> list = gson.fromJson(s, new TypeToken<List<History>>() {}.getType());
                return list != null ? list : new ArrayList<>();
            } catch (Exception e) {
                return new ArrayList<>();
            }
        }
    }
}
