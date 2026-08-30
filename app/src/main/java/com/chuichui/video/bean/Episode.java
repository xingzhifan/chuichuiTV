package com.chuichui.video.bean;

/** 集（Episode）：某线路下的一集。name=集名，url=播放地址。 */
public class Episode {
    public String name;
    public String url;

    public Episode() {
    }

    public Episode(String name, String url) {
        this.name = name;
        this.url = url;
    }

    @Override
    public String toString() {
        return name;
    }
}
