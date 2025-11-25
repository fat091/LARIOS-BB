package com.mycompany.proyectopcypoto2025;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Map;

public class ProyectoPCyPoto2025 extends JFrame {

    private final PanelProblemas izq = new PanelProblemas();
    private final GrafoPanel derG = new GrafoPanel();
    private final MetricasGpuPanel derM = new MetricasGpuPanel();
    private final MetricasGpuCollector metricasCollector = new MetricasGpuCollector();

    private SimuladorCincoCore simulador;
    private JCheckBoxMenuItem simuladorMenuItem;

    public ProyectoPCyPoto2025() {
        super("Proyecto PCyP Otoño 2025 - Animaciones + Métricas GPU + 5 Cores + MPJ");
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

        // ====== Menú Archivo ======
        JMenu archivo = new JMenu("Archivo");
        JMenuItem salir = new JMenuItem("Salir");
        salir.addActionListener(e -> {
            detenerSimulador();
            System.exit(0);
        });
        archivo.add(salir);
        mb.add(archivo);

        // ====== Menú Simulador 5 Cores (local, sin MPJ) ======
        JMenu coresMenu = new JMenu("5 Cores");
        simuladorMenuItem = new JCheckBoxMenuItem("🚀 Activar 5 Cores (simulador local)");
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

        // ====== Menú MPJ (cores reales con SyncMetricsMPJ) ======
        JMenu mpjMenu = new JMenu("MPJ");
        JMenuItem ejecutarMPJ = new JMenuItem("Ejecutar MPJ (5 cores reales)");
        ejecutarMPJ.addActionListener(e -> ejecutarMPJ());

        JMenuItem cargarCSV = new JMenuItem("Cargar CSV MPJ en gráfica");
        cargarCSV.addActionListener(e -> derM.cargarDatosDesdeMPJ());

        mpjMenu.add(ejecutarMPJ);
        mpjMenu.add(cargarCSV);
        mb.add(mpjMenu);

        // ====== Menú Sincronización ======
        JMenu synch = new JMenu("Sincronización");
        synch.add(crearItemSync("Semáforos", SyncMode.SEMAFOROS));
        synch.add(crearItemSync("Variables de condición", SyncMode.VAR_CONDICION));
        synch.add(crearItemSync("Monitores", SyncMode.MONITORES));
        synch.add(crearItemSync("Mutex", SyncMode.MUTEX));
        synch.add(crearItemSync("Barreras", SyncMode.BARRERAS));
        mb.add(synch);

        // ====== Menú Gráfica ======
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

        // ====== Menú Problemas ======
        JMenu prob = new JMenu("Problemas");
        prob.add(crearItemProblema("Productor-Consumidor"));
        prob.add(crearItemProblema("Cena de Filósofos"));
        prob.add(crearItemProblema("Barbero Dormilón"));
        prob.add(crearItemProblema("Fumadores"));
        prob.add(crearItemProblema("Lectores-Escritores"));
        prob.add(crearItemProblema("Clúster GPU"));
        mb.add(prob);

        // ====== Menú Deadlock ======
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

    // ==================== Simulador local de 5 cores ====================

    private void iniciarSimulador() {
        if (simulador != null && simulador.estaEjecutando()) {
            return;
        }

        simulador = new SimuladorCincoCore(metricasCollector);
        simulador.iniciar();

        JOptionPane.showMessageDialog(this,
                "✅ Simulador local de 5 Cores ejecutándose en paralelo\n\n" +
                        "🔵 Core 0: Semáforos\n" +
                        "🔴 Core 1: Variables de Condición\n" +
                        "🟢 Core 2: Monitores\n" +
                        "🟠 Core 3: Mutex\n" +
                        "🟣 Core 4: Barreras\n\n" +
                        "Las gráficas muestran datos simulados de cada core.",
                "Simulador 5 Cores Activo", JOptionPane.INFORMATION_MESSAGE);
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
                    <li>Cada core ejecuta N operaciones</li>
                    <li>Se miden operaciones exitosas vs conflictos</li>
                    <li>Las gráficas se actualizan periódicamente</li>
                </ul>
                </html>
                """;

        JOptionPane.showMessageDialog(this, info, "Info 5 Cores", JOptionPane.INFORMATION_MESSAGE);
    }

    // ==================== Ejecución de MPJ real ====================

    /**
     * Ejecuta MPJ (SyncMetricsMPJ) como proceso externo usando mpjrun.bat,
     * configurando CLASSPATH para incluir target/classes y mpj.jar.
     * Al terminar, carga mpj_metrics.csv en la gráfica.
     */
    private void ejecutarMPJ() {
        try {
            // Directorio del proyecto (donde estás parado con NetBeans/Maven)
            String proyectoDir = System.getProperty("user.dir");
            File workDir = new File(proyectoDir);

            // Ruta estándar de instalación de MPJ
            String mpjHome = "C:\\mpj";
            String mpjBin = mpjHome + "\\bin\\mpjrun.bat";
            String mpjJar = mpjHome + "\\lib\\mpj.jar";

            // Carpeta de clases compiladas
            String classesDir = proyectoDir + "\\target\\classes";

            // Construimos el proceso:
            ProcessBuilder pb = new ProcessBuilder(
                    mpjBin,
                    "-np", "5",
                    "com.mycompany.proyectopcypoto2025.SyncMetricsMPJ"
            );

            pb.directory(workDir);

            // Variables de entorno para que MPJ vea todo
            Map<String, String> env = pb.environment();
            env.put("MPJ_HOME", mpjHome);
            // CLASSPATH: nuestras clases + mpj.jar + lo que ya tuviera
            String originalCp = env.getOrDefault("CLASSPATH", "");
            String nuevoCp = classesDir + ";" + mpjJar;
            if (!originalCp.isEmpty()) {
                nuevoCp = nuevoCp + ";" + originalCp;
            }
            env.put("CLASSPATH", nuevoCp);
            // PATH para encontrar mpjrun.bat y Java
            String originalPath = env.getOrDefault("PATH", "");
            env.put("PATH", mpjHome + "\\bin;" + originalPath);

            pb.redirectErrorStream(true);
            Process p = pb.start();

            // Leer salida de MPJ y mostrarla en consola de NetBeans
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    System.out.println("[MPJ] " + line);
                }
            }

            int exit = p.waitFor();
            System.out.println("[MPJ] Proceso terminado con código: " + exit);

            if (exit == 0) {
                // Cargar CSV en la gráfica
                derM.cargarDatosDesdeMPJ();
                JOptionPane.showMessageDialog(this,
                        "MPJ finalizó correctamente.\nSe cargaron las métricas en la gráfica.",
                        "MPJ completado", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "MPJ terminó con código de error: " + exit +
                                "\nRevisa la consola para ver el detalle.",
                        "Error al ejecutar MPJ", JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error al ejecutar MPJ: " + ex.getMessage(),
                    "Error MPJ", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ==================== Helpers de menú ====================

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

    // ==================== main ====================

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
