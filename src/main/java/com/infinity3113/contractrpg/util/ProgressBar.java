package com.infinity3113.contractrpg.util;

import org.bukkit.ChatColor;

public class ProgressBar {
    public static String create(double current, double max, int totalBars, String barChar, String completedColor, String notCompletedColor) {
        if (max == 0) max = 1; // Avoid division by zero
        float percent = (float) (current / max);
        int progressBars = (int) (totalBars * percent);

        return ChatColor.translateAlternateColorCodes('&',
                completedColor + new String(new char[progressBars]).replace('\0', barChar.charAt(0)) +
                notCompletedColor + new String(new char[totalBars - progressBars]).replace('\0', barChar.charAt(0))
        );
    }
}
