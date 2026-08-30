package com.chuichui.video.bean;

import com.google.gson.annotations.SerializedName;

import java.util.UUID;

/** 源（Source）：一个内容提供方（苹果 CMS 采集 API 端点或 JS 蜘蛛脚本），由 type 区分协议。 */
public class Source {

    public enum Type {
        /** 苹果 CMS 采集 API。 */
        @SerializedName("maccms") MACCMS,
        /** JS 蜘蛛脚本：api 字段存脚本内容，或指向脚本的 http(s) 地址（适配器会拉取）。 */
        @SerializedName("js") JS_SPIDER
    }

    /** 稳定 id：选中等持久化用它（删源/重排不漂移）。 */
    public String id = UUID.randomUUID().toString();
    public String name;
    public String api;
    public Type type = Type.MACCMS;

    public Source() {
    }

    public Source(String name, String api) {
        this(name, api, Type.MACCMS);
    }

    public Source(String name, String api, Type type) {
        this.name = name;
        this.api = api;
        this.type = type;
    }

    @Override
    public String toString() {
        return name + "  ↔  " + api;
    }
}
