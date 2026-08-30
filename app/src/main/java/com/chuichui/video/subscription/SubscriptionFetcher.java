package com.chuichui.video.subscription;

import com.chuichui.video.api.Http;

import java.io.IOException;

/** 从订阅 URL 拉取订阅 JSON 文本（复用共享 {@link Http}，网络层与解析层分离）。 */
public final class SubscriptionFetcher {

    private SubscriptionFetcher() {
    }

    /** 下载订阅 URL 的内容文本；非 2xx 或网络错误抛 IOException（由调用方兜底）。 */
    public static String fetch(String url) throws IOException {
        return Http.text(url.trim());
    }
}
