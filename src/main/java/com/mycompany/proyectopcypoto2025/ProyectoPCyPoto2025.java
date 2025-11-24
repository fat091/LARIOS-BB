package com.mycompany.proyectopcypoto2025;

import javax.swing.*;
import java.awt.*;

public class ProyectoPCyPoto2025 extends JFrame {
    private final PanelProblemas izq = new PanelProblemas();
    private final GrafoPanel derG = new GrafoPanel();
    private final MetricasGpuPanel derM = new MetricasGpuPanel();
    private final MetricasGpuCollector metricasCollector = new MetricasGpuCollector();
    
    private SimuladorCincoCore simulador;
    private JCheckBoxMenuItem simuladorMenuItem;

    public ProyectoPCyPoto2025() {
        super("Proyecto PCyP Otoño 2025 - Animaciones + Métricas GPU + 5 Cores");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1280, 800));
        setLocationByPlatform(true);
        setJMenuBar(menu());

        derM.setMetricasCollector(metricasCollector);

        JSplitPane right = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                wrap("Grafo de Recursos", derG),
                wrap("Métricas GPU - 5 Cores en Paralelo", derM));
        right.setResizeWeight(0.6);

        JSplitPane root = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                wrap("Animaciones de Problemas", izq),
                right);
        root.setResizeWeight(0.5);

        getContentPane().add(root, BorderLayout.CENTER);
        derM.demo();
        
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                detenerSimulador();
            }
        });
    }

    private JMenuBar menu() {
        JMenuBar mb = new JMenuBar();

        JMenu archivo = new JMenu("Archivo");
        JMenuItem salir = new JMenuItem("Salir");
        salir.addActionListener(e -> {
            detenerSimulador();
            System.exit(0);
        });
        archivo.add(salir);
        mb.add(archivo);

        // Menú Simulador 5 Cores
        JMenu coresMenu = new JMenu("5 Cores");
        simuladorMenuItem = new JCheckBoxMenuItem("🚀 Activar 5 Cores");
        simuladorMenuItem.addActionListener(e -> {
            if (simuladorMenuItem.isSelected()) {
                iniciarSimulador();
            } else {
                detenerSimulador();
            }
        });
        
        JMenuItem infoCores = new JMenuItem("ℹ Info 5 Cores");
        infoCores.addActionListener(e -> mostrarInfoCores());
        
        coresMenu.add(simuladorMenuItem);
        coresMenu.addSeparator();
        coresMenu.add(infoCores);
        mb.add(coresMenu);

        JMenu synch = new JMenu("Sincronización");
        synch.add(crearItemSync("Semáforos", SyncMode.SEMAFOROS));
        synch.add(crearItemSync("Variables de condición", SyncMode.VAR_CONDICION));
        synch.add(crearItemSync("Monitores", SyncMode.MONITORES));
        synch.add(crearItemSync("Mutex", SyncMode.MUTEX));
        synch.add(crearItemSync("Barreras", SyncMode.BARRERAS));
        mb.add(synch);

        JMenu graf = new JMenu("Gráfica");
        JMenuItem scroll = new JMenuItem("Scroll");
        scroll.addActionListener(e -> derM.setMode(MetricasGpuPanel.Mode.SCROLL));
        JMenuItem carr = new JMenuItem("Carrusel");
        carr.addActionListener(e -> derM.setMode(MetricasGpuPanel.Mode.CARRUSEL));
        JMenuItem acor = new JMenuItem("Acordeón");
        acor.addActionListener(e -> derM.setMode(MetricasGpuPanel.Mode.ACORDEON));
        JMenuItem reset = new JMenuItem("Reset Métricas");
        reset.addActionListener(e -> {
            derM.reset();
            metricasCollector.reset();
        });
        graf.add(scroll);
        graf.add(carr);
        graf.add(acor);
        graf.addSeparator();
        graf.add(reset);
        mb.add(graf);

        JMenu prob = new JMenu("Problemas");
        prob.add(crearItemProblema("Productor-Consumidor"));
        prob.add(crearItemProblema("Cena de Filósofos"));
        prob.add(crearItemProblema("Barbero Dormilón"));
        prob.add(crearItemProblema("Fumadores"));
        prob.add(crearItemProblema("Lectores-Escritores"));
        prob.add(crearItemProblema("Clúster GPU"));
        mb.add(prob);

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
    
    private void iniciarSimulador() {
        if (simulador != null && simulador.estaEjecutando()) {
            return;
        }
        
        simulador = new SimuladorCincoCore(metricasCollector);
        simulador.iniciar();
        
        JOptionPane.showMessageDialog(this,
            "✅ 5 Cores ejecutándose en paralelo\n\n" +
            "🔵 Core 0: Semáforos\n" +
            "🔴 Core 1: Variables de Condición\n" +
            "🟢 Core 2: Monitores\n" +
            "🟠 Core 3: Mutex\n" +
            "🟣 Core 4: Barreras\n\n" +
            "Las gráficas muestran datos REALES de cada core.",
            "5 Cores Activos", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void detenerSimulador() {
        if (simulador != null) {
            simulador.detener();
            simulador = null;
        }
    }
    
    private void mostrarInfoCores() {
        String info = """
            <html>
            <h2>Simulador de 5 Cores en Paralelo</h2>
            <p><b>¿Qué hace?</b></p>
            <ul>
                <li>Ejecuta 5 threads en paralelo (simulando 5 cores)</li>
                <li><span style='color:blue'>Core 0</span>: Semáforos</li>
                <li><span style='color:red'>Core 1</span>: Variables de Condición</li>
                <li><span style='color:green'>Core 2</span>: Monitores</li>
                <li><span style='color:orange'>Core 3</span>: Mutex</li>
                <li><span style='color:purple'>Core 4</span>: Barreras</li>
            </ul>
            <p><b>Funcionamiento:</b></p>
            <ul>
                <li>Cada core ejecuta 200 operaciones</li>
                <li>Se miden operaciones exitosas vs conflictos</li>
                <li>Las gráficas se actualizan cada 10 operaciones</li>
                <li>Los datos son REALES de cada mecanismo de sincronización</li>
            </ul>
            <p><b>Interpretación:</b></p>
            <ul>
                <li>Líneas altas = Mejor eficiencia</li>
                <li>Líneas bajas = Más conflictos</li>
                <li>Monitores generalmente tienen mejor rendimiento</li>
            </ul>
            </html>
            """;
        
        JOptionPane.showMessageDialog(this, info, "Info 5 Cores", JOptionPane.INFORMATION_MESSAGE);
    }

    private JMenuItem crearItemSync(String nombre, SyncMode modo) {
        JMenuItem item = new JMenuItem(nombre);
        item.addActionListener(e -> {
            izq.setSyncMode(modo);
            metricasCollector.setModoActual(modo);
        });
        return item;
    }

    private JMenuItem crearItemProblema(String nombre) {
        JMenuItem item = new JMenuItem(nombre);
        item.addActionListener(e -> {
            izq.mostrar(nombre);
            derG.mostrarProblema(nombre);
            
            if ("Clúster GPU".equals(nombre)) {
                for (Component c : izq.getComponents()) {
                    if (c instanceof GpuClusterPanel) {
                        GpuClusterPanel gpuPanel = (GpuClusterPanel) c;
                        gpuPanel.setMetricasCollector(metricasCollector);
                        break;
                    }
                }
            }
            
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