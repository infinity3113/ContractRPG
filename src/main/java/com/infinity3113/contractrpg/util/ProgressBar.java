package com.infinity3113.contractrpg.util;

import org.bukkit.ChatColor;

public class ProgressBar {

    public static String create(double current, double max, int totalBars, String barChar, String completedColor, String notCompletedColor) {
        if (max <= 0) max = 1; // Evitar división por cero y valores negativos
        
        // Asegurarse de que el progreso no supere el máximo
        current = Math.min(current, max);

        float percentFraction = (float) (current / max);
        int progressBars = (int) (totalBars * percentFraction);
        int percent = (int) (percentFraction * 100);

        // Se usa el traductor de colores de Bukkit ya que es más simple para la barra
        StringBuilder bar = new StringBuilder();
        bar.append(ChatColor.translateAlternateColorCodes('&', completedColor));
        for (int i = 0; i < progressBars; i++) {
            bar.append(barChar);
        }
        
        bar.append(ChatColor.translateAlternateColorCodes('&', notCompletedColor));
        for (int i = 0; i < totalBars - progressBars; i++) {
            bar.append(barChar);
        }
        
        // Añadir el porcentaje al final
        bar.append(" ").append(ChatColor.translateAlternateColorCodes('&', completedColor)).append(percent).append("%");

        return bar.toString();
    }
}
