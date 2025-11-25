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
        super("Proyecto PCyP Otoño 2025 - Animaciones + Métricas GPU + 5 Cores MPJ");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1280, 800));
        setLocationByPlatform(true);
        setJMenuBar(menu());

        derM.setMetricasCollector(metricasCollector);

        JSplitPane right = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                wrap("Grafo de Recursos", derG),
                wrap("Métricas GPU - 5 Cores MPJ en Paralelo", derM));
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
        JMenu coresMenu = new JMenu("5 Cores MPJ");
        simuladorMenuItem = new JCheckBoxMenuItem("🚀 Activar 5 Cores Locales");
        simuladorMenuItem.addActionListener(e -> {
            if (simuladorMenuItem.isSelected()) {
                iniciarSimulador();
            } else {
                detenerSimulador();
            }
        });
        
        JMenuItem mpjItem = new JMenuItem("🚀 Ejecutar MPJ Express (5 Cores)");
        mpjItem.addActionListener(e -> ejecutarMPJExpress());
        
        JMenuItem infoCores = new JMenuItem("ℹ Info 5 Cores");
        infoCores.addActionListener(e -> mostrarInfoCores());
        
        coresMenu.add(simuladorMenuItem);
        coresMenu.add(mpjItem);
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
        JMenuItem cargarMPJ = new JMenuItem("📊 Cargar Datos MPJ");
        cargarMPJ.addActionListener(e -> derM.cargarDatosDesdeMPJ());
        graf.add(scroll);
        graf.add(carr);
        graf.add(acor);
        graf.addSeparator();
        graf.add(reset);
        graf.add(cargarMPJ);
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
    
    private void ejecutarMPJExpress() {
        new Thread(() -> {
            try {
                JOptionPane.showMessageDialog(this,
                    "🚀 Iniciando MPJ Express con 5 cores...\n\n" +
                    "🔵 Core 0: Semáforos\n" +
                    "🔴 Core 1: Variables de Condición\n" +
                    "🟢 Core 2: Monitores\n" +
                    "🟠 Core 3: Mutex\n" +
                    "🟣 Core 4: Barreras\n\n" +
                    "Los datos se guardarán en mpj_metrics.csv",
                    "Ejecutando MPJ Express", JOptionPane.INFORMATION_MESSAGE);
                    
                String[] args = new String[0];
                SyncMetricsMPJ.main(args);
                
                // Cuando termina MPJ, cargar los datos en el panel
                SwingUtilities.invokeLater(() -> {
                    derM.cargarDatosDesdeMPJ();
                    JOptionPane.showMessageDialog(this,
                        "✅ Ejecución MPJ Express completada\n" +
                        "📊 Datos cargados en las gráficas\n\n" +
                        "Archivos generados:\n" +
                        "• mpj_tiempos.csv\n" +
                        "• mpj_operaciones.csv",
                        "MPJ Finalizado", JOptionPane.INFORMATION_MESSAGE);
                });
                
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this,
                        "❌ Error en MPJ Express: " + e.getMessage() + "\n\n" +
                        "Asegúrate de tener MPJ Express instalado y configurado.",
                        "Error MPJ", JOptionPane.ERROR_MESSAGE);
                });
                e.printStackTrace();
            }
        }).start();
    }
    
    private void iniciarSimulador() {
        if (simulador != null && simulador.estaEjecutando()) {
            return;
        }
        
        simulador = new SimuladorCincoCore(metricasCollector);
        simulador.iniciar();
        
        JOptionPane.showMessageDialog(this,
            "✅ 5 Cores locales ejecutándose en paralelo\n\n" +
            "🔵 Core 0: Semáforos\n" +
            "🔴 Core 1: Variables de Condición\n" +
            "🟢 Core 2: Monitores\n" +
            "🟠 Core 3: Mutex\n" +
            "🟣 Core 4: Barreras\n\n" +
            "Las gráficas muestran datos REALES de cada core.",
            "5 Cores Locales Activos", JOptionPane.INFORMATION_MESSAGE);
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
            
            <p><b>🎯 Dos Modos de Ejecución:</b></p>
            
            <p><b>1. MPJ Express (Distribuido):</b></p>
            <ul>
                <li>Ejecuta 5 procesos REALES en paralelo</li>
                <li>Usa MPJ Express para computación distribuida</li>
                <li>Genera archivos CSV con métricas detalladas</li>
                <li>Más preciso para análisis de rendimiento</li>
            </ul>
            
            <p><b>2. Cores Locales (Threads):</b></p>
            <ul>
                <li>Ejecuta 5 threads en paralelo en una sola JVM</li>
                <li>Más rápido para demostraciones</li>
                <li>Datos en tiempo real en la interfaz</li>
            </ul>
            
            <p><b>🔧 Configuración MPJ:</b></p>
            <ul>
                <li>Ejecutar con: <code>mpjrun -np 5 -cp target/classes SyncMetricsMPJ</code></li>
                <li>Requiere MPJ Express instalado</li>
            </ul>
            
            <p><b>📊 Métricas Colectadas:</b></p>
            <ul>
                <li>Tiempos de ejecución por operación</li>
                <li>Operaciones exitosas vs conflictos</li>
                <li>Eficiencia de cada mecanismo</li>
                <li>Comparación en tiempo real</li>
            </ul>
            </html>
            """;
        
        JOptionPane.showMessageDialog(this, info, "Info 5 Cores MPJ", JOptionPane.INFORMATION_MESSAGE);
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
        return p;SS
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