package com.chuichui.video.play;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 源健康度：失败计数用于候选降级排序（进程内；起播成功即清零恢复）。 */
public class SourceHealth {

    public static final SourceHealth GLOBAL = new SourceHealth();

    private final Map<String, Integer> failures = new HashMap<>();

    public synchronized void bumpFailure(String sourceId) {
        Integer n = failures.get(sourceId);
        failures.put(sourceId, n == null ? 1 : n + 1);
    }

    /** 起播成功：清零该源失败计数（源恢复健康）。 */
    public synchronized void markSuccess(String sourceId) {
        failures.remove(sourceId);
    }

    public synchronized int failures(String sourceId) {
        Integer n = failures.get(sourceId);
        return n == null ? 0 : n;
    }

    /** 按 health 升序稳定排序 id（失败少的在前）。供聚合层给"其他源"排序。 */
    public synchronized List<String> rankIds(List<String> ids) {
        List<String> out = new ArrayList<>(ids);
        out.sort(Comparator.comparingInt(this::failures));
        return out;
    }
}
