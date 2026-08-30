package com.chuichui.video.bean;

/** 片源（Vod）：list[] 中的一项。 */
public class Vod {
    public String vodId;
    public String vodName;
    public String vodPic;
    public String vodRemarks;
    public String typeName;
    public String vodYear;
    public String vodPlayFrom;
    public String vodPlayUrl;

    @Override
    public String toString() {
        return vodName;
    }
}
