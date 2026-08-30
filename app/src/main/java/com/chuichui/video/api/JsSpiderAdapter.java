package com.chuichui.video.api;

import com.chuichui.video.bean.Category;
import com.chuichui.video.bean.Detail;
import com.chuichui.video.bean.Vod;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import java.io.IOException;
import java.util.List;

/**
 * 「JS 蜘蛛」源适配器：Rhino 执行用户 JS，脚本导出 <code>function spider(json)</code>，
 * 入参 JSON 描述操作（action=home/category/detail/search + 参数），返回苹果 CMS 形状 JSON，
 * 经 {@link MaccmsJson} 归一化为领域模型。脚本可调用 <code>fetchText(url)</code> 拉远程内容。
 */
public class JsSpiderAdapter implements SourceAdapter {

    /** api 字段：脚本源码，或指向脚本的 http(s) 地址（惰性拉取并缓存）。 */
    private final String api;
    private String resolvedJs;

    public JsSpiderAdapter(String api) {
        this.api = api;
    }

    /** 解析出 JS 脚本源码：api 是 http(s) 地址则拉取，否则视为脚本内容本身。 */
    private String js() throws IOException {
        if (resolvedJs != null) return resolvedJs;
        String src = api == null ? "" : api;
        if (src.startsWith("http://") || src.startsWith("https://")) {
            src = Http.text(src);
            if (src.isEmpty()) throw new IOException("无法拉取 JS 蜘蛛脚本: " + api);
        }
        resolvedJs = src;
        return src;
    }

    @Override
    public List<Category> home() throws IOException {
        return MaccmsJson.categories(JsonParser.parseString(call("home", null, null, null)).getAsJsonObject());
    }

    @Override
    public List<Vod> category(String typeId, int pg) throws IOException {
        return MaccmsJson.vods(JsonParser.parseString(call("category", "typeId", typeId, pg)).getAsJsonObject());
    }

    @Override
    public List<Vod> search(String kw, int pg) throws IOException {
        return MaccmsJson.vods(JsonParser.parseString(call("search", "wd", kw, pg)).getAsJsonObject());
    }

    @Override
    public Detail detail(String vodId) throws IOException {
        return MaccmsJson.detail(JsonParser.parseString(call("detail", "vodId", vodId, null)).getAsJsonObject());
    }

    /** 执行 JS 的 spider(json)：构造入参 JSON、评估脚本、调用并取回结果字符串。 */
    private String call(String action, String paramKey, String paramValue, Integer pg) throws IOException {
        JsonObject input = new JsonObject();
        input.addProperty("action", action);
        if (paramKey != null) input.addProperty(paramKey, paramValue);
        if (pg != null) input.addProperty("pg", pg);

        Context cx = Context.enter();
        try {
            cx.setOptimizationLevel(-1);
            Scriptable scope = cx.initStandardObjects();
            ScriptableObject.putProperty(scope, "fetchText", new BaseFunction() {
                @Override
                public Object call(Context c, Scriptable scope, Scriptable thisObj, Object[] args) {
                    try {
                        return Http.text(Context.toString(args[0]));
                    } catch (IOException e) {
                        return "";
                    }
                }
            });
            cx.evaluateString(scope, js(), "spider.js", 1, null);
            Object fn = ScriptableObject.getProperty(scope, "spider");
            if (fn instanceof Function) {
                return Context.toString(((Function) fn).call(cx, scope, scope, new Object[]{input.toString()}));
            }
            return "";
        } finally {
            Context.exit();
        }
    }
}
