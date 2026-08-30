package com.chuichui.video.play;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 候选全局排序（优先级从高到低）：
 * 1) 当前源中与所点集同名的候选（所点线路的所点集最先，其余线路的同名集随后）；
 * 2) 其他源中的同名（数字归一）候选，外层顺序 = 健康度升序（失败多的靠后）；
 * 3) 当前源中不同名的候选（最后手段：同一内容的不同集，宁错换不空放）；
 * 4) 无同名集的其他源的首个候选（最末）；
 * 最后按 url 去重。
 */
public final class PlayCandidates {

    private PlayCandidates() {
    }

    public static List<PlayCandidate> order(
            List<PlayCandidate> currentSource,
            List<List<PlayCandidate>> others,
            String episodeName) {
        List<PlayCandidate> matchesCurrent = matching(currentSource, episodeName);
        List<PlayCandidate> currentRest = new ArrayList<>(currentSource);
        currentRest.removeAll(matchesCurrent);

        List<PlayCandidate> otherMatches = new ArrayList<>();
        List<PlayCandidate> otherFirsts = new ArrayList<>();
        for (List<PlayCandidate> oc : others) {
            List<PlayCandidate> m = matching(oc, episodeName);
            if (!m.isEmpty()) otherMatches.addAll(m);
            else otherFirsts.addAll(firstOnly(oc));
        }

        List<PlayCandidate> out = new ArrayList<>();
        out.addAll(matchesCurrent);
        out.addAll(otherMatches);
        out.addAll(currentRest);
        out.addAll(otherFirsts);
        return dedupeByUrl(out);
    }

    private static List<PlayCandidate> matching(List<PlayCandidate> in, String episodeName) {
        List<PlayCandidate> out = new ArrayList<>();
        for (PlayCandidate c : in) {
            if (episodeMatches(c.episodeName, episodeName)) out.add(c);
        }
        return out;
    }

    /** 集名匹配：精确相等，或去除非数字后一致（"第01集" ≍ "01"）。 */
    static boolean episodeMatches(String candidate, String target) {
        if (candidate.equals(target)) return true;
        String a = candidate.replaceAll("\\D", "");
        String b = target.replaceAll("\\D", "");
        return !a.isEmpty() && a.equals(b);
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
