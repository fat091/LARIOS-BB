package com.mycompany.proyectopcypoto2025.graphics;

import javax.swing.*;
import java.awt.*;

public class ChartPanelSimple extends JPanel {
    public ChartPanelSimple() { setPreferredSize(new Dimension(300,350)); }
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawString("Gráfica (scroll/carrusel/acordeón) - placeholder", 10,20);
    }
}
