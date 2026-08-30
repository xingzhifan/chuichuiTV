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
    private final JsonPref<Integer> selPref;

    public SourceRepo(Context c) {
        pref = new JsonPref<>(c, "chui_src", "list", new TypeToken<List<Source>>() {});
        selPref = new JsonPref<>(c, "chui_src", "selected", new TypeToken<Integer>() {});
    }

    public synchronized void save(List<Source> list) {
        pref.set(list);
    }

    /** 已配置的源；未配置时返回空列表（纯空壳：不预置任何源）。 */
    public synchronized List<Source> load() {
        List<Source> list = pref.get();
        return list != null ? list : new ArrayList<>();
    }

    /** 当前浏览的源索引（首页/搜索用它；故障切换在 ticket 04 扩展到全源）。 */
    public synchronized int selected() {
        Integer i = selPref.get();
        return i == null ? 0 : i;
    }

    public synchronized void setSelected(int index) {
        selPref.set(index);
    }
}
