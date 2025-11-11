package com.mycompany.proyectopcypoto2025.modules;

import com.mycompany.proyectopcypoto2025.graphics.AnimationBridge;
import com.mycompany.proyectopcypoto2025.utils.SyncHelpers;
import com.mycompany.proyectopcypoto2025.utils.ProblemModule;

import javax.swing.*;
import java.util.concurrent.*;

public class SmokersModule implements ProblemModule {
    private JPanel panel;
    private JLabel status;
    private volatile boolean running = false;
    private Thread agent;
    private Thread[] smokers;
    private SyncHelpers syncHelpers;
    private AnimationBridge bridge;
    private int[] smokerStates = new int[3];

    public SmokersModule(SyncHelpers syncHelpers) {
        this.syncHelpers = syncHelpers;
        panel = new JPanel();
        status = new JLabel("Fumadores: inactivo");
        panel.add(status);
        panel.setBorder(javax.swing.BorderFactory.createTitledBorder("Fumadores"));
    }

    @Override public JPanel getPanel() { return panel; }
    @Override public String getName() { return "Fumadores"; }

    @Override public void start(String technique, AnimationBridge bridge) {
        stop();
        this.bridge = bridge;
        running = true;
        status.setText("Fumadores: ejecutando ("+technique+")");
        final Semaphore tobacco = new Semaphore(0);
        final Semaphore paper = new Semaphore(0);
        final Semaphore match = new Semaphore(0);
        final Semaphore agentSem = new Semaphore(1);
        smokers = new Thread[3];
        smokers[0] = new Thread(() -> { try {
            while (running) {
                paper.acquire(); match.acquire();
                smokerStates[0]=1; if (bridge!=null) bridge.setSmokersState(smokerStates);
                Thread.sleep(100);
                smokerStates[0]=0; if (bridge!=null) bridge.setSmokersState(smokerStates);
                agentSem.release();
            }
        } catch (InterruptedException e){Thread.currentThread().interrupt();}}, "Smoker-0");
        smokers[1] = new Thread(() -> { try {
            while (running) {
                tobacco.acquire(); match.acquire();
                smokerStates[1]=1; if (bridge!=null) bridge.setSmokersState(smokerStates);
                Thread.sleep(100);
                smokerStates[1]=0; if (bridge!=null) bridge.setSmokersState(smokerStates);
                agentSem.release();
            }
        } catch (InterruptedException e){Thread.currentThread().interrupt();}}, "Smoker-1");
        smokers[2] = new Thread(() -> { try {
            while (running) {
                tobacco.acquire(); paper.acquire();
                smokerStates[2]=1; if (bridge!=null) bridge.setSmokersState(smokerStates);
                Thread.sleep(100);
                smokerStates[2]=0; if (bridge!=null) bridge.setSmokersState(smokerStates);
                agentSem.release();
            }
        } catch (InterruptedException e){Thread.currentThread().interrupt();}}, "Smoker-2");
        agent = new Thread(() -> {
            try {
                while (running) {
                    agentSem.acquire();
                    int r = ThreadLocalRandom.current().nextInt(3);
                    switch (r) {
                        case 0: paper.release(); match.release(); break;
                        case 1: tobacco.release(); match.release(); break;
                        default: tobacco.release(); paper.release(); break;
                    }
                    Thread.sleep(150);
                }
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }, "Agent");
        for (Thread t: smokers) t.start();
        agent.start();
    }

    @Override public void stop() {
        running = false;
        if (agent!=null) agent.interrupt();
        if (smokers!=null) for (Thread t: smokers) if (t!=null) t.interrupt();
        syncHelpers.clearBarrier();
        status.setText("Fumadores: inactivo");
        if (bridge!=null) bridge.setSmokersState(new int[]{0,0,0});
    }
}
