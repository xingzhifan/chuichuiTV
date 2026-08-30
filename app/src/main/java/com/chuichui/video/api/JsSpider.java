package com.chuichui.video.api;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 「JS 蜘蛛」源适配器：用 Rhino 执行用户提供的 JS 爬虫脚本（jsSource），脚本约定导出
 * <code>function spider(json)</code>，入参为该次操作的 JSON（如 {"action":"home"}），
 * 返回与苹果 CMS 相同的 JSON（class/list/vod_play_from/vod_play_url），即可复用 Maccms 的
 * 领域 bean 解析，归一化到 片源→源→线路→播放地址。
 * 脚本可调用 <code>fetchText(url)</code> 拉取远程内容。
 * 这是"空壳定位"下延续 catvod/TVBox 风格、但自成一体的轻量 JS 蜘蛛桥。
 */
public class JsSpider {

    /** 用户设置的 JS 蜘蛛脚本源码（或可改为从 URL 拉取）。 */
    public static String JS_SOURCE = "";

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();

    /** 执行 JS 蜘蛛的 spider(json) 并返回 JSON 结果字符串。 */
    public String run(String jsSource, String json) {
        Context cx = Context.enter();
        try {
            cx.setOptimizationLevel(-1);
            Scriptable scope = cx.initStandardObjects();
            ScriptableObject.putProperty(scope, "fetchText", new BaseFunction() {
                @Override
                public Object call(Context c, Scriptable scope, Scriptable thisObj, Object[] args) {
                    String url = Context.toString(args[0]);
                    try (Response r = client.newCall(new Request.Builder().url(url).build()).execute()) {
                        return r.body() == null ? "" : r.body().string();
                    } catch (IOException e) {
                        return "";
                    }
                }
            });
            cx.evaluateString(scope, jsSource, "spider.js", 1, null);
            Object fn = ScriptableObject.getProperty(scope, "spider");
            if (fn instanceof Function) {
                Object result = ((Function) fn).call(cx, scope, scope, new Object[]{json});
                return Context.toString(result);
            }
            return "";
        } finally {
            Context.exit();
        }
    }
}
