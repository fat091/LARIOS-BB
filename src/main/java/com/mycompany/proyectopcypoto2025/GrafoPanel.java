package com.mycompany.proyectopcypoto2025;

import javax.swing.*;
import java.awt.*;

public class GrafoPanel extends JPanel {
    private final DibujaGrafo grafo;

    public GrafoPanel() {
        setLayout(new BorderLayout());
        grafo = new DibujaGrafo();
        JScrollPane sp = new JScrollPane(grafo,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        add(sp, BorderLayout.CENTER);

        grafo.configurarProblema("filosofos");
    }

    public void mostrarProblema(String tipo) {
        grafo.configurarProblema(tipo);
    }


    // --- Wrappers para compatibilidad con código antiguo ---
    public void deadlockEjecutar() { grafo.configurarProblema("deadlock"); }
    public void deadlockEvitar()   { grafo.configurarProblema("productor"); }

}
