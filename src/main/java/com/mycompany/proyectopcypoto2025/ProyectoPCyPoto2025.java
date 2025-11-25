package com.mycompany.proyectopcypoto2025;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class ProyectoPCyPoto2025 extends JFrame {
    private final PanelProblemas izq = new PanelProblemas();
    private final GrafoPanel derG = new GrafoPanel();
    private final MetricasGpuPanel derM = new MetricasGpuPanel();
    private final MetricasGpuCollector metricasCollector = new MetricasGpuCollector();

    public ProyectoPCyPoto2025() {
        super("Proyecto PCyP Otoño 2025 - GPU Cluster + MPJ Express (5 Cores)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1280, 800));
        setLocationByPlatform(true);
        setJMenuBar(menu());

        derM.setMetricasCollector(metricasCollector);

        JSplitPane right = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                wrap("📊 Grafo de Recursos", derG),
                wrap("📈 Métricas GPU - 5 Cores MPJ Distribuidos", derM));
        right.setResizeWeight(0.6);

        JSplitPane root = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                wrap("🎬 Animaciones de Problemas Clásicos", izq),
                right);
        root.setResizeWeight(0.5);

        getContentPane().add(root, BorderLayout.CENTER);
    }

    private JMenuBar menu() {
        JMenuBar mb = new JMenuBar();

        // ========== MENÚ ARCHIVO ==========
        JMenu archivo = new JMenu("Archivo");
        JMenuItem salir = new JMenuItem("Salir");
        salir.addActionListener(e -> System.exit(0));
        archivo.add(salir);
        mb.add(archivo);

        // ========== MENÚ MPJ EXPRESS ==========
        JMenu mpjMenu = new JMenu("🚀 MPJ Express");
        
        JMenuItem ejecutarMPJ = new JMenuItem("▶️ Ejecutar 5 Cores (Genera Datos)");
        ejecutarMPJ.addActionListener(e -> ejecutarMPJExpress());
        
        JMenuItem cargarDatos = new JMenuItem("📊 Cargar Datos Generados");
        cargarDatos.addActionListener(e -> derM.cargarDatosDesdeMPJ());
        
        JMenuItem verArchivos = new JMenuItem("📁 Ver Archivos CSV");
        verArchivos.addActionListener(e -> mostrarArchivosCSV());
        
        JMenuItem infoMPJ = new JMenuItem("ℹ️ Info MPJ Express");
        infoMPJ.addActionListener(e -> mostrarInfoMPJ());
        
        mpjMenu.add(ejecutarMPJ);
        mpjMenu.add(cargarDatos);
        mpjMenu.addSeparator();
        mpjMenu.add(verArchivos);
        mpjMenu.add(infoMPJ);
        mb.add(mpjMenu);

        // ========== MENÚ SINCRONIZACIÓN ==========
        JMenu synch = new JMenu("🔒 Sincronización");
        synch.add(crearItemSync("Semáforos", SyncMode.SEMAFOROS));
        synch.add(crearItemSync("Variables de Condición", SyncMode.VAR_CONDICION));
        synch.add(crearItemSync("Monitores", SyncMode.MONITORES));
        synch.add(crearItemSync("Mutex", SyncMode.MUTEX));
        synch.add(crearItemSync("Barreras", SyncMode.BARRERAS));
        mb.add(synch);

        // ========== MENÚ GRÁFICA ==========
        JMenu graf = new JMenu("📈 Gráfica");
        JMenuItem scroll = new JMenuItem("Scroll (Desplazamiento)");
        scroll.addActionListener(e -> derM.setMode(MetricasGpuPanel.Mode.SCROLL));
        
        JMenuItem carr = new JMenuItem("Carrusel (Rotación Automática)");
        carr.addActionListener(e -> derM.setMode(MetricasGpuPanel.Mode.CARRUSEL));
        
        JMenuItem acor = new JMenuItem("Acordeón (Todas Visibles)");
        acor.addActionListener(e -> derM.setMode(MetricasGpuPanel.Mode.ACORDEON));
        
        JMenuItem reset = new JMenuItem("🔄 Reset Métricas");
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

        // ========== MENÚ PROBLEMAS ==========
        JMenu prob = new JMenu("🎭 Problemas");
        prob.add(crearItemProblema("Productor-Consumidor"));
        prob.add(crearItemProblema("Cena de Filósofos"));
        prob.add(crearItemProblema("Barbero Dormilón"));
        prob.add(crearItemProblema("Fumadores"));
        prob.add(crearItemProblema("Lectores-Escritores"));
        prob.add(crearItemProblema("Clúster GPU"));
        mb.add(prob);

        // ========== MENÚ DEADLOCK ==========
        JMenu dead = new JMenu("⚠️ Deadlock");
        JMenuItem ejecutar = new JMenuItem("Ejecutar (Provocar)");
        ejecutar.addActionListener(e -> {
            derG.deadlockEjecutar();
            if ("Cena de Filósofos".equals(izq.getCurrentKey())) {
                activarDeadlockFilosofos(true);
            }
        });
        
        JMenuItem evitar = new JMenuItem("Evitar (Resolver)");
        evitar.addActionListener(e -> {
            derG.deadlockEvitar();
            if ("Cena de Filósofos".equals(izq.getCurrentKey())) {
                activarDeadlockFilosofos(false);
            }
        });
        
        dead.add(ejecutar);
        dead.add(evitar);
        mb.add(dead);

        return mb;
    }

    // ========== EJECUTAR MPJ EXPRESS ==========
    
    private void ejecutarMPJExpress() {
        // Crear diálogo de progreso
        JDialog progressDialog = new JDialog(this, "Ejecutando MPJ Express", true);
        progressDialog.setLayout(new BorderLayout(10, 10));
        progressDialog.setSize(500, 200);
        progressDialog.setLocationRelativeTo(this);
        
        JTextArea logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        JScrollPane scrollPane = new JScrollPane(logArea);
        
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(new JLabel("🚀 Ejecutando 5 cores MPJ en paralelo..."), BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(progressBar, BorderLayout.SOUTH);
        
        progressDialog.add(panel);
        
        // Ejecutar en thread separado
        new Thread(() -> {
            try {
                SwingUtilities.invokeLater(() -> {
                    logArea.append("═══════════════════════════════════════════\n");
                    logArea.append("  EJECUTANDO MPJ EXPRESS - 5 CORES\n");
                    logArea.append("═══════════════════════════════════════════\n\n");
                    logArea.append("Core 0: SEMÁFOROS 🔵\n");
                    logArea.append("Core 1: VARIABLES DE CONDICIÓN 🔴\n");
                    logArea.append("Core 2: MONITORES 🟢\n");
                    logArea.append("Core 3: MUTEX 🟠\n");
                    logArea.append("Core 4: BARRERAS 🟣\n\n");
                    logArea.append("Iniciando simulación...\n\n");
                });
                
                // Ejecutar SyncMetricsMPJ
                String[] args = new String[0];
                SyncMetricsMPJ.main(args);
                
                SwingUtilities.invokeLater(() -> {
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(100);
                    logArea.append("\n✅ EJECUCIÓN COMPLETADA\n\n");
                    logArea.append("Archivos generados:\n");
                    logArea.append("  • mpj_tiempos.csv\n");
                    logArea.append("  • mpj_operaciones.csv\n\n");
                    logArea.append("Cargando datos en gráficas...\n");
                    
                    // Cargar automáticamente los datos
                    derM.cargarDatosDesdeMPJ();
                    
                    JButton cerrarBtn = new JButton("Cerrar");
                    cerrarBtn.addActionListener(ev -> progressDialog.dispose());
                    panel.add(cerrarBtn, BorderLayout.SOUTH);
                    
                    logArea.append("\n✅ Datos cargados exitosamente!\n");
                });
                
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    progressBar.setIndeterminate(false);
                    logArea.append("\n❌ ERROR: " + ex.getMessage() + "\n\n");
                    logArea.append("Verifica:\n");
                    logArea.append("  1. MPJ Express está instalado\n");
                    logArea.append("  2. mpj.jar está en lib/\n");
                    logArea.append("  3. El proyecto está compilado\n");
                    
                    JButton cerrarBtn = new JButton("Cerrar");
                    cerrarBtn.addActionListener(ev -> progressDialog.dispose());
                    panel.add(cerrarBtn, BorderLayout.SOUTH);
                });
                ex.printStackTrace();
            }
        }).start();
        
        progressDialog.setVisible(true);
    }

    private void mostrarArchivosCSV() {
        File tiempos = new File(System.getProperty("user.dir"), "mpj_tiempos.csv");
        File operaciones = new File(System.getProperty("user.dir"), "mpj_operaciones.csv");
        
        StringBuilder info = new StringBuilder();
        info.append("<html><h2>📁 Archivos CSV Generados</h2>");
        
        if (tiempos.exists()) {
            info.append("<p>✅ <b>mpj_tiempos.csv</b></p>");
            info.append("<p>   Tamaño: ").append(tiempos.length() / 1024).append(" KB</p>");
            info.append("<p>   Ruta: ").append(tiempos.getAbsolutePath()).append("</p><br>");
        } else {
            info.append("<p>❌ <b>mpj_tiempos.csv</b> no encontrado</p><br>");
        }
        
        if (operaciones.exists()) {
            info.append("<p>✅ <b>mpj_operaciones.csv</b></p>");
            info.append("<p>   Tamaño: ").append(operaciones.length() / 1024).append(" KB</p>");
            info.append("<p>   Ruta: ").append(operaciones.getAbsolutePath()).append("</p>");
        } else {
            info.append("<p>❌ <b>mpj_operaciones.csv</b> no encontrado</p>");
        }
        
        if (!tiempos.exists() && !operaciones.exists()) {
            info.append("<br><p>⚠️ Ejecuta primero MPJ Express para generar los archivos.</p>");
        }
        
        info.append("</html>");
        
        JOptionPane.showMessageDialog(this, info.toString(), 
                "Archivos CSV", JOptionPane.INFORMATION_MESSAGE);
    }

    private void mostrarInfoMPJ() {
        String info = """
            <html>
            <h2>🚀 MPJ Express - 5 Cores Distribuidos</h2>
            
            <h3>📊 Qué hace cada Core:</h3>
            <ul>
                <li><b>Core 0 (SEMÁFOROS):</b> Simula acquire/release con contadores</li>
                <li><b>Core 1 (VAR. CONDICIÓN):</b> Simula wait/notify con condiciones</li>
                <li><b>Core 2 (MONITORES):</b> Simula entrada/salida sincronizada</li>
                <li><b>Core 3 (MUTEX):</b> Simula lock/unlock exclusivo</li>
                <li><b>Core 4 (BARRERAS):</b> Simula puntos de sincronización grupal</li>
            </ul>
            
            <h3>📈 Métricas Recopiladas:</h3>
            <ul>
                <li><b>Tiempo por iteración:</b> Latencia de cada operación</li>
                <li><b>Operaciones exitosas:</b> Sin conflictos</li>
                <li><b>Conflictos:</b> Operaciones bloqueadas/fallidas</li>
                <li><b>Eficiencia:</b> (Exitosas / Total) × 100</li>
            </ul>
            
            <h3>🔧 Archivos Generados:</h3>
            <ul>
                <li><b>mpj_tiempos.csv:</b> Tiempos por iteración y core</li>
                <li><b>mpj_operaciones.csv:</b> Operaciones y eficiencia detallada</li>
            </ul>
            
            <h3>⚙️ Configuración:</h3>
            <ul>
                <li>Iteraciones por core: 200</li>
                <li>Operaciones por iteración: 10</li>
                <li>Total de operaciones: 10,000 (5 cores × 200 × 10)</li>
            </ul>
            </html>
            """;
        
        JOptionPane.showMessageDialog(this, info, 
                "Info MPJ Express", JOptionPane.INFORMATION_MESSAGE);
    }

    // ========== UTILIDADES ==========

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

    private void activarDeadlockFilosofos(boolean activar) {
        for (Component c : izq.getComponents()) {
            if (c instanceof CenaFilosofosPanel) {
                ((CenaFilosofosPanel) c).setDeadlock(activar);
                break;
            }
        }
    }

    private static JComponent wrap(String title, JComponent inner) {
        JPanel p = new JPanel(new BorderLayout());
        JLabel lbl = new JLabel(title);
        lbl.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 13f));
        lbl.setOpaque(true);
        lbl.setBackground(new Color(240, 240, 245));
        p.add(lbl, BorderLayout.NORTH);
        p.add(inner, BorderLayout.CENTER);
        return p;
    }

    // ========== MAIN ==========

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            ProyectoPCyPoto2025 frame = new ProyectoPCyPoto2025();
            frame.setVisible(true);
            
            // Mensaje de bienvenida
            JOptionPane.showMessageDialog(frame,
                "🎓 Proyecto PCyP Otoño 2025\n\n" +
                "Para comenzar:\n" +
                "1. Ve a: 🚀 MPJ Express → ▶️ Ejecutar 5 Cores\n" +
                "2. Espera a que se generen los datos\n" +
                "3. Las gráficas se cargarán automáticamente\n\n" +
                "Explora los diferentes problemas clásicos de concurrencia!",
                "Bienvenido", JOptionPane.INFORMATION_MESSAGE);
        });
    }
}