package com.mycompany.proyectopcypoto2025.modules;

import com.mycompany.proyectopcypoto2025.graphics.AnimationBridge;
import com.mycompany.proyectopcypoto2025.utils.SyncHelpers;
import com.mycompany.proyectopcypoto2025.utils.ProblemModule;

import javax.swing.*;
import java.util.concurrent.*;

public class ReadersWritersModule implements ProblemModule {
    private JPanel panel;
    private JLabel status;
    private volatile boolean running = false;
    private Thread readersThread, writersThread;
    private SyncHelpers syncHelpers;
    private AnimationBridge bridge;

    public ReadersWritersModule(SyncHelpers syncHelpers) {
        this.syncHelpers = syncHelpers;
        panel = new JPanel();
        status = new JLabel("Lectores-Escritores: inactivo");
        panel.add(status);
        panel.setBorder(javax.swing.BorderFactory.createTitledBorder("Lectores-Escritores"));
    }

    @Override public JPanel getPanel() { return panel; }
    @Override public String getName() { return "LectoresEscritores"; }

    @Override public void start(String technique, AnimationBridge bridge) {
        stop();
        this.bridge = bridge;
        running = true;
        status.setText("Lectores-Escritores: ejecutando ("+technique+")");
        readersThread = new Thread(() -> {
            try {
                while (running) {
                    if (bridge!=null) bridge.setReadersWritersState(1,false);
                    Thread.sleep(100);
                    if (bridge!=null) bridge.setReadersWritersState(0,false);
                    Thread.sleep(80);
                }
            } catch (InterruptedException e){Thread.currentThread().interrupt(); }
        }, "Readers");
        writersThread = new Thread(() -> {
            try {
                while (running) {
                    if (bridge!=null) bridge.setReadersWritersState(0,true);
                    Thread.sleep(150);
                    if (bridge!=null) bridge.setReadersWritersState(0,false);
                    Thread.sleep(120);
                }
            } catch (InterruptedException e){Thread.currentThread().interrupt(); }
        }, "Writers");
        readersThread.start(); writersThread.start();
    }

    @Override public void stop() {
        running = false;
        if (readersThread!=null) readersThread.interrupt();
        if (writersThread!=null) writersThread.interrupt();
        syncHelpers.clearBarrier();
        status.setText("Lectores-Escritores: inactivo");
        if (bridge!=null) bridge.setReadersWritersState(0,false);
    }
}
