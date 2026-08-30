package com.chuichui.video.play;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 候选全局排序：
 * 1) 当前源中，与目标集名相同的候选（当前线路已由调用方排最前）；
 * 2) 其他源的同类候选，外层顺序 = 健康度升序（失败多的靠后）；
 * 3) 仅当所有源都没有同名集时 → 兜底：当前源全部候选 + 每个其他源的第一个候选（宁错换不空放）；
 * 最后按 url 去重。
 */
public final class PlayCandidates {

    private PlayCandidates() {
    }

    public static List<PlayCandidate> order(
            List<PlayCandidate> currentSource,
            List<List<PlayCandidate>> others,
            String episodeName) {
        List<PlayCandidate> currentMatches = matching(currentSource, episodeName);

        List<PlayCandidate> otherMatches = new ArrayList<>();
        List<PlayCandidate> fallback = new ArrayList<>();
        for (List<PlayCandidate> othersCandidates : others) {
            List<PlayCandidate> matches = matching(othersCandidates, episodeName);
            if (!matches.isEmpty()) {
                otherMatches.addAll(matches);
            } else {
                fallback.addAll(firstOnly(othersCandidates));
            }
        }

        List<PlayCandidate> out = new ArrayList<>();
        if (!currentMatches.isEmpty() || !otherMatches.isEmpty()) {
            out.addAll(currentMatches);
            out.addAll(otherMatches);
        } else {
            out.addAll(currentSource);
            out.addAll(fallback);
        }
        return dedupeByUrl(out);
    }

    private static List<PlayCandidate> matching(List<PlayCandidate> in, String episodeName) {
        List<PlayCandidate> out = new ArrayList<>();
        for (PlayCandidate c : in) {
            if (c.episodeName.equals(episodeName)) out.add(c);
        }
        return out;
    }

    private static List<PlayCandidate> firstOnly(List<PlayCandidate> in) {
        return in.isEmpty() ? in : java.util.Collections.singletonList(in.get(0));
    }

    private static List<PlayCandidate> dedupeByUrl(List<PlayCandidate> in) {
        List<PlayCandidate> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (PlayCandidate c : in) {
            if (c.url != null && seen.add(c.url)) out.add(c);
        }
        return out;
    }
}
