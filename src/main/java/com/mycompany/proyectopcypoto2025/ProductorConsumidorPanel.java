package com.mycompany.proyectopcypoto2025;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

public class ProductorConsumidorPanel extends JPanel implements Reseteable, Demoable, SyncAware {
    private final Tanque tanque = new Tanque();
    private SyncMode modo = SyncMode.SEMAFOROS;
    private Thread prod, cons;
    private volatile boolean running = false;

    public ProductorConsumidorPanel() {
        setLayout(new BorderLayout());
        add(tanque, BorderLayout.CENTER);
        autoStart();
    }

    private void autoStart() {
        if (running) return;
        running = true;

        prod = new Thread(() -> {
            int v = 0;
            while (running) {
                tanque.setProduciendo(true);
                dormir(180);
                tanque.addNivel();
                tanque.setProduciendo(false);
                tanque.animarProduccion();
                if (++v > 2000) v = 0;
            }
        }, "PC-P");

        cons = new Thread(() -> {
            int v = 0;
            while (running) {
                tanque.setConsumiendo(true);
                dormir(260);
                tanque.remNivel();
                tanque.setConsumiendo(false);
                tanque.animarConsumo();
                if (++v > 2000) v = 0;
            }
        }, "PC-C");

        prod.start();
        cons.start();
    }

    @Override
    public void setSyncMode(SyncMode m) {
        this.modo = m;
    }

    @Override
    public void reset() {
        running = false;
        tanque.setNivel(0);
        autoStart();
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

    static class Tanque extends JPanel {
        private int nivel = 0;
        private boolean produciendo = false;
        private boolean consumiendo = false;
        private int anguloProductor = 0;
        private int anguloConsumidor = 0;
        private float alphaProduccion = 0.0f;
        private float alphaConsumo = 0.0f;
        private Timer animTimer;

        public Tanque() {
            setPreferredSize(new Dimension(560, 280));
            setBackground(Color.white);

            animTimer = new Timer(50, e -> {
                anguloProductor = (anguloProductor + 5) % 360;
                anguloConsumidor = (anguloConsumidor + 3) % 360;
                if (alphaProduccion > 0) alphaProduccion -= 0.05f;
                if (alphaConsumo > 0) alphaConsumo -= 0.05f;
                repaint();
            });
            animTimer.start();
        }

        public synchronized void addNivel() {
            if (nivel < 20) nivel++;
        }

        public synchronized void remNivel() {
            if (nivel > 0) nivel--;
        }

        public synchronized void setNivel(int n) {
            nivel = Math.max(0, Math.min(20, n));
        }

        public synchronized void setProduciendo(boolean p) {
            produciendo = p;
        }

        public synchronized void setConsumiendo(boolean c) {
            consumiendo = c;
        }

        public void animarProduccion() {
            alphaProduccion = 1.0f;
        }

        public void animarConsumo() {
            alphaConsumo = 1.0f;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int W = 180, H = 220, x = getWidth() / 2 - W / 2, y = 30;

            dibujarPersonaAnimada(g2, 60, 100, anguloProductor, new Color(0, 100, 200), "Productor");
            dibujarTanqueConEfectos(g2, x, y, W, H);
            dibujarPersonaAnimada(g2, 480, 100, anguloConsumidor, new Color(200, 50, 50), "Consumidor");

            if (alphaProduccion > 0) {
                dibujarParticulas(g2, x - 40, y + H / 2, alphaProduccion, Color.GREEN);
            }

            if (alphaConsumo > 0) {
                dibujarParticulas(g2, x + W + 20, y + H / 2, alphaConsumo, Color.ORANGE);
            }

            g2.dispose();
        }

        private void dibujarPersonaAnimada(Graphics2D g2, int x, int y, int angulo, Color color, String label) {
            g2.setColor(new Color(255, 220, 180));
            g2.fillOval(x - 12, y - 28, 24, 24);

            g2.setColor(Color.BLACK);
            g2.fillOval(x - 6, y - 20, 4, 4);
            g2.fillOval(x + 2, y - 20, 4, 4);

            g2.drawArc(x - 6, y - 16, 12, 8, 0, -180);

            g2.setColor(color);
            g2.fillRect(x - 8, y - 4, 16, 24);

            double angRad = Math.toRadians(angulo);
            int brazoX = (int) (Math.cos(angRad) * 10);
            int brazoY = (int) (Math.sin(angRad) * 6);
            g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x - 8, y + 4, x - 16 + brazoX, y + 10 + brazoY);
            g2.drawLine(x + 8, y + 4, x + 16 - brazoX, y + 10 - brazoY);

            g2.drawLine(x - 4, y + 20, x - 6, y + 36);
            g2.drawLine(x + 4, y + 20, x + 6, y + 36);

            g2.setColor(Color.BLACK);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 11f));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(label, x - fm.stringWidth(label) / 2, y + 50);
        }

        private void dibujarTanqueConEfectos(Graphics2D g2, int x, int y, int W, int H) {
            g2.setColor(new Color(200, 200, 200));
            g2.fillRoundRect(x - 6, y - 6, W + 12, H + 12, 20, 20);

            g2.setPaint(new GradientPaint(x, y, new Color(210, 210, 220),
                    x + W, y + H, new Color(180, 190, 210)));
            g2.fillRoundRect(x, y, W, H, 16, 16);

            g2.setColor(new Color(70, 70, 80));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(x, y, W, H, 16, 16);

            int nl;
            synchronized (this) {
                nl = this.nivel;
            }

            if (nl > 0) {
                int waterH = nl * H / 20;

                GradientPaint water = new GradientPaint(x, y + H - waterH,
                        new Color(140, 200, 255),
                        x, y + H, new Color(20, 120, 220));
                g2.setPaint(water);

                GeneralPath waveShape = new GeneralPath();
                waveShape.moveTo(x + 4, y + H - waterH);
                for (int i = 0; i <= W - 8; i += 8) {
                    double waveY = y + H - waterH + Math.sin((i + anguloProductor) * 0.15) * 2;
                    waveShape.lineTo(x + 4 + i, waveY);
                }
                waveShape.lineTo(x + W - 4, y + H - 4);
                waveShape.lineTo(x + 4, y + H - 4);
                waveShape.closePath();

                Shape clip = new RoundRectangle2D.Float(x + 4, y + 4, W - 8, H - 8, 12, 12);
                Shape oldClip = g2.getClip();
                g2.setClip(clip);
                g2.fill(waveShape);
                g2.setClip(oldClip);
            }

            g2.setColor(new Color(100, 100, 100));
            for (int i = 0; i <= 20; i += 5) {
                int markY = y + H - (i * H / 20);
                g2.drawLine(x - 4, markY, x, markY);
                if (i % 5 == 0) {
                    g2.setFont(g2.getFont().deriveFont(9f));
                    g2.drawString(String.valueOf(i), x - 20, markY + 3);
                }
            }

            g2.setColor(Color.BLACK);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 12f));
            String label = "Nivel: " + nl + "/20";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(label, x + (W - fm.stringWidth(label)) / 2, y + H + 20);
        }

        private void dibujarParticulas(Graphics2D g2, int x, int y, float alpha, Color color) {
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

            for (int i = 0; i < 8; i++) {
                int px = x + (int) (Math.random() * 40 - 20);
                int py = y + (int) (Math.random() * 40 - 20);
                int size = (int) (Math.random() * 6 + 2);
                g2.setColor(color);
                g2.fillOval(px, py, size, size);
            }
            g2.setComposite(old);
        }
    }
}