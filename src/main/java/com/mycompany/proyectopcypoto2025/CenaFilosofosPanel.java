package com.mycompany.proyectopcypoto2025;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.concurrent.Semaphore;

public class CenaFilosofosPanel extends JPanel implements Reseteable, Demoable, SyncAware, DeadlockAware {
    private final Mesa mesa = new Mesa();
    private SyncMode modo = SyncMode.SEMAFOROS;
    private volatile boolean running = false;
    private Thread[] filos;
    private volatile boolean deadlocked = false;

    public CenaFilosofosPanel() {
        setLayout(new BorderLayout());
        add(mesa, BorderLayout.CENTER);
        start();
    }

    private void start() {
        if (running) return;
        running = true;
        filos = new Thread[5];
        for (int i = 0; i < 5; i++) {
            final int id = i;
            filos[i] = new Thread(() -> ciclo(id), "F" + id);
            filos[i].start();
        }
    }

    private final Semaphore camarero = new Semaphore(4, true);
    private final Semaphore[] ten = {
        new Semaphore(1), new Semaphore(1), new Semaphore(1),
        new Semaphore(1), new Semaphore(1)
    };

    private void ciclo(int id) {
        while (running && !deadlocked) {
            mesa.setEstado(id, Mesa.Estado.PENSANDO);
            dormir(220);

            try {
                mesa.setEstado(id, Mesa.Estado.HAMBRIENTO);

                if (modo == SyncMode.SEMAFOROS) camarero.acquire();
                ten[id].acquire();
                ten[(id + 1) % 5].acquire();
                mesa.ocupaTenedor(id, true);
                mesa.ocupaTenedor((id + 1) % 5, true);

                mesa.setEstado(id, Mesa.Estado.COMIENDO);
                dormir(260);
            } catch (Exception ignored) {
            } finally {
                mesa.ocupaTenedor(id, false);
                mesa.ocupaTenedor((id + 1) % 5, false);
                ten[(id + 1) % 5].release();
                ten[id].release();
                if (modo == SyncMode.SEMAFOROS) camarero.release();
            }
        }
    }

    @Override
    public void setSyncMode(SyncMode m) {
        this.modo = m;
    }

    @Override
    public void setDeadlock(boolean on) {
        deadlocked = on;
        if (on) {
            running = false;
            mesa.forceDeadlock();
        } else {
            mesa.reset();
            start();
        }
    }

    @Override
    public void reset() {
        running = false;
        mesa.reset();
        start();
    }

    @Override
    public void demo() {
    }

