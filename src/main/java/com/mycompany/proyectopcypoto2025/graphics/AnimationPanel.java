package com.mycompany.proyectopcypoto2025.graphics;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class AnimationPanel extends JPanel implements AnimationBridge {
    private int[] philosophers = new int[5];
    private int produced = 0, consumed = 0, bufferSize = 0;
    private int barberWaiting = 0; private boolean barberBusy = false;
    private int[] smokers = new int[3];
    private int readers = 0; private boolean writerActive = false;
    private Timer repaintTimer;
    private volatile boolean freezeRequested = false;
    private final Object freezeLock = new Object();

    public AnimationPanel() {
        setPreferredSize(new Dimension(740,700));
        setBackground(Color.WHITE);
        Arrays.fill(philosophers, 0);
        Arrays.fill(smokers, 0);
        repaintTimer = new Timer(60, e -> repaint());
        repaintTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        int w = getWidth(), h = getHeight();
        drawPhilosophers(g, w, h);
        g.dispose();

        if (freezeRequested) {
            synchronized (freezeLock) {
                try {
                    freezeLock.wait();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void drawPhilosophers(Graphics2D g, int w, int h) {
        int n = philosophers.length;
        int cx = w/2, cy = h/2, radius = Math.min(w,h)/3;
        for (int i=0;i<n;i++) {
            double angle = 2*Math.PI*i/n - Math.PI/2;
            int x = (int)(cx + Math.cos(angle)*radius);
            int y = (int)(cy + Math.sin(angle)*radius);
            int r = 30;
            Color fill = Color.LIGHT_GRAY;
            switch(philosophers[i]) {
                case 0: fill = Color.CYAN; break;
                case 1: fill = Color.ORANGE; break;
                case 2: fill = Color.GREEN; break;
                case 3: fill = Color.RED; break;
            }
            g.setColor(fill);
            g.fillOval(x-r, y-r, r*2, r*2);
            g.setColor(Color.BLACK);
            g.drawOval(x-r, y-r, r*2, r*2);
            g.drawString("P"+i, x-6, y+4);
            int fx = (int)(cx + Math.cos(angle + 0.4)* (radius-60));
            int fy = (int)(cy + Math.sin(angle + 0.4)* (radius-60));
            g.setStroke(new BasicStroke(4));
            g.setColor(Color.DARK_GRAY);
            g.drawLine(cx, cy, fx, fy);
        }
    }

    @Override
    public void setPhilosophersState(int[] states) {
        if (states == null) return;
        synchronized (philosophers) {
            int len = Math.min(states.length, philosophers.length);
            for (int i=0;i<len;i++) philosophers[i]=states[i];
        }
    }

    @Override
    public void setProducerConsumerState(int produced, int consumed, int bufferSize) {
        this.produced = produced; this.consumed = consumed; this.bufferSize = bufferSize;
    }

    @Override
    public void setBarberState(int waiting, boolean barberBusy) {
        this.barberWaiting = waiting; this.barberBusy = barberBusy;
    }

    @Override
    public void setSmokersState(int[] smokerStates) {
        if (smokerStates == null) return;
        synchronized (smokers) {
            int len = Math.min(smokerStates.length, smokers.length);
            for (int i=0;i<len;i++) smokers[i]=smokerStates[i];
        }
    }

    @Override
    public void setReadersWritersState(int readers, boolean writerActive) {
        this.readers = readers; this.writerActive = writerActive;
    }

    @Override
    public void requestFreezeUI() {
        freezeRequested = true;
        SwingUtilities.invokeLater(() -> repaint());
    }

    @Override
    public void requestUnfreezeUI() {
        freezeRequested = false;
        synchronized (freezeLock) {
            freezeLock.notifyAll();
        }
    }
}
