package com.chuichui.video.api;

import com.chuichui.video.bean.Source;

/** 按源 type 创建对应的适配器（UI 与聚合层不感知具体协议）。 */
public final class SourceFactory {

    private SourceFactory() {
    }

    public static SourceAdapter create(Source source) {
        Source.Type type = source.type == null ? Source.Type.MACCMS : source.type;
        switch (type) {
            case JS_SPIDER:
                return new JsSpiderAdapter(source.api);
            case MACCMS:
            default:
                return new MaccmsAdapter(source.api);
        }
    }
}
