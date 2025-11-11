package com.mycompany.proyectopcypoto2025.graphics;

import javax.swing.*;
import java.awt.*;

public class ResourceGraphPanel extends JPanel {
    public ResourceGraphPanel() { setPreferredSize(new Dimension(300,350)); }
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawString("Grafo de recursos (placeholder).", 10,20);
        g.setColor(Color.BLUE); g.fillOval(20,40,40,40); g.setColor(Color.BLACK); g.drawString("R1",35,65);
        g.setColor(Color.MAGENTA); g.fillOval(80,40,40,40); g.setColor(Color.BLACK); g.drawString("R2",95,65);
    }
}
