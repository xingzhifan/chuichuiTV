package com.chuichui.video.bean;

/** 分类（Category）：源提供的片源目录分组（苹果 CMS class[] 的 type_id/type_name），浏览入口。 */
public class Category {
    public String typeId;
    public String typeName;
    /** 父分类 id（苹果 CMS class[].type_pid）；无 pid 的源（平铺）为 0。 */
    public long typePid;

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
