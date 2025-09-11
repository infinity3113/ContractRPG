package com.example.contractrpg.util;

public class ProgressBar {

    /**
     * Crea una barra de progreso textual.
     * @param current El valor actual.
     * @param max El valor máximo.
     * @param totalBars El número total de segmentos en la barra.
     * @param symbol El caracter para los segmentos de progreso.
     * @param completedColor El color para la parte completada.
     * @param notCompletedColor El color para la parte no completada.
     * @return La barra de progreso como un String formateado.
     */
    public static String create(int current, int max, int totalBars, String symbol, String completedColor, String notCompletedColor) {
        if (current > max) {
            current = max;
        }

        float percent = (float) current / max;
        int progressBars = (int) (totalBars * percent);

        StringBuilder sb = new StringBuilder();
        sb.append(completedColor);
        for (int i = 0; i < progressBars; i++) {
            sb.append(symbol);
        }

        sb.append(notCompletedColor);
        for (int i = 0; i < totalBars - progressBars; i++) {
            sb.append(symbol);
        }

        return sb.toString();
    }
}