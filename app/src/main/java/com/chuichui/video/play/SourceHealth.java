package com.chuichui.video.play;

import java.util.HashMap;
import java.util.Map;

/** 源健康度：记录失败次数用于候选降级排序（进程内；重启归零，MVP 可接受）。 */
public class SourceHealth {

    public static final SourceHealth GLOBAL = new SourceHealth();

    private final Map<String, Integer> failures = new HashMap<>();

    public synchronized void bumpFailure(String sourceId) {
        Integer n = failures.get(sourceId);
        failures.put(sourceId, n == null ? 1 : n + 1);
    }

    public synchronized int failures(String sourceId) {
        Integer n = failures.get(sourceId);
        return n == null ? 0 : n;
    }
}
