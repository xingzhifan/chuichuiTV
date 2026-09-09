package com.chuichui.video.category;

import com.chuichui.video.bean.Category;

import java.util.List;

/** 首页分类的一个渲染分组：title 为空表示「无分组标题」（平铺源 / 孤立分类）。 */
public class CategoryGroup {
    public String title;
    public List<Category> categories;

    public CategoryGroup(String title, List<Category> categories) {
        this.title = title;
        this.categories = categories;
    }
}
