package com.chuichui.video.api;

import com.chuichui.video.bean.Detail;
import com.chuichui.video.bean.Site;
import com.chuichui.video.bean.Vod;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 「苹果 CMS (maccms) 采集 API」源适配器。按源实例化（每个 Source 一个实例）。
 * 覆盖：分类(ac=list)、分类片源(ac=videolist&t=&pg=)、搜索(ac=videolist&wd=)、详情(ac=detail&ids=)。
 */
public class Maccms {

    private final String base;
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();

    public Maccms(String base) {
        this.base = base;
    }

    public List<Site> home() throws IOException {
        JsonObject obj = get("ac=list");
        List<Site> sites = new ArrayList<>();
        JsonArray a = obj.has("class") ? obj.getAsJsonArray("class") : new JsonArray();
        for (JsonElement e : a) {
            JsonObject c = e.getAsJsonObject();
            sites.add(new Site(get(c, "type_id"), get(c, "type_name")));
        }
        return sites;
    }

    public List<Vod> category(String tid, int pg) throws IOException {
        return list("ac=videolist&t=" + tid + "&pg=" + pg);
    }

    public List<Vod> search(String kw, int pg) throws IOException {
        return list("ac=videolist&wd=" + enc(kw) + "&pg=" + pg);
    }

    public Detail detail(String vodId) throws IOException {
        JsonObject obj = get("ac=detail&ids=" + vodId);
        JsonArray a = obj.has("list") ? obj.getAsJsonArray("list") : new JsonArray();
        if (a.size() == 0) return new Detail();
        return new Detail(vod(a.get(0).getAsJsonObject()));
    }

    private List<Vod> list(String query) throws IOException {
        JsonObject obj = get(query);
        List<Vod> vods = new ArrayList<>();
        JsonArray a = obj.has("list") ? obj.getAsJsonArray("list") : new JsonArray();
        for (JsonElement e : a) vods.add(vod(e.getAsJsonObject()));
        return vods;
    }

    private JsonObject get(String query) throws IOException {
        Request req = new Request.Builder().url(base + "?" + query).build();
        try (Response res = client.newCall(req).execute()) {
            if (res.body() == null) throw new IOException("empty body");
            return JsonParser.parseString(res.body().string()).getAsJsonObject();
        }
    }

    private static Vod vod(JsonObject o) {
        Vod v = new Vod();
        v.vodId = get(o, "vod_id");
        v.vodName = get(o, "vod_name");
        v.vodPic = get(o, "vod_pic");
        v.vodRemarks = get(o, "vod_remarks");
        v.typeName = get(o, "type_name");
        v.vodYear = get(o, "vod_year");
        v.vodPlayFrom = get(o, "vod_play_from");
        v.vodPlayUrl = get(o, "vod_play_url");
        return v;
    }

    private static String get(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
    }

    private static String enc(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }
}
