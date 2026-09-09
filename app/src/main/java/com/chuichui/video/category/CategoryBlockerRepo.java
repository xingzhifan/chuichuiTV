package com.chuichui.video.category;

import android.content.Context;

import com.chuichui.video.data.JsonPref;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

/** 分类白名单仓库：本地持久化用户词表；未保存过时返回内置默认词表。 */
public class CategoryBlockerRepo {

    private final JsonPref<List<String>> pref;

    public CategoryBlockerRepo(Context c) {
        pref = new JsonPref<>(c, "chui_cat", "allowTerms", new TypeToken<List<String>>() {});
    }

    /** 当前生效词表；从未保存过 → 内置默认词表（等价「恢复默认」）。 */
    public synchronized List<String> load() {
        List<String> v = pref.get();
        return v != null ? new ArrayList<>(v) : new ArrayList<>(CategoryFilter.DEFAULT_TERMS);
    }

    /** 保存词表：null 不写（防脏数据）；空列表照常写入（=「清空全部 → 全隐藏」的合法语义）。 */
    public synchronized void save(List<String> terms) {
        if (terms == null) return;
        pref.set(terms);
    }
}
