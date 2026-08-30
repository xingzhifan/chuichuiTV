package com.chuichui.video.api;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/** 共享 HTTP 客户端（MaccmsAdapter / JsSpiderAdapter 复用，消除重复构造）。 */
public final class Http {

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();

    private Http() {
    }

    public static OkHttpClient client() {
        return CLIENT;
    }

    /** GET 一个 URL；非 2xx 抛 IOException（让空态兜底、错误可见），成功返回响应体文本。 */
    public static String text(String url) throws IOException {
        try (Response res = CLIENT.newCall(new Request.Builder().url(url).build()).execute()) {
            if (!res.isSuccessful()) throw new IOException("HTTP " + res.code() + " for " + url);
            return res.body() == null ? "" : res.body().string();
        }
    }
}
