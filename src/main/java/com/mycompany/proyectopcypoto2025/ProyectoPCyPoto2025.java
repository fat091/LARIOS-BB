package com.mycompany.proyectopcypoto2025;

import javax.swing.*;
import java.awt.*;

public class ProyectoPCyPoto2025 extends JFrame {
    private final PanelProblemas izq = new PanelProblemas();
    private final GrafoPanel derG = new GrafoPanel();
    private final MetricasPanelJFree derM = new MetricasPanelJFree();

    public ProyectoPCyPoto2025() {
        super("Proyecto PCyP Otoño 2025 - Animaciones");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1280, 800));
        setLocationByPlatform(true);
        setJMenuBar(menu());

        JSplitPane right = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                wrap("Grafo de Recursos", derG),
                wrap("Métricas (JFreeChart)", derM));
        right.setResizeWeight(0.6);

        JSplitPane root = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                wrap("Animaciones de Problemas", izq),
                right);
        root.setResizeWeight(0.5);

        getContentPane().add(root, BorderLayout.CENTER);
        derM.demo();
    }

    private JMenuBar menu() {
        JMenuBar mb = new JMenuBar();

        // Menú Archivo
        JMenu archivo = new JMenu("Archivo");
        JMenuItem salir = new JMenuItem("Salir");
        salir.addActionListener(e -> System.exit(0));
        archivo.add(salir);
        mb.add(archivo);

        // Menú Sincronización
        JMenu synch = new JMenu("Sincronización");
        synch.add(crearItemSync("Semáforos", SyncMode.SEMAFOROS));
        synch.add(crearItemSync("Variables de condición", SyncMode.VAR_CONDICION));
        synch.add(crearItemSync("Monitores", SyncMode.MONITORES));
        synch.add(crearItemSync("Mutex", SyncMode.MUTEX));
        synch.add(crearItemSync("Barreras", SyncMode.BARRERAS));
        mb.add(synch);

        // Menú Gráfica
        JMenu graf = new JMenu("Gráfica");
        JMenuItem scroll = new JMenuItem("Scroll");
        scroll.addActionListener(e -> derM.setMode(MetricasPanelJFree.Mode.SCROLL));
        JMenuItem carr = new JMenuItem("Carrusel");
        carr.addActionListener(e -> derM.setMode(MetricasPanelJFree.Mode.CARRUSEL));
        JMenuItem acor = new JMenuItem("Acordeón");
        acor.addActionListener(e -> derM.setMode(MetricasPanelJFree.Mode.ACORDEON));
        graf.add(scroll);
        graf.add(carr);
        graf.add(acor);
        mb.add(graf);

        // Menú Problemas
        JMenu prob = new JMenu("Problemas");
        prob.add(crearItemProblema("Productor-Consumidor"));
        prob.add(crearItemProblema("Cena de Filósofos"));
        prob.add(crearItemProblema("Barbero Dormilón"));
        prob.add(crearItemProblema("Fumadores"));
        prob.add(crearItemProblema("Lectores-Escritores"));
        prob.add(crearItemProblema("Clúster GPU"));
        mb.add(prob);

        // Menú Deadlock
        JMenu dead = new JMenu("Deadlock");
        JMenuItem eje = new JMenuItem("Ejecutar");
        eje.addActionListener(e -> {
            derG.deadlockEjecutar();
            if ("Cena de Filósofos".equals(izq.getCurrentKey())) {
                for (Component c : izq.getComponents()) {
                    if (c instanceof CenaFilosofosPanel) {
                        ((CenaFilosofosPanel) c).setDeadlock(true);
                        break;
                    }
                }
                derM.setPaused(true);
            }
        });
        JMenuItem ev = new JMenuItem("Evitar");
        ev.addActionListener(e -> {
            derG.deadlockEvitar();
            if ("Cena de Filósofos".equals(izq.getCurrentKey())) {
                for (Component c : izq.getComponents()) {
                    if (c instanceof CenaFilosofosPanel) {
                        ((CenaFilosofosPanel) c).setDeadlock(false);
                        break;
                    }
                }
                derM.setPaused(false);
            }
        });
        dead.add(eje);
        dead.add(ev);
        mb.add(dead);

        return mb;
    }

    private JMenuItem crearItemSync(String nombre, SyncMode modo) {
        JMenuItem item = new JMenuItem(nombre);
        item.addActionListener(e -> izq.setSyncMode(modo));
        return item;
    }

    private JMenuItem crearItemProblema(String nombre) {
        JMenuItem item = new JMenuItem(nombre);
        item.addActionListener(e -> {
            izq.mostrar(nombre);
            derG.mostrarProblema(nombre);
            revalidate();
            repaint();
        });
        return item;
    }

    private static JComponent wrap(String title, JComponent inner) {
        JPanel p = new JPanel(new BorderLayout());
        JLabel lbl = new JLabel(title);
        lbl.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
        p.add(lbl, BorderLayout.NORTH);
        p.add(inner, BorderLayout.CENTER);
        return p;
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            ProyectoPCyPoto2025 frame = new ProyectoPCyPoto2025();
            frame.setVisible(true);
        });
    }
}