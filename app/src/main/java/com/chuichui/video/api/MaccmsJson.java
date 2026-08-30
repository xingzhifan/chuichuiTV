package com.chuichui.video.api;

import com.chuichui.video.bean.Category;
import com.chuichui.video.bean.Detail;
import com.chuichui.video.bean.Vod;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/** 苹果 CMS 形状 JSON → 领域 bean 的共享解析（MaccmsAdapter 与 JsSpiderAdapter 复用）。 */
public final class MaccmsJson {

    private MaccmsJson() {
    }

    /** class[] → 分类列表。 */
    public static List<Category> categories(JsonObject obj) {
        List<Category> out = new ArrayList<>();
        JsonArray a = obj.has("class") ? obj.getAsJsonArray("class") : new JsonArray();
        for (JsonElement e : a) {
            JsonObject c = e.getAsJsonObject();
            out.add(new Category(str(c, "type_id"), str(c, "type_name")));
        }
        return out;
    }

    /** list[] → 片源列表。 */
    public static List<Vod> vods(JsonObject obj) {
        List<Vod> out = new ArrayList<>();
        JsonArray a = obj.has("list") ? obj.getAsJsonArray("list") : new JsonArray();
        for (JsonElement e : a) out.add(vod(e.getAsJsonObject()));
        return out;
    }

    /** list[0] → 片源详情（含线路/集）。 */
    public static Detail detail(JsonObject obj) {
        JsonArray a = obj.has("list") ? obj.getAsJsonArray("list") : new JsonArray();
        if (a.size() == 0) return new Detail();
        return new Detail(vod(a.get(0).getAsJsonObject()));
    }

    public static Vod vod(JsonObject o) {
        Vod v = new Vod();
        v.vodId = str(o, "vod_id");
        v.vodName = str(o, "vod_name");
        v.vodPic = str(o, "vod_pic");
        v.vodRemarks = str(o, "vod_remarks");
        v.typeName = str(o, "type_name");
        v.vodYear = str(o, "vod_year");
        v.vodPlayFrom = str(o, "vod_play_from");
        v.vodPlayUrl = str(o, "vod_play_url");
        return v;
    }

    static String str(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
    }
}
