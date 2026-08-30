package com.chuichui.video;

import android.content.Context;

import com.chuichui.video.bean.Source;
import com.chuichui.video.data.JsonPref;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

/** 订阅/源仓库：本地保存用户配置的多个源（多源并存）。空壳定位，内容由用户源决定。 */
public class SourceRepo {

    private final JsonPref<List<Source>> pref;

    public SourceRepo(Context c) {
        pref = new JsonPref<>(c, "chui_src", "list", new TypeToken<List<Source>>() {});
    }

    public synchronized void save(List<Source> list) {
        pref.set(list);
    }

    /** 已配置的源；未配置时返回空列表（纯空壳：不预置任何源）。 */
    public synchronized List<Source> load() {
        List<Source> list = pref.get();
        return list != null ? list : new ArrayList<>();
    }
}
