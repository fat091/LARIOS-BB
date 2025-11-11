package com.mycompany.proyectopcypoto2025.modules;

import com.mycompany.proyectopcypoto2025.graphics.AnimationBridge;
import com.mycompany.proyectopcypoto2025.utils.SyncHelpers;
import com.mycompany.proyectopcypoto2025.utils.ProblemModule;

import javax.swing.*;
import java.util.concurrent.*;

public class BarberModule implements ProblemModule {
    private JPanel panel;
    private JLabel status;
    private volatile boolean running = false;
    private Thread barber, customersThread;
    private SyncHelpers syncHelpers;
    private AnimationBridge bridge;

    public BarberModule(SyncHelpers syncHelpers) {
        this.syncHelpers = syncHelpers;
        panel = new JPanel();
        status = new JLabel("Barbero: inactivo");
        panel.add(status);
        panel.setBorder(javax.swing.BorderFactory.createTitledBorder("Barbero Dormilón"));
    }

    @Override public JPanel getPanel() { return panel; }
    @Override public String getName() { return "Barbero"; }

    @Override public void start(String technique, AnimationBridge bridge) {
        stop();
        this.bridge = bridge;
        running = true;
        status.setText("Barbero: ejecutando ("+technique+")");
        final BlockingQueue<Integer> chairs = new ArrayBlockingQueue<>(3);
        barber = new Thread(() -> {
            try {
                while (running) {
                    Integer c = chairs.take();
                    status.setText("Barbero atiende cliente "+c+" ocupadas="+chairs.size());
                    Thread.sleep(150);
                }
            } catch (InterruptedException e){ Thread.currentThread().interrupt(); }
        }, "Barber");
        customersThread = new Thread(() -> {
            try {
                int id=0;
                while (running) {
                    if (chairs.offer(id)) {
                        status.setText("Cliente "+id+" se sienta ocupadas="+chairs.size());
                    } else {
                        status.setText("Cliente "+id+" se va (no hay sillas)");
                    }
                    id++;
                    Thread.sleep(120);
                }
            } catch (InterruptedException e){ Thread.currentThread().interrupt(); }
        }, "Customers");
        barber.start(); customersThread.start();
    }

    @Override public void stop() {
        running = false;
        if (barber!=null) barber.interrupt();
        if (customersThread!=null) customersThread.interrupt();
        syncHelpers.clearBarrier();
        status.setText("Barbero: inactivo");
    }
}
