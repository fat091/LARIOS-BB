package com.mycompany.proyectopcypoto2025;

import com.mycompany.proyectopcypoto2025.graphics.AnimationPanel;
import com.mycompany.proyectopcypoto2025.graphics.ResourceGraphPanel;
import com.mycompany.proyectopcypoto2025.graphics.ChartPanelSimple;
import com.mycompany.proyectopcypoto2025.modules.*;
import com.mycompany.proyectopcypoto2025.utils.SyncHelpers;
import com.mycompany.proyectopcypoto2025.utils.ProblemModule;

import javax.swing.*;
import java.awt.*;
import java.util.*;

public class ProyectoPCyPoto2025 extends JFrame {
    private AnimationPanel animationPanel;
    private ResourceGraphPanel graphPanel;
    private ChartPanelSimple chartPanel;
    private JPanel rightPane;
    private JMenuBar menuBar;
    private JMenu archivoMenu, synchMenu, problemasMenu, deadlockMenu, graficaMenu;
    private JMenuItem salirItem;
    private ButtonGroup techniqueGroup;
    private JRadioButtonMenuItem semaforoRadio, conditionRadio, monitorRadio, barreraRadio, mutexRadio;
    private JMenuItem ejecutarDeadlock, evitarDeadlock;
    private SyncHelpers syncHelpers;
    private Map<String, ProblemModule> modules;
    private ProblemModule currentModule;

    public ProyectoPCyPoto2025() {
        super("ProyectoPCyPoto2025 - Corregido");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100,700);
        setLocationRelativeTo(null);
        syncHelpers = new SyncHelpers();
        modules = new LinkedHashMap<>();
        initUI();
        createModules();
    }

    private void initUI() {
        animationPanel = new AnimationPanel();
        graphPanel = new ResourceGraphPanel();
        chartPanel = new ChartPanelSimple();
        rightPane = new JPanel(new GridLayout(2,1));
        rightPane.setPreferredSize(new Dimension(300,600));
        rightPane.add(graphPanel);
        rightPane.add(chartPanel);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(animationPanel, BorderLayout.CENTER);
        getContentPane().add(rightPane, BorderLayout.EAST);

        // menus
        menuBar = new JMenuBar();
        archivoMenu = new JMenu("Archivo");
        salirItem = new JMenuItem("Salir");
        salirItem.addActionListener(e -> System.exit(0));
        archivoMenu.add(salirItem);

        synchMenu = new JMenu("Synch");
        techniqueGroup = new ButtonGroup();
        semaforoRadio = new JRadioButtonMenuItem("Semaphore");
        conditionRadio = new JRadioButtonMenuItem("Condition");
        monitorRadio = new JRadioButtonMenuItem("Monitor");
        barreraRadio = new JRadioButtonMenuItem("Barrier");
        mutexRadio = new JRadioButtonMenuItem("Mutex");
        techniqueGroup.add(semaforoRadio); techniqueGroup.add(conditionRadio);
        techniqueGroup.add(monitorRadio); techniqueGroup.add(barreraRadio); techniqueGroup.add(mutexRadio);
        barreraRadio.setSelected(true);
        synchMenu.add(semaforoRadio); synchMenu.add(conditionRadio); synchMenu.add(monitorRadio);
        synchMenu.add(barreraRadio); synchMenu.add(mutexRadio);

        problemasMenu = new JMenu("Problemas");
        JMenuItem proconItem = new JMenuItem("Productor-Consumidor");
        JMenuItem filosItem = new JMenuItem("Filósofos");
        JMenuItem barberoItem = new JMenuItem("Barbero Dormilón");
        JMenuItem fumadoresItem = new JMenuItem("Fumadores");
        JMenuItem lecescItem = new JMenuItem("Lectores-Escritores");
        proconItem.addActionListener(e -> selectModule("Productor-Consumidor"));
        filosItem.addActionListener(e -> selectModule("Filosofos"));
        barberoItem.addActionListener(e -> selectModule("Barbero"));
        fumadoresItem.addActionListener(e -> selectModule("Fumadores"));
        lecescItem.addActionListener(e -> selectModule("LectoresEscritores"));
        problemasMenu.add(proconItem); problemasMenu.add(filosItem); problemasMenu.add(barberoItem);
        problemasMenu.add(fumadoresItem); problemasMenu.add(lecescItem);

        deadlockMenu = new JMenu("Deadlock");
        ejecutarDeadlock = new JMenuItem("Ejecutar");
        evitarDeadlock = new JMenuItem("Evitar");
        ejecutarDeadlock.addActionListener(e -> onExecuteDeadlock());
        evitarDeadlock.addActionListener(e -> onAvoidDeadlock());
        deadlockMenu.add(ejecutarDeadlock); deadlockMenu.add(evitarDeadlock);

        graficaMenu = new JMenu("Gráfica");
        JRadioButtonMenuItem scroll = new JRadioButtonMenuItem("Scroll");
        JRadioButtonMenuItem carrusel = new JRadioButtonMenuItem("Carrusel");
        JRadioButtonMenuItem acordeon = new JRadioButtonMenuItem("Acordeon");
        ButtonGroup g2 = new ButtonGroup(); g2.add(scroll); g2.add(carrusel); g2.add(acordeon);
        scroll.setSelected(true);
        graficaMenu.add(scroll); graficaMenu.add(carrusel); graficaMenu.add(acordeon);

        menuBar.add(archivoMenu); menuBar.add(synchMenu); menuBar.add(problemasMenu);
        menuBar.add(deadlockMenu); menuBar.add(graficaMenu);
        setJMenuBar(menuBar);
    }

    private void createModules() {
        modules.put("Filosofos", new PhilosophersModule(5, syncHelpers));
        modules.put("Productor-Consumidor", new ProducerConsumerModule(syncHelpers));
        modules.put("Barbero", new BarberModule(syncHelpers));
        modules.put("Fumadores", new SmokersModule(syncHelpers));
        modules.put("LectoresEscritores", new ReadersWritersModule(syncHelpers));
        selectModule("Filosofos");
    }

    private String getSelectedTechnique() {
        if (semaforoRadio.isSelected()) return "semaphore";
        if (conditionRadio.isSelected()) return "condition";
        if (monitorRadio.isSelected()) return "monitor";
        if (barreraRadio.isSelected()) return "barrier";
        if (mutexRadio.isSelected()) return "mutex";
        return "monitor";
    }

    private void selectModule(String name) {
        if (currentModule != null) currentModule.stop();
        currentModule = modules.get(name);
        if (currentModule != null) {
            currentModule.start(getSelectedTechnique(), animationPanel);
        }
    }

    private void onExecuteDeadlock() {
        if (currentModule != null) {
            try {
                currentModule.start("deadlock-execute", animationPanel);
            } catch (Exception ex) {
                animationPanel.requestFreezeUI();
            }
        }
    }

    private void onAvoidDeadlock() {
        if (currentModule != null) {
            if (currentModule.getName().equals("Filosofos")) currentModule.start("deadlock-avoid", animationPanel);
            else currentModule.start(getSelectedTechnique(), animationPanel);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ProyectoPCyPoto2025 app = new ProyectoPCyPoto2025();
            app.setVisible(true);
        });
    }
}
