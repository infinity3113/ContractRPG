package com.example.contractrpg.util;

public class ProgressBar {

    public static String create(int current, int max) {
        if (max == 0) return "";
        float percent = (float) current / max;
        int progressBars = (int) (10 * percent);

        StringBuilder bar = new StringBuilder();
        bar.append("<#32CD32>");
        for (int i = 0; i < progressBars; i++) {
            bar.append("▌");
        }
        bar.append("<#808080>");
        for (int i = 0; i < 10 - progressBars; i++) {
            bar.append("▌");
        }
        bar.append(" <white>(").append(current).append("/").append(max).append(")");
        return bar.toString();
    }
}