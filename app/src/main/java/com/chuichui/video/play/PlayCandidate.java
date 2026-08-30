package com.chuichui.video.play;

/** 播放候选：某一个源、某条线路上的某一集的可播放地址。 */
public class PlayCandidate {
    public final String sourceId;
    public final String sourceName;
    public final String lineName;
    public final String episodeName;
    public final String url;

    public PlayCandidate(String sourceId, String sourceName, String lineName, String episodeName, String url) {
        this.sourceId = sourceId;
        this.sourceName = sourceName;
        this.lineName = lineName;
        this.episodeName = episodeName;
        this.url = url;
    }

    public String label() {
        return sourceName + " · " + lineName + " | " + episodeName;
    }
}
