package com.chuichui.video.play;

import java.util.ArrayList;
import java.util.List;

/** 播放候选队列：按优先级排序；当前候选失败可标记降级并推进到下一候选。 */
public class PlayQueue {

    private final List<PlayCandidate> candidates;
    private final SourceHealth health;
    private int index;

    public PlayQueue(List<PlayCandidate> candidates, SourceHealth health) {
        this.candidates = new ArrayList<>(candidates);
        this.health = health;
        this.index = 0;
    }

    /** 当前候选；队列耗尽返回 null。 */
    public PlayCandidate current() {
        return index < candidates.size() ? candidates.get(index) : null;
    }

    public int position() {
        return index;
    }

    public int size() {
        return candidates.size();
    }

    public PlayCandidate get(int i) {
        return candidates.get(i);
    }

    public boolean exhausted() {
        return index >= candidates.size();
    }

    /** 当前候选失败：记源健康降级并推进到下一候选（返回新的当前，可能为 null）。 */
    public PlayCandidate markFailedAndAdvance() {
        PlayCandidate cur = current();
        if (cur != null) health.bumpFailure(cur.sourceId);
        index++;
        return current();
    }
}
