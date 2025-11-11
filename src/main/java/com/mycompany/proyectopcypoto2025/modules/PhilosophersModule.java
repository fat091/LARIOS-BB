package com.mycompany.proyectopcypoto2025.modules;

import com.mycompany.proyectopcypoto2025.graphics.AnimationBridge;
import com.mycompany.proyectopcypoto2025.utils.SyncHelpers;
import com.mycompany.proyectopcypoto2025.utils.ProblemModule;

import javax.swing.*;
import java.util.concurrent.*;

public class PhilosophersModule implements ProblemModule {
    private JPanel panel;
    private JLabel status;
    private volatile boolean running = false;
    private Thread[] threads;
    private final Object[] forks;
    private final int n;
    private SyncHelpers syncHelpers;
    private AnimationBridge bridge;
    private int[] state;

    public PhilosophersModule(int n, SyncHelpers syncHelpers) {
        this.n = n;
        this.syncHelpers = syncHelpers;
        forks = new Object[n];
        for (int i=0;i<n;i++) forks[i] = new Object();
        panel = new JPanel();
        status = new JLabel("Filósofos: inactivo");
        panel.add(status);
        panel.setBorder(javax.swing.BorderFactory.createTitledBorder("Cena de filósofos"));
        state = new int[n];
    }

    @Override public JPanel getPanel() { return panel; }
    @Override public String getName() { return "Filosofos"; }

    @Override
    public void start(String technique, AnimationBridge bridge) {
        // stop previous safely
        stop();
        this.bridge = bridge;
        running = true;
        status.setText("Filosofos: ejecutando ("+technique+")");
        if ("deadlock-execute".equals(technique)) startDeadlock();
        else if ("deadlock-avoid".equals(technique)) startAvoidDeadlock();
        else if ("barrier".equals(technique)) startWithBarrier();
        else startAvoidDeadlock();
    }

    private void publish() { if (bridge!=null) bridge.setPhilosophersState(state); }

    private void startDeadlock() {
        threads = new Thread[n];
        for (int i=0;i<n;i++) {
            final int id=i;
            threads[i] = new Thread(() -> {
                try {
                    while (running) {
                        state[id]=0; publish();
                        Thread.sleep(60);
                        state[id]=1; publish();
                        Object left = forks[id];
                        Object right = forks[(id+1)%n];
                        synchronized(left) {
                            state[id]=1; publish();
                            Thread.sleep(30);
                            synchronized(right) {
                                state[id]=2; publish();
                                Thread.sleep(100);
                                state[id]=0; publish();
                            }
                        }
                        Thread.sleep(50);
                    }
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }, "Philo-dead-"+i);
            threads[i].start();
        }
        if (bridge!=null) bridge.requestFreezeUI();
    }

    private void startAvoidDeadlock() {
        threads = new Thread[n];
        for (int i=0;i<n;i++) {
            final int id=i;
            threads[i] = new Thread(() -> {
                try {
                    while (running) {
                        state[id]=0; publish();
                        Thread.sleep(60);
                        int leftIndex = id;
                        int rightIndex = (id+1)%n;
                        int first = Math.min(leftIndex, rightIndex);
                        int second = Math.max(leftIndex, rightIndex);
                        synchronized(forks[first]) {
                            state[id]=1; publish();
                            Thread.sleep(20);
                            synchronized(forks[second]) {
                                state[id]=2; publish();
                                Thread.sleep(80);
                                state[id]=0; publish();
                            }
                        }
                        Thread.sleep(50);
                    }
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }, "Philo-safe-"+i);
            threads[i].start();
        }
    }

    private void startWithBarrier() {
        syncHelpers.createBarrier(n, () -> {});
        startAvoidDeadlock();
    }

    @Override
    public void stop() {
        running = false;
        if (threads == null) return;
        for (int i=0;i<threads.length;i++) {
            if (threads[i] != null) threads[i].interrupt();
        }
        if (bridge!=null) bridge.requestUnfreezeUI();
        syncHelpers.clearBarrier();
        status.setText("Filósofos: inactivo");
        for (int i=0;i<state.length;i++) state[i]=0;
        if (bridge!=null) bridge.setPhilosophersState(state);
    }
}
