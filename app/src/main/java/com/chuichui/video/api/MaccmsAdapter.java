package com.chuichui.video.api;

import com.chuichui.video.bean.Category;
import com.chuichui.video.bean.Detail;
import com.chuichui.video.bean.Vod;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

/** 「苹果 CMS (maccms) 采集 API」源适配器。按源实例化（每个 Source 一个实例）。 */
public class MaccmsAdapter implements SourceAdapter {

    private final String base;

    public MaccmsAdapter(String base) {
        this.base = base;
    }

    @Override
    public List<Category> home() throws IOException {
        return MaccmsJson.categories(get("ac=list"));
    }

    @Override
    public List<Vod> category(String typeId, int pg) throws IOException {
        return MaccmsJson.vods(get("ac=videolist&t=" + enc(typeId) + "&pg=" + pg));
    }

    @Override
    public List<Vod> search(String kw, int pg) throws IOException {
        return MaccmsJson.vods(get("ac=videolist&wd=" + enc(kw) + "&pg=" + pg));
    }

    @Override
    public Detail detail(String vodId) throws IOException {
        return MaccmsJson.detail(get("ac=detail&ids=" + enc(vodId)));
    }

    private JsonObject get(String query) throws IOException {
        return JsonParser.parseString(Http.text(base + "?" + query)).getAsJsonObject();
    }

    private static String enc(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }
}
