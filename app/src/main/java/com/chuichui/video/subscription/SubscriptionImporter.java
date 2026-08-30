package com.chuichui.video.subscription;

import com.chuichui.video.bean.Source;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 订阅批量导入器（纯逻辑，无 Android 依赖，可 JVM 测试）。
 * 输入：JSON 数组文本 {@code [{"name":"…","api":"…","type":"maccms|js"}]}（或订阅 URL 解析后的条目）；
 * 输出：合并（去重/覆盖/追加）后的源列表与导入统计，可直接交 {@code SourceRepo.save} 持久化。
 *
 * 去重键为 {@code api}（地址）。两种模式：
 * <ul>
 *   <li><b>追加（overwrite=false）</b>：保留现有源；导入/已有 api 相同者跳过（计数 skippedDupes）。</li>
 *   <li><b>覆盖（overwrite=true）</b>：以导入条目为最终列表，替换原有全部源。</li>
 * </ul>
 */
public final class SubscriptionImporter {

    private SubscriptionImporter() {
    }

    /** 待导入条目：name/api/type（type 已归一化为领域枚举）。 */
    public static final class Entry {
        public final String name;
        public final String api;
        public final Source.Type type;

        public Entry(String name, String api, Source.Type type) {
            this.name = name == null ? "" : name;
            this.api = api == null ? "" : api;
            this.type = type;
        }
    }

    /** 一次导入的结果：合并后的源列表 + 统计。 */
    public static final class Result {
        public final List<Source> sources;
        public final int imported;      // 真正落库（新增或覆盖保留）的条目数
        public final int skippedDupes;  // 因 api 重复被跳过的条目数
        public final int invalid;       // 因缺 api 或 JSON 格式错误被忽略的条目数
        public final String error;      // 非空表示整批失败（如 JSON 不可解析），此时 sources 为空

        Result(List<Source> sources, int imported, int skippedDupes, int invalid, String error) {
            this.sources = sources;
            this.imported = imported;
            this.skippedDupes = skippedDupes;
            this.invalid = invalid;
            this.error = error;
        }

        /** 构造一个失败结果（如网络拉取订阅失败），供调用方展示。 */
        public static Result failure(String error) {
            return new Result(new ArrayList<>(), 0, 0, 0, error);
        }
    }

    /**
     * 从 JSON 批量导入。
     *
     * @param json      JSON 数组文本（或 null）。
     * @param existing  当前已配置的源（追加时作为基座；覆盖时忽略）。
     * @param overwrite true=覆盖（以本批为最终列表）；false=追加（保留现有，跳过 api 重复）。
     */
    public static Result fromJson(String json, List<Source> existing, boolean overwrite) {
        List<Entry> entries = parse(json);
        if (entries == null) {
            return new Result(new ArrayList<>(), 0, 0, 0, "JSON 格式错误：应为 [{\"name\":…,\"api\":…,\"type\":\"maccms|js\"}]");
        }
        return fromEntries(entries, existing, overwrite);
    }

    /** 从已解析条目批量导入（供订阅 URL 下载解析后调用），语义同 {@link #fromJson}。 */
    public static Result fromEntries(List<Entry> entries, List<Source> existing, boolean overwrite) {
        List<Entry> list = entries == null ? new ArrayList<Entry>() : entries;
        List<Source> base = existing == null ? new ArrayList<Source>() : existing;
        Map<String, Source> present = new LinkedHashMap<>(); // 已占用 api → 其源（顺序稳定）

        // 追加：先放现有源；覆盖：不放入。
        if (!overwrite) {
            for (Source s : base) {
                if (s.api != null && !s.api.isEmpty()) present.put(s.api, s);
            }
        }

        List<Source> out = new ArrayList<>(present.values());
        int imported = 0, skippedDupes = 0, invalid = 0;
        for (Entry e : list) {
            if (e.api.isEmpty()) {
                invalid++;
                continue;
            }
            if (present.containsKey(e.api)) {
                skippedDupes++;
                continue;
            }
            Source src = new Source(e.name.isEmpty() ? e.api : e.name, e.api, e.type);
            present.put(e.api, src);
            out.add(src);
            imported++;
        }
        return new Result(out, imported, skippedDupes, invalid, null);
    }

    /** 解析 JSON 数组 → 条目列表；无法解析（非数组/语法错/null）返回 null。 */
    private static List<Entry> parse(String json) {
        if (json == null || json.trim().isEmpty()) return null;
        JsonElement root;
        try {
            root = JsonParser.parseString(json);
        } catch (JsonSyntaxException e) {
            return null;
        }
        if (root == null || !root.isJsonArray()) return null;
        List<Entry> out = new ArrayList<>();
        for (JsonElement e : root.getAsJsonArray()) {
            if (e == null || !e.isJsonObject()) continue;
            JsonObject o = e.getAsJsonObject();
            out.add(new Entry(str(o, "name"), str(o, "api"), parseType(str(o, "type"))));
        }
        return out;
    }

    private static Source.Type parseType(String s) {
        if (s == null) return Source.Type.MACCMS;
        String t = s.trim().toLowerCase(Locale.ROOT);
        switch (t) {
            case "js":
            case "spider":
            case "js_spider":
                return Source.Type.JS_SPIDER;
            case "maccms":
            default:
                return Source.Type.MACCMS;
        }
    }

    private static String str(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
    }
}
