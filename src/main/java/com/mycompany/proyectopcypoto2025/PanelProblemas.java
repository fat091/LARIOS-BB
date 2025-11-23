package com.mycompany.proyectopcypoto2025;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class PanelProblemas extends JPanel {
    private final CardLayout cardLayout;
    private final Map<String, JComponent> paneles;
    private String currentKey;

    public PanelProblemas() {
        cardLayout = new CardLayout();
        setLayout(cardLayout);
        paneles = new HashMap<>();

        paneles.put("Productor-Consumidor", new ProductorConsumidorPanel());
        paneles.put("Cena de Filósofos", new CenaFilosofosPanel());
        paneles.put("Barbero Dormilón", new BarberoDormilonPanel());
        paneles.put("Fumadores", new FumadoresPanel());
        paneles.put("Lectores-Escritores", new LectoresEscritoresPanel());
        paneles.put("Clúster GPU", new GpuClusterPanel());

        for (Map.Entry<String, JComponent> entry : paneles.entrySet()) {
            add(entry.getValue(), entry.getKey());
        }

        mostrar("Productor-Consumidor");
    }

    public void mostrar(String key) {
        if (paneles.containsKey(key)) {
            cardLayout.show(this, key);
            currentKey = key;
        }
    }

    public void setSyncMode(SyncMode mode) {
        for (JComponent panel : paneles.values()) {
            if (panel instanceof SyncAware) {
                ((SyncAware) panel).setSyncMode(mode);
            }
        }
    }

    public String getCurrentKey() {
        return currentKey;
    }
}