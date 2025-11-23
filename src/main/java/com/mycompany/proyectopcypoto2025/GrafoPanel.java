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

        // Inicializar con el problema por defecto
        grafo.configurarProblema("filosofos");
    }

    public void mostrarProblema(String tipo) {
        // Mapear nombres de menú a identificadores internos
        String tipoInterno = switch (tipo) {
            case "Clúster GPU" -> "gpu_cluster";
            case "Cena de Filósofos" -> "filosofos";
            case "Lectores-Escritores" -> "lectores-escritores";
            case "Productor-Consumidor" -> "productor-consumidor";
            case "Barbero Dormilón" -> "barbero dormilón";
            case "Fumadores" -> "fumadores";
            default -> tipo.toLowerCase().replaceAll("[ -]", "");
        };
        grafo.configurarProblema(tipoInterno);
    }

    public void deadlockEjecutar() {
        grafo.configurarProblema("deadlock");
    }

    public void deadlockEvitar() {
        grafo.configurarProblema("productor-consumidor");
    }

    public DibujaGrafo getGrafo() {
        return grafo;
    }
}