package com.chuichui.video.bean;

/** 分类（Category）：源提供的片源目录分组（苹果 CMS class[] 的 type_id/type_name），浏览入口。 */
public class Category {
    public String typeId;
    public String typeName;

    public Category() {
    }

    public Category(String typeId, String typeName) {
        this.typeId = typeId;
        this.typeName = typeName;
    }

    @Override
    public String toString() {
        return typeName;
    }
}
