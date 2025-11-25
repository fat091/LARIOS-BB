package com.mycompany.proyectopcypoto2025;

import javax.swing.*;

/**
 * Main simple para probar MetricasGpuPanel sin toda la app completa.
 */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Métricas GPU + MPJ (Demo)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            MetricasGpuPanel panel = new MetricasGpuPanel();
            frame.add(panel);

            frame.setSize(900, 600);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // Arranca la demo de puntos aleatorios
            panel.demo();
        });
    }
}
