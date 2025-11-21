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

        // Problema inicial
        grafo.configurarProblema("filosofos");
    }
    
    // Método modificado para manejar el mapeo de nombres de menú a nombres de grafo
    public void mostrarProblema(String tipo) {
        // Mapear el nombre del menú al nombre interno/limpio del grafo
        String tipoInterno = switch (tipo) {
            case "Clúster GPU" -> "gpu_cluster";
            case "Cena de Filósofos" -> "filosofos";
            case "Lectores-Escritores" -> "lectores";
            case "Productor-Consumidor" -> "productor";
            default -> tipo.toLowerCase().replaceAll("[ -]", "");
        };
        grafo.configurarProblema(tipoInterno);
    }


    // --- Wrappers para compatibilidad con código antiguo ---
    public void deadlockEjecutar() { grafo.configurarProblema("deadlock"); }
    public void deadlockEvitar()   { grafo.configurarProblema("productor"); }
    
    // Getter añadido para que GpuClusterPanel pueda actualizar el grafo dinámicamente
    public DibujaGrafo getGrafo() { return grafo; }

}