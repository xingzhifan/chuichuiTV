package com.chuichui.video.bean;

import java.util.ArrayList;
import java.util.List;

/** 线路（Line）：一个源下的若干集。 */
public class Line {
    public String name;
    public List<Episode> episodes = new ArrayList<>();
}
