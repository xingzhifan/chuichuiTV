package com.chuichui.video.bean;

/** 源（Source）：苹果 CMS 分类。class[] 中的一项。 */
public class Site {
    public String typeId;
    public String typeName;

    public Site() {
    }

    public Site(String typeId, String typeName) {
        this.typeId = typeId;
        this.typeName = typeName;
    }

    @Override
    public String toString() {
        return typeName;
    }
}
