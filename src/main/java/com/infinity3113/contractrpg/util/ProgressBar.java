package com.infinity3113.contractrpg.util;

public class ProgressBar {

    /**
     * Crea una barra de progreso con formato MiniMessage.
     * @param current El valor actual del progreso.
     * @param max El valor máximo del progreso.
     * @param totalBars El número total de caracteres que tendrá la barra.
     * @param barChar El carácter a usar para la barra.
     * @param completedColor El color en formato MiniMessage para la parte completada (ej: "<green>", "<#32CD32>").
     * @param notCompletedColor El color en formato MiniMessage para la parte no completada (ej: "<gray>", "<#808080>").
     * @return Una cadena de texto formateada con MiniMessage representando la barra de progreso.
     */
    public static String create(double current, double max, int totalBars, String barChar, String completedColor, String notCompletedColor) {
        if (max <= 0) max = 1; // Evitar división por cero
        
        current = Math.min(current, max); // Asegurarse de que el progreso no supere el máximo

        float percentFraction = (float) (current / max);
        int progressBars = (int) (totalBars * percentFraction);

        StringBuilder bar = new StringBuilder();
        
        // Parte completada
        bar.append(completedColor);
        for (int i = 0; i < progressBars; i++) {
            bar.append(barChar);
        }
        
        // Parte no completada
        bar.append(notCompletedColor);
        for (int i = 0; i < totalBars - progressBars; i++) {
            bar.append(barChar);
        }
        
        return bar.toString();
    }
}