    private void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception ignored) {
        }
    }

    static class Mesa extends JPanel {
        enum Estado {
            PENSANDO, HAMBRIENTO, COMIENDO
        }

        private final Estado[] est = new Estado[]{
            Estado.PENSANDO, Estado.PENSANDO, Estado.PENSANDO,
            Estado.PENSANDO, Estado.PENSANDO
        };
        private final boolean[] forkBusy = new boolean[5];
        private final int[] mouthAngles = new int[5];
        private Timer animTimer;

        public Mesa() {
            setPreferredSize(new Dimension(560, 300));
            setBackground(Color.white);

            animTimer = new Timer(100, e -> {
                for (int i = 0; i < 5; i++) {
                    if (est[i] == Estado.COMIENDO) {
                        mouthAngles[i] = (mouthAngles[i] + 20) % 360;
                    }
                }
                repaint();
            });
            animTimer.start();
        }

        public synchronized void setEstado(int i, Estado e) {
            est[i] = e;
        }

        public synchronized void ocupaTenedor(int i, boolean b) {
            forkBusy[i] = b;
        }

        public synchronized void reset() {
            for (int i = 0; i < 5; i++) {
                est[i] = Estado.PENSANDO;
                forkBusy[i] = false;
            }
        }

        public synchronized void forceDeadlock() {
            for (int i = 0; i < 5; i++) {
                est[i] = Estado.HAMBRIENTO;
                forkBusy[i] = true;
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int cx = getWidth() / 2, cy = getHeight() / 2;
            int R = 110;

            g2.setColor(new Color(100, 100, 100, 50));
            g2.fillOval(cx - 75, cy - 50, 150, 100);

            GradientPaint mesaGrad = new GradientPaint(
                    cx - 70, cy - 45, new Color(210, 180, 140),
                    cx + 70, cy + 45, new Color(160, 130, 90)
            );
            g2.setPaint(mesaGrad);
            g2.fillOval(cx - 70, cy - 45, 140, 90);

            g2.setColor(new Color(139, 69, 19));
            g2.setStroke(new BasicStroke(3f));
            g2.drawOval(cx - 70, cy - 45, 140, 90);

            synchronized (this) {
                for (int i = 0; i < 5; i++) {
                    double ang = i * 2 * Math.PI / 5 - Math.PI / 2;
                    int px = cx + (int) (Math.cos(ang) * (R - 40));
                    int py = cy + (int) (Math.sin(ang) * (R - 40));
                    g2.setColor(Color.WHITE);
                    g2.fillOval(px - 10, py - 10, 20, 20);
                    g2.setColor(Color.GRAY);
                    g2.drawOval(px - 10, py - 10, 20, 20);
                }

                for (int i = 0; i < 5; i++) {
                    double ang = i * 2 * Math.PI / 5 - Math.PI / 2 + Math.PI / 5;
                    int x = cx + (int) (Math.cos(ang) * R * 0.75);
                    int y = cy + (int) (Math.sin(ang) * R * 0.75);

                    dibujarTenedor(g2, x, y, forkBusy[i], ang);
                }

                Color[] colores = {
                    new Color(255, 100, 100), new Color(100, 255, 100),
                    new Color(100, 100, 255), new Color(255, 255, 100),
                    new Color(255, 100, 255)
                };

                for (int i = 0; i < 5; i++) {
                    double ang = i * 2 * Math.PI / 5 - Math.PI / 2;
                    int x = cx + (int) (Math.cos(ang) * R);
                    int y = cy + (int) (Math.sin(ang) * R);

                    dibujarFilosofo(g2, x, y, i, colores[i], est[i]);
                }
            }

            g2.dispose();
        }

        private void dibujarFilosofo(Graphics2D g2, int x, int y, int id, Color baseColor, Estado estado) {
            Color color = baseColor;
            if (estado == Estado.COMIENDO) color = color.brighter();
            else if (estado == Estado.HAMBRIENTO) color = color.darker();

            g2.setColor(color);
            g2.fillRect(x - 16, y - 8, 32, 26);

            g2.setColor(new Color(255, 220, 180));
            g2.fillOval(x - 14, y - 24, 28, 28);

            g2.setColor(Color.BLACK);
            g2.fillOval(x - 8, y - 18, 4, 4);
            g2.fillOval(x + 4, y - 18, 4, 4);

            if (estado == Estado.COMIENDO) {
                int apertura = (int) (Math.abs(Math.sin(Math.toRadians(mouthAngles[id]))) * 6);
                g2.fillOval(x - 4, y - 8, 8, 4 + apertura);
            } else {
                g2.drawArc(x - 6, y - 10, 12, 8, 0, -180);
            }

            g2.setColor(color.darker());
            g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x - 16, y, x - 22, y + 12);
            g2.drawLine(x + 16, y, x + 22, y + 12);

            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(5f));
            g2.drawLine(x - 6, y + 18, x - 6, y + 32);
            g2.drawLine(x + 6, y + 18, x + 6, y + 32);

            g2.setColor(Color.BLACK);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 10f));
            g2.drawString("F" + id, x - 4, y + 42);

            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 8f));
            String estadoText = switch (estado) {
                case PENSANDO -> "P";
                case HAMBRIENTO -> "H";
                case COMIENDO -> "C";
            };
            g2.drawString(estadoText, x - 3, y + 52);
        }

        private void dibujarTenedor(Graphics2D g2, int x, int y, boolean ocupado, double angulo) {
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(ocupado ? Color.RED : new Color(180, 180, 180));

            AffineTransform old = g2.getTransform();
            g2.translate(x, y);
            g2.rotate(angulo + Math.PI / 2);

            g2.fillRect(-1, -14, 2, 18);
            g2.fillRect(-4, -14, 2, 5);
            g2.fillRect(-1, -14, 2, 5);
            g2.fillRect(2, -14, 2, 5);

            g2.setTransform(old);
        }
    }
}