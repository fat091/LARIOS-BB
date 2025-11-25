package com.mycompany.proyectopcypoto2025;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Ventana principal del proyecto PCyP Otoño 2025.
 *
 * Estructura:
 * - Lado izquierdo: PanelProblemas (animaciones de islas GPU, etc.)
 * - Lado derecho superior: GrafoPanel (grafo de recursos).
 * - Lado derecho inferior: MetricasGpuPanel (gráfica JFreeChart + slider).
 *
 * También expone un menú para cambiar el modo de sincronización y para
 * cargar datos generados por MPJ (archivo de métricas).
 */
public class ProyectoPCyPoto2025 extends JFrame {

    private final PanelProblemas panelProblemas = new PanelProblemas();
    private final GrafoPanel grafoPanel = new GrafoPanel();
    private final MetricasGpuPanel metricasPanel = new MetricasGpuPanel();

    public ProyectoPCyPoto2025() {
        super("Proyecto PCyP Otoño 2025 - Animaciones + Métricas GPU + 5 Cores");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1280, 800));
        setLocationRelativeTo(null);

        setJMenuBar(crearMenuBar());
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(crearContenidoPrincipal(), BorderLayout.CENTER);
    }

    /**
     * Construye el layout principal tal como se ve en la captura:
     * un JSplitPane donde a la izquierda está PanelProblemas
     * y a la derecha un panel con grafo arriba y métricas abajo.
     */
    private JComponent crearContenidoPrincipal() {
        // Panel derecho: grafo arriba, métricas abajo
        JPanel panelDerecho = new JPanel(new BorderLayout(8, 8));
        panelDerecho.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JScrollPane scrollGrafo = new JScrollPane(grafoPanel);
        scrollGrafo.setBorder(BorderFactory.createTitledBorder("Grafo de Recursos"));

        JPanel contMetricas = new JPanel(new BorderLayout());
        contMetricas.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        contMetricas.add(metricasPanel, BorderLayout.CENTER);

        panelDerecho.add(scrollGrafo, BorderLayout.CENTER);
        panelDerecho.add(contMetricas, BorderLayout.SOUTH);

        // Panel izquierdo (animaciones GPU)
        JPanel panelIzquierdo = new JPanel(new BorderLayout());
        panelIzquierdo.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 4));
        panelIzquierdo.add(panelProblemas, BorderLayout.CENTER);

        // JSplitPane general
        JSplitPane split = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                panelIzquierdo,
                panelDerecho
        );
        split.setOneTouchExpandable(true);
        split.setResizeWeight(0.45); // 45% izquierda, 55% derecha

        return split;
    }

    /**
     * Crea la barra de menú:
     * - Archivo → Salir
     * - Modo → Mutex / Semáforos / Monitores / Barreras / Variables de Condición
     * - MPJ / Métricas → Cargar archivo de métricas...
     */
    private JMenuBar crearMenuBar() {
        JMenuBar mb = new JMenuBar();

        // ===== Menú Archivo =====
        JMenu menuArchivo = new JMenu("Archivo");
        JMenuItem itemSalir = new JMenuItem("Salir");
        itemSalir.addActionListener(e -> System.exit(0));
        menuArchivo.add(itemSalir);
        mb.add(menuArchivo);

        // ===== Menú Modo de sincronización =====
        JMenu menuModo = new JMenu("Modo");
        ButtonGroup grupoModos = new ButtonGroup();

        menuModo.add(crearItemModo("Mutex", SyncMode.MUTEX, grupoModos, KeyEvent.VK_M));
        menuModo.add(crearItemModo("Semáforos", SyncMode.SEMAFOROS, grupoModos, KeyEvent.VK_S));
        menuModo.add(crearItemModo("Monitores", SyncMode.MONITORES, grupoModos, KeyEvent.VK_O));
        menuModo.add(crearItemModo("Barreras", SyncMode.BARRERAS, grupoModos, KeyEvent.VK_B));
        menuModo.add(crearItemModo("Var. Condición", SyncMode.VAR_CONDICION, grupoModos, KeyEvent.VK_V));

        mb.add(menuModo);

        // ===== Menú MPJ (cargar datos desde archivo generado) =====
        JMenu menuMPJ = new JMenu("MPJ / Métricas");

        JMenuItem itemCargar = new JMenuItem("Cargar archivo de métricas...");
        itemCargar.addActionListener(this::accionCargarDatosDesdeArchivo);
        menuMPJ.add(itemCargar);

        mb.add(menuMPJ);

        return mb;
    }

    private JRadioButtonMenuItem crearItemModo(
            String texto,
            SyncMode modo,
            ButtonGroup grupo,
            int keyEvent) {

        JRadioButtonMenuItem item = new JRadioButtonMenuItem(texto);
        item.setMnemonic(keyEvent);
        grupo.add(item);

        // Seleccionado por defecto
        if (modo == SyncMode.MONITORES) {
            item.setSelected(true);
        }

        item.addActionListener(e -> {
            metricasPanel.setModoActual(modo);
            // Si PanelProblemas también soporta modo, puedes descomentar:
            // panelProblemas.setModo(modo);
        });

        return item;
    }

    /**
     * Abre un JFileChooser para seleccionar el archivo de métricas generado
     * por MPJ. El formato esperado es:
     *
     *   iter;mutex;semaforos;monitores;barreras;varCond
     */
    private void accionCargarDatosDesdeArchivo(ActionEvent e) {
        JFileChooser chooser = new JFileChooser(new java.io.File("."));
        chooser.setDialogTitle("Selecciona archivo de métricas generado por MPJ");

        int r = chooser.showOpenDialog(this);
        if (r == JFileChooser.APPROVE_OPTION) {
            java.io.File f = chooser.getSelectedFile();
            try {
                metricasPanel.cargarDatosDesdeArchivo(f.toPath());
                JOptionPane.showMessageDialog(this,
                        "Datos cargados correctamente desde:\n" + f.getAbsolutePath(),
                        "Métricas GPU/MPJ",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Error al leer el archivo:\n" + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ProyectoPCyPoto2025 frame = new ProyectoPCyPoto2025();
            frame.setVisible(true);
        });
    }
}
