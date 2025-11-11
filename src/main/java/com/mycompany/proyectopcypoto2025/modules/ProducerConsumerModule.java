package com.mycompany.proyectopcypoto2025.modules;

import com.mycompany.proyectopcypoto2025.graphics.AnimationBridge;
import com.mycompany.proyectopcypoto2025.utils.SyncHelpers;
import com.mycompany.proyectopcypoto2025.utils.ProblemModule;

import javax.swing.*;
import java.util.concurrent.*;

public class ProducerConsumerModule implements ProblemModule {
    private JPanel panel;
    private JLabel status;
    private volatile boolean running = false;
    private Thread producer, consumer;
    private BlockingQueue<Integer> queue;
    private SyncHelpers syncHelpers;
    private AnimationBridge bridge;
    private int produced=0, consumed=0;

    public ProducerConsumerModule(SyncHelpers syncHelpers) {
        this.syncHelpers = syncHelpers;
        queue = new ArrayBlockingQueue<>(5);
        panel = new JPanel();
        status = new JLabel("Productor-Consumidor: inactivo");
        panel.add(status);
        panel.setBorder(javax.swing.BorderFactory.createTitledBorder("Productor-Consumidor"));
    }

    @Override public JPanel getPanel() { return panel; }
    @Override public String getName() { return "Productor-Consumidor"; }

    @Override
    public void start(String technique, AnimationBridge bridge) {
        stop();
        this.bridge = bridge;
        running = true;
        produced=0; consumed=0;
        status.setText("Productor-Consumidor: ejecutando ("+technique+")");
        if ("barrier".equals(technique)) syncHelpers.createBarrier(2, ()->{});
        producer = new Thread(() -> {
            int v=0;
            try {
                while (running) {
                    queue.put(v++);
                    produced++;
                    if (bridge!=null) bridge.setProducerConsumerState(produced, consumed, queue.size());
                    status.setText("Produjo "+(v-1)+" size="+queue.size());
                    if ("barrier".equals(technique)) syncHelpers.awaitBarrier();
                    Thread.sleep(120);
                }
            } catch (InterruptedException e){ Thread.currentThread().interrupt(); }
        }, "Producer");
        consumer = new Thread(() -> {
            try {
                while (running) {
                    Integer x = queue.take();
                    consumed++;
                    if (bridge!=null) bridge.setProducerConsumerState(produced, consumed, queue.size());
                    status.setText("Consumió "+x+" size="+queue.size());
                    if ("barrier".equals(technique)) syncHelpers.awaitBarrier();
                    Thread.sleep(180);
                }
            } catch (InterruptedException e){ Thread.currentThread().interrupt(); }
        }, "Consumer");
        producer.start(); consumer.start();
    }

    @Override public void stop() {
        running = false;
        if (producer!=null) producer.interrupt();
        if (consumer!=null) consumer.interrupt();
        syncHelpers.clearBarrier();
        status.setText("Productor-Consumidor: inactivo");
        if (bridge!=null) bridge.setProducerConsumerState(0,0,0);
    }
}
