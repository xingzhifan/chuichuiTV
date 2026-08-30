package com.chuichui.video;

import android.content.Context;
import android.content.SharedPreferences;

import com.chuichui.video.bean.Source;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

/** 订阅/源仓库：本地保存用户配置的多个源（多源并存）。空壳定位，内容由用户源决定。 */
public class SourceRepo {
    private static final String PREF = "chui_src";
    private static final String KEY = "list";
    private final SharedPreferences sp;
    private final Gson gson = new Gson();

    public SourceRepo(Context c) {
        sp = c.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public synchronized void save(List<Source> list) {
        sp.edit().putString(KEY, gson.toJson(list)).apply();
    }

    public synchronized List<Source> load() {
        String s = sp.getString(KEY, null);
        if (s == null) {
            List<Source> def = new ArrayList<>();
            def.add(new Source("示例源", "https://api.example.com/api.php/provide/vod"));
            return def;
        }
        try {
            List<Source> list = gson.fromJson(s, new TypeToken<List<Source>>() {}.getType());
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
