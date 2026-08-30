package com.chuichui.video.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/** 共享的 SharedPreferences+Gson JSON 存取（消除 History/SourceRepo 的重复存取形状）。 */
public class JsonPref<T> {

    private final SharedPreferences sp;
    private final String key;
    private final Gson gson = new Gson();
    private final TypeToken<T> type;

    public JsonPref(Context c, String pref, String key, TypeToken<T> type) {
        this.sp = c.getApplicationContext().getSharedPreferences(pref, Context.MODE_PRIVATE);
        this.key = key;
        this.type = type;
    }

    /** 读取；无值或解析失败返回 null。 */
    public synchronized T get() {
        String s = sp.getString(key, null);
        if (s == null) return null;
        try {
            return gson.fromJson(s, type.getType());
        } catch (Exception e) {
            return null;
        }
    }

    public synchronized void set(T value) {
        sp.edit().putString(key, gson.toJson(value)).apply();
    }
}
