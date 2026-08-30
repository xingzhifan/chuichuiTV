package com.chuichui.video.bean;

/** 源（Source）：一个内容提供方，即一个可配置的 Content 源（苹果 CMS 采集 API 端点）。 */
public class Source {
    public String name;
    public String api;

    public Source() {
    }

    public Source(String name, String api) {
        this.name = name;
        this.api = api;
    }

    @Override
    public String toString() {
        return name + "  ↔  " + api;
    }
}
