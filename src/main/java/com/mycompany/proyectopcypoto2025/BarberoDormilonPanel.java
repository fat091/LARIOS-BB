package com.mycompany.proyectopcypoto2025;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedList;
import java.util.Queue;

public class BarberoDormilonPanel extends JPanel implements Reseteable, Demoable, SyncAware {
    private final Sala sala = new Sala();
    private volatile boolean running = false;
    private Thread bar, clientes;
    private SyncMode modo = SyncMode.SEMAFOROS;

    public BarberoDormilonPanel() {
        setLayout(new BorderLayout());
        add(sala, BorderLayout.CENTER);
        start();
    }

    private void start() {
        if (running) return;
        running = true;

        bar = new Thread(() -> {
            while (running) {
                Integer id = null;
                synchronized (sala) {
                    if (!sala.queue.isEmpty()) {
                        id = sala.queue.poll();
                        sala.barberState = "CORTANDO";
                    } else {
                        sala.barberState = "DURMIENDO";
                    }
                }

                if (id != null) {
                    sala.sillaAct = id;
                    dormir(300);
                    sala.sillaAct = -1;
                } else {
                    dormir(80);
                }
            }
        }, "Barbero");

        clientes = new Thread(() -> {
            int id = 1;
            while (running) {
                synchronized (sala) {
                    if (sala.queue.size() < 3) {
                        sala.queue.add(id);
                    }
                }
                id++;
                dormir(180);
            }
        }, "Clientes");

        bar.start();
        clientes.start();
    }

    @Override
    public void setSyncMode(SyncMode m) {
        this.modo = m;
    }

    @Override
    public void reset() {
        running = false;
        synchronized (sala) {
            sala.queue.clear();
            sala.sillaAct = -1;
            sala.barberState = "DURMIENDO";
        }
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

    static class Sala extends JPanel {
        final Queue<Integer> queue = new LinkedList<>();
        int sillaAct = -1;
        String barberState = "DURMIENDO";
        private int scissorsAnim = 0;
        private int zzzOffset = 0;
        private Timer animTimer;

        public Sala() {
            setPreferredSize(new Dimension(560, 280));
            setBackground(Color.white);

            animTimer = new Timer(100, e -> {
                scissorsAnim = (scissorsAnim + 1) % 20;
                zzzOffset = (zzzOffset + 1) % 30;
                repaint();
            });
            animTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(new Color(230, 230, 200));
            g2.fillRect(0, 0, getWidth(), getHeight() - 60);

            g2.setColor(new Color(200, 200, 200));
            g2.fillRect(0, getHeight() - 60, getWidth(), 60);

            int sx = 40, sy = 160;
            int gap = 110;

            synchronized (this) {
                for (int i = 0; i < 3; i++) {
                    Color sillaCor = (i < queue.size())
                            ? new Color(255, 220, 100) : new Color(230, 230, 230);

                    g2.setColor(sillaCor);
                    g2.fillRoundRect(sx + i * gap, sy, 60, 40, 12, 12);
                    g2.setColor(Color.GRAY);
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawRoundRect(sx + i * gap, sy, 60, 40, 12, 12);

                    g2.setColor(new Color(80, 50, 20));
                    g2.fillRect(sx + i * gap + 5, sy + 40, 6, 14);
                    g2.fillRect(sx + i * gap + 49, sy + 40, 6, 14);
                }

                int i = 0;
                for (Integer id : queue) {
                    if (i >= 3) break;
                    DrawUtil.person(g2, sx + i * gap + 30, sy - 10, new Color(200, 220, 255));
                    g2.setColor(Color.BLACK);
                    g2.setFont(g2.getFont().deriveFont(Font.BOLD, 10f));
                    g2.drawString("#" + id, sx + i * gap + 22, sy + 56);
                    i++;
                }
            }

            int bx = 420, by = 140;

            g2.setColor(new Color(100, 100, 100));
            g2.fillRect(bx + 16, by + 50, 24, 30);
            g2.setColor(new Color(150, 50, 50));
            g2.fillRect(bx, by + 30, 60, 20);
            g2.fillRect(bx + 8, by, 44, 40);
            g2.setColor(Color.DARK_GRAY);
            g2.drawRect(bx, by, 60, 80);

            dibujarBarbero(g2, bx + 30, by - 40, barberState);

            int act;
            synchronized (this) {
                act = sillaAct;
            }
            if (act > 0) {
                DrawUtil.person(g2, bx + 30, by + 10, new Color(200, 210, 255));
                g2.setColor(Color.BLACK);
                g2.setFont(g2.getFont().deriveFont(Font.BOLD, 10f));
                g2.drawString("#" + act, bx + 22, by + 94);
            }

            g2.setColor(Color.BLUE.darker());
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14f));
            g2.drawString("Sala de Espera", sx, sy - 20);
            g2.drawString("Barbero: " + barberState, bx - 20, by - 60);

            g2.dispose();
        }

