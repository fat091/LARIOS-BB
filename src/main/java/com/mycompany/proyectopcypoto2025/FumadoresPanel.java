package com.mycompany.proyectopcypoto2025;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

public class FumadoresPanel extends JPanel implements Reseteable, Demoable, SyncAware {
    private final Mesa mesa = new Mesa();
    private volatile boolean running = false;
    private Thread agente, ciclo;
    private SyncMode modo = SyncMode.SEMAFOROS;

    public FumadoresPanel() {
        setLayout(new BorderLayout());
        add(mesa, BorderLayout.CENTER);
        start();
    }

    private void start() {
        if (running) return;
        running = true;

        agente = new Thread(() -> {
            while (running) {
                int a = (int) (Math.random() * 3);
                int b = (a + 1 + (int) (Math.random() * 2)) % 3;
                mesa.setRecursos(a, b);
                dormir(220);
            }
        }, "Agente");

        ciclo = new Thread(() -> {
            while (running) {
                int fum = mesa.smokerWith(mesa.a, mesa.b);
                mesa.fumando = fum;
                mesa.t++;
                dormir(260);
                mesa.fumando = -1;
            }
        }, "Fumadores");

        agente.start();
        ciclo.start();
    }

    @Override
    public void setSyncMode(SyncMode m) {
        this.modo = m;
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
        int a = -1, b = -1;
        int fumando = -1;
        int t = 0;
        private Timer animTimer;

        public Mesa() {
            setPreferredSize(new Dimension(560, 260));
            setBackground(Color.white);

            animTimer = new Timer(33, e -> {
                t++;
                repaint();
            });
            animTimer.start();
        }

        public synchronized void setRecursos(int a, int b) {
            this.a = a;
            this.b = b;
        }

        public synchronized void reset() {
            a = b = -1;
            fumando = -1;
        }

        public int smokerWith(int a, int b) {
            if (a == -1 || b == -1) return -1;
            int falta = 3 - a - b;
            return falta;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(new Color(139, 69, 19));
            g2.fillRect(160, 50, 240, 10);
            g2.fillRect(168, 60, 8, 30);
            g2.fillRect(384, 60, 8, 30);

            DrawUtil.person(g2, 280, 20, new Color(100, 100, 200));
            g2.setColor(Color.BLACK);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 11f));
            g2.drawString("Agente", 258, 65);

            g2.setColor(new Color(240, 240, 240));
            g2.fillRoundRect(200, 80, 160, 80, 16, 16);
            g2.setColor(Color.GRAY);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(200, 80, 160, 80, 16, 16);

            g2.setColor(Color.BLACK);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 10f));
            g2.drawString("Ingredientes:", 210, 96);

            String[] names = {"Papel", "Tabaco", "Cerillos"};
            Color[] colors = {
                new Color(255, 240, 200),
                new Color(160, 100, 60),
                new Color(255, 100, 100)
            };

            for (int i = 0; i < 3; i++) {
                int rx = 220, ry = 100 + i * 18;
                boolean presente = (i == a || i == b);

                Color baseColor = presente
                        ? new Color(120, 220, 160) : new Color(230, 230, 230);

                g2.setColor(baseColor);
                g2.fillRoundRect(rx, ry, 120, 14, 8, 8);
                g2.setColor(Color.DARK_GRAY);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(rx, ry, 120, 14, 8, 8);

                g2.setColor(Color.BLACK);
                g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 10f));
                g2.drawString(names[i], rx + 6, ry + 11);

                dibujarIconoIngrediente(g2, rx + 100, ry + 7, i, presente);

                if (presente) {
                    g2.setColor(new Color(255, 255, 150, 80));
                    g2.fillRoundRect(rx - 2, ry - 2, 124, 18, 8, 8);
                }
            }

            Color[] fumColores = {
                new Color(255, 150, 150),
                new Color(150, 255, 150),
                new Color(150, 150, 255)
            };

            for (int i = 0; i < 3; i++) {
                int x = 140 + i * 140, y = 190;
                Color c = (i == fumando)
                        ? new Color(120, 220, 160) : fumColores[i];

                dibujarFumador(g2, x, y, c, i == fumando);

                g2.setColor(Color.BLACK);
                g2.setFont(g2.getFont().deriveFont(Font.BOLD, 11f));
                g2.drawString("F" + i, x - 6, y + 50);

                g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 9f));
                g2.drawString(names[i], x - 18, y + 62);

                if (i == fumando) {
                    g2.setColor(new Color(100, 100, 100, 160));
                    DrawUtil.smoke(g2, x + 16, y - 18, t);

                    dibujarBrasaCigarro(g2, x + 20, y - 10, t);
                }
            }

            g2.dispose();
        }

        private void dibujarIconoIngrediente(Graphics2D g2, int x, int y, int tipo, boolean activo) {
            g2.setColor(activo ? Color.BLACK : new Color(180, 180, 180));
            int size = 6;

            switch (tipo) {
                case 0:
                    g2.fillRect(x - size / 2, y - size / 2, size, size + 2);
                    g2.setColor(activo ? Color.WHITE : new Color(200, 200, 200));
                    for (int i = 0; i < 2; i++) {
                        g2.drawLine(x - size / 2 + 1, y - size / 2 + 2 + i * 2,
                                x + size / 2 - 1, y - size / 2 + 2 + i * 2);
                    }
                    break;
                case 1:
                    g2.fillOval(x - size / 2, y - size / 2, size, size);
                    g2.setColor(activo ? new Color(101, 67, 33) : new Color(180, 180, 180));
                    g2.fillOval(x - size / 2 + 1, y - size / 2 + 1, size - 2, size - 2);
                    break;
                case 2:
                    g2.fillRect(x - 1, y - size / 2, 2, size + 2);
                    g2.setColor(activo ? Color.RED : new Color(180, 180, 180));
                    g2.fillOval(x - 2, y - size / 2 - 2, 4, 4);
                    break;
            }
        }

        private void dibujarFumador(Graphics2D g2, int x, int y, Color c, boolean fumando) {
            g2.setColor(c);
            g2.fillRect(x - 12, y - 4, 24, 28);

            g2.setColor(new Color(255, 220, 180));
            g2.fillOval(x - 14, y - 24, 28, 28);

            g2.setColor(Color.BLACK);
            g2.fillOval(x - 8, y - 16, 5, 5);
            g2.fillOval(x + 3, y - 16, 5, 5);

            if (fumando) {
                g2.fillOval(x - 4, y - 6, 8, 6);

                g2.setColor(Color.WHITE);
                g2.fillRect(x + 8, y - 8, 12, 2);
                g2.setColor(Color.ORANGE);
                g2.fillOval(x + 19, y - 9, 3, 3);
            } else {
                g2.drawArc(x - 6, y - 8, 12, 8, 0, -180);
            }

            g2.setColor(c.darker());
            g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x - 12, y + 4, x - 18, y + 14);
            g2.drawLine(x + 12, y + 4, x + 18, y + 14);
        }

        private void dibujarBrasaCigarro(Graphics2D g2, int x, int y, int t) {
            float alpha = (float) (Math.sin(t * 0.15) * 0.3 + 0.6);
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

            g2.setColor(Color.ORANGE);
            g2.fillOval(x - 3, y - 3, 6, 6);
            g2.setColor(Color.RED);
            g2.fillOval(x - 2, y - 2, 4, 4);
            g2.setColor(Color.YELLOW);
            g2.fillOval(x - 1, y - 1, 2, 2);

            g2.setComposite(old);
        }
    }
}