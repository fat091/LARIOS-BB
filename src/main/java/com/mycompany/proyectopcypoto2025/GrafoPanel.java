package com.mycompany.proyectopcypoto2025;

import javax.swing.*;
import java.awt.*;

public class GrafoPanel extends JPanel {

    private final DibujaGrafo grafo;

    public GrafoPanel() {
        setLayout(new BorderLayout());
        grafo = new DibujaGrafo();
        grafo.setDoubleBuffered(true);

        // Si tu UI muestra el grafo dentro de un JScrollPane (como en tu captura):
        JScrollPane sp = new JScrollPane(grafo,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        add(sp, BorderLayout.CENTER);

        // 👇 Muestra algo por defecto al abrir la app (cámbialo si quieres)
        grafo.configurarProblema("filosofos");
    }

    public void mostrarProblema(String tipo) {
        grafo.configurarProblema(tipo);
    }
}