        private void dibujarBarbero(Graphics2D g2, int x, int y, String state) {
            g2.setColor(Color.WHITE);
            g2.fillRect(x - 12, y, 24, 36);

            g2.setColor(new Color(255, 220, 180));
            g2.fillOval(x - 12, y - 24, 24, 24);

            g2.setColor(new Color(80, 50, 20));
            g2.fillArc(x - 12, y - 26, 24, 18, 0, 180);

            if (state.equals("DURMIENDO")) {
                g2.setColor(Color.BLACK);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine(x - 6, y - 14, x - 2, y - 14);
                g2.drawLine(x + 2, y - 14, x + 6, y - 14);

                g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14f));
                float alpha = (float) (Math.sin(zzzOffset * 0.2) * 0.3 + 0.7);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g2.drawString("Z", x + 16, y - 30 + (zzzOffset % 8));
                g2.drawString("Z", x + 22, y - 36 + (zzzOffset % 12));
                g2.drawString("Z", x + 28, y - 42 + (zzzOffset % 16));
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            } else {
                g2.setColor(Color.WHITE);
                g2.fillOval(x - 8, y - 16, 6, 6);
                g2.fillOval(x + 2, y - 16, 6, 6);
                g2.setColor(Color.BLACK);
                g2.fillOval(x - 6, y - 14, 3, 3);
                g2.fillOval(x + 4, y - 14, 3, 3);

                if (state.equals("CORTANDO")) {
                    dibujarTijeras(g2, x + 20, y - 4, scissorsAnim);
                }
            }

            g2.setColor(new Color(80, 50, 20));
            g2.fillArc(x - 6, y - 10, 5, 4, 0, 180);
            g2.fillArc(x + 1, y - 10, 5, 4, 0, 180);

            g2.setColor(new Color(255, 220, 180));
            g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x - 12, y + 12, x - 20, y + 24);
            g2.drawLine(x + 12, y + 12, x + 20, y + 24);

            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(6f));
            g2.drawLine(x - 4, y + 36, x - 4, y + 54);
            g2.drawLine(x + 4, y + 36, x + 4, y + 54);
        }

        private void dibujarTijeras(Graphics2D g2, int x, int y, int anim) {
            g2.setColor(new Color(180, 180, 180));
            g2.setStroke(new BasicStroke(1.5f));
            int apertura = (int) (Math.sin(anim * 0.3) * 6);

            int[] x1 = {x, x - 3 - apertura, x - 6 - apertura};
            int[] y1 = {y, y - 12, y - 10};
            g2.fillPolygon(x1, y1, 3);

            int[] x2 = {x, x + 3 + apertura, x + 6 + apertura};
            int[] y2 = {y, y - 12, y - 10};
            g2.fillPolygon(x2, y2, 3);

            g2.setColor(Color.DARK_GRAY);
            g2.fillOval(x - 2, y - 2, 4, 4);
        }
    }
}