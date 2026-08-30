package com.chuichui.video;

import android.content.Context;

import com.chuichui.video.bean.Source;
import com.chuichui.video.data.JsonPref;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

/** 订阅/源仓库：本地保存用户配置的多个源与当前选中（按稳定 id）。空壳定位，内容由用户源决定。 */
public class SourceRepo {

    private final JsonPref<List<Source>> pref;
    private final JsonPref<String> selPref;

    public SourceRepo(Context c) {
        pref = new JsonPref<>(c, "chui_src", "list", new TypeToken<List<Source>>() {});
        selPref = new JsonPref<>(c, "chui_src", "selectedId", new TypeToken<String>() {});
    }

    public synchronized void save(List<Source> list) {
        pref.set(list);
    }

    /** 已配置的源；未配置时返回空列表（纯空壳：不预置任何源）。 */
    public synchronized List<Source> load() {
        List<Source> list = pref.get();
        return list != null ? list : new ArrayList<>();
    }

    /** 当前浏览源的稳定 id；空串 = 未选中（加载层回退到首个源）。 */
    public synchronized String selectedId() {
        String id = selPref.get();
        return id == null ? "" : id;
    }

    public synchronized void setSelectedId(String id) {
        selPref.set(id);
    }
}
