package com.chuichui.video.category;

import com.chuichui.video.bean.Category;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 分类白名单过滤 + 两级分组析构（纯函数，无 Android 依赖）。
 *
 * 匹配规则：typeName（忽略大小写）包含任一非空白名单词即放行；typeName 为空放行；
 * 词表为空 → 全隐藏；词表为 null → 全放行。
 *
 * 分组规则：
 * - 无 pid 结构的源（全部 typePid==0，如天涯）→ 平铺单组（title=null），逐分类过白名单；
 * - 有 pid 结构的源 → 叶子（typePid>0）按父级分组（组标题=父分类名）；
 *   叶子仅按自身 typeName 命中即放行；父级不参与叶子匹配；
 *   父亲不在列表的叶子、以及有 pid 结构中孤立的 pid==0 分类 → 归入若干 null 标题组放在最后（按 raw 顺序）。
 */
public final class CategoryFilter {

    private CategoryFilter() {
    }

    /** 内置默认白名单：一级兼容词（供平铺源的白名单匹配）+ 常见影视叶子类目。 */
    public static final List<String> DEFAULT_TERMS = Arrays.asList(
            "电影", "电视剧", "动漫", "综艺",
            "动作片", "喜剧片", "爱情片", "科幻片", "奇幻片", "冒险片",
            "恐怖片", "惊悚片", "悬疑片", "犯罪片", "剧情片", "战争片",
            "纪录片", "动画片", "武侠片", "灾难片",
            "大陆剧", "国产剧", "港剧", "台剧", "韩剧", "日剧", "美剧",
            "英剧", "泰剧", "海外剧", "日漫", "国漫", "少儿");

    public static boolean isAllowed(String typeName, List<String> terms) {
        if (typeName == null) return true;
        if (terms == null) return true;
        if (typeName.isEmpty()) return true;
        if (terms.isEmpty()) return false;
        String t = typeName.toLowerCase(Locale.ROOT).trim();
        for (String term : terms) {
            if (term == null) continue;
            String tr = term.trim().toLowerCase(Locale.ROOT);
            if (!tr.isEmpty() && t.contains(tr)) return true;
        }
        return false;
    }

    /** 分类 → 渲染分组（顺序稳定、空组已剔除、孤立归尾部）。空词表返回空列表。 */
    public static List<CategoryGroup> buildGroups(List<Category> raw, List<String> allowTerms) {
        if (raw == null || raw.isEmpty()) return new ArrayList<>();

        boolean tree = false;
        for (Category c : raw) {
            if (c.typePid > 0) {
                tree = true;
                break;
            }
        }

        if (!tree) {
            List<Category> ok = new ArrayList<>();
            for (Category c : raw) if (isAllowed(c.typeName, allowTerms)) ok.add(c);
            if (ok.isEmpty()) return new ArrayList<>();
            List<CategoryGroup> flat = new ArrayList<>();
            flat.add(new CategoryGroup(null, ok));
            return flat;
        }

        Map<String, Category> byId = new HashMap<>();
        for (Category c : raw) byId.put(c.typeId, c);
        Set<String> childPids = new HashSet<>();
        for (Category c : raw) if (c.typePid > 0) childPids.add(String.valueOf(c.typePid));

        Map<String, CategoryGroup> byGroup = new LinkedHashMap<>(); // key = 父 typeId
        List<Category> isolated = new ArrayList<>();
        for (Category c : raw) {
            if (c.typePid > 0) {
                Category parent = byId.get(String.valueOf(c.typePid));
                boolean allow = isAllowed(c.typeName, allowTerms);
                if (!allow) continue;
                if (parent == null) {
                    isolated.add(c);
                    continue;
                }
                CategoryGroup g = byGroup.get(parent.typeId);
                if (g == null) {
                    g = new CategoryGroup(parent.typeName, new ArrayList<>());
                    byGroup.put(parent.typeId, g);
                }
                g.categories.add(c);
            } else {
                if (!childPids.contains(c.typeId) && isAllowed(c.typeName, allowTerms)) isolated.add(c);
            }
        }

        List<CategoryGroup> out = new ArrayList<>();
        for (CategoryGroup g : byGroup.values()) if (!g.categories.isEmpty()) out.add(g);
        if (!isolated.isEmpty()) out.add(new CategoryGroup(null, isolated));
        return out;
    }
}
