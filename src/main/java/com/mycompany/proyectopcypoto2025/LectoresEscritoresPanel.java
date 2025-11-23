package com.mycompany.proyectopcypoto2025;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

public class LectoresEscritoresPanel extends JPanel implements Reseteable, Demoable, SyncAware {
    private final Biblioteca biblioteca = new Biblioteca();
    private SyncMode modo = SyncMode.SEMAFOROS;
    private volatile boolean running = false;
    private Thread lector;
    private Thread escritor;

    public LectoresEscritoresPanel() {
        setLayout(new BorderLayout());
        add(biblioteca, BorderLayout.CENTER);
        start();
    }

    private void start() {
        if (running) return;
        running = true;

        // Solo 1 lector
        lector = new Thread(() -> cicloLector(0), "Lector0");
        lector.start();

        // Solo 1 escritor
        escritor = new Thread(() -> cicloEscritor(0), "Escritor0");
        escritor.start();
    }

    private void cicloLector(int id) {
        while (running) {
            biblioteca.setEstadoLector(id, Biblioteca.EstadoLector.ESPERANDO);
            dormir(200);

            try {
                biblioteca.setEstadoLector(id, Biblioteca.EstadoLector.LEYENDO);
                dormir(300);
            } catch (Exception e) {
            } finally {
                biblioteca.setEstadoLector(id, Biblioteca.EstadoLector.INACTIVO);
                dormir(150);
            }
        }
    }

    private void cicloEscritor(int id) {
        while (running) {
            biblioteca.setEstadoEscritor(id, Biblioteca.EstadoEscritor.ESPERANDO);
            dormir(250);

            try {
                biblioteca.setEstadoEscritor(id, Biblioteca.EstadoEscritor.ESCRIBIENDO);
                dormir(400);
            } catch (Exception e) {
            } finally {
                biblioteca.setEstadoEscritor(id, Biblioteca.EstadoEscritor.INACTIVO);
                dormir(200);
            }
        }
    }

    @Override
    public void setSyncMode(SyncMode m) {
        this.modo = m;
    }

    @Override
    public void reset() {
        running = false;
        biblioteca.reset();
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

    static class Biblioteca extends JPanel {
        enum EstadoLector { INACTIVO, ESPERANDO, LEYENDO }
        enum EstadoEscritor { INACTIVO, ESPERANDO, ESCRIBIENDO }

        private EstadoLector estadoLector = EstadoLector.INACTIVO;
        private EstadoEscritor estadoEscritor = EstadoEscritor.INACTIVO;
        private int lectoresActivos = 0;
        private boolean escritorActivo = false;
        private Timer animTimer;
        private int frameCount = 0;
        private List<String> textosEnPizarra = new ArrayList<>();
        private float tizaX = 0;
        private float tizaY = 0;

        public Biblioteca() {
            setPreferredSize(new Dimension(700, 400));
            setBackground(new Color(245, 240, 230));
            
            // Textos de ejemplo en la pizarra
            textosEnPizarra.add("Semáforos");
            textosEnPizarra.add("Monitores");
            textosEnPizarra.add("Mutex");

            animTimer = new Timer(50, e -> {
                frameCount++;
                repaint();
            });
            animTimer.start();
        }

        public synchronized void setEstadoLector(int id, EstadoLector estado) {
            estadoLector = estado;
            if (estado == EstadoLector.LEYENDO) {
                lectoresActivos = 1;
            } else if (estado == EstadoLector.INACTIVO) {
                lectoresActivos = 0;
            }
        }

        public synchronized void setEstadoEscritor(int id, EstadoEscritor estado) {
            estadoEscritor = estado;
            escritorActivo = (estado == EstadoEscritor.ESCRIBIENDO);
        }

        public synchronized void reset() {
            estadoLector = EstadoLector.INACTIVO;
            estadoEscritor = EstadoEscritor.INACTIVO;
            lectoresActivos = 0;
            escritorActivo = false;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int W = getWidth();
            int H = getHeight();

            // Título
            g2.setFont(new Font("Arial", Font.BOLD, 18));
            g2.setColor(new Color(60, 60, 80));
            String titulo = "Lectores: " + lectoresActivos + " | Escritor: " + (escritorActivo ? "Activo" : "Inactivo");
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(titulo, (W - fm.stringWidth(titulo)) / 2, 30);

            // Pizarra en el centro
            dibujarPizarra(g2, W / 2 - 150, H / 2 - 120, 300, 240);

            // Lector a la izquierda
            dibujarLector(g2, 100, H / 2, estadoLector);

            // Escritor a la derecha
            dibujarEscritor(g2, W - 100, H / 2, estadoEscritor);

            // Líneas de conexión
            dibujarConexiones(g2, W, H);

            g2.dispose();
        }

        private void dibujarPizarra(Graphics2D g2, int x, int y, int w, int h) {
            // Marco de madera
            g2.setColor(new Color(101, 67, 33));
            g2.fillRect(x - 15, y - 15, w + 30, h + 30);
            
            // Sombra interior del marco
            g2.setColor(new Color(80, 50, 20));
            g2.fillRect(x - 12, y - 12, w + 24, 8);
            g2.fillRect(x - 12, y - 12, 8, h + 24);

            // Superficie de la pizarra (verde oscuro)
            GradientPaint pizarraGrad = new GradientPaint(
                x, y, new Color(40, 70, 50),
                x + w, y + h, new Color(30, 60, 40)
            );
            g2.setPaint(pizarraGrad);
            g2.fillRect(x, y, w, h);

            // Textura de pizarra (marcas de tiza viejas)
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f));
            g2.setColor(Color.WHITE);
            for (int i = 0; i < 30; i++) {
                int tx = x + 20 + (i * 23) % (w - 40);
                int ty = y + 30 + (i * 17) % (h - 60);
                g2.drawLine(tx, ty, tx + 10, ty);
            }
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

            // Contenido de la pizarra
            if (lectoresActivos > 0 || escritorActivo) {
                g2.setFont(new Font("Comic Sans MS", Font.BOLD, 16));
                g2.setColor(Color.WHITE);
                
                int textY = y + 40;
                for (String texto : textosEnPizarra) {
                    g2.drawString(texto, x + 30, textY);
                    textY += 30;
                }

                // Si hay escritor activo, dibujar mano escribiendo
                if (escritorActivo) {
                    tizaX = x + 30 + (frameCount * 2) % (w - 60);
                    tizaY = textY;
                    
                    // Tiza
                    g2.setColor(Color.WHITE);
                    g2.fillRect((int)tizaX, (int)tizaY - 5, 15, 8);
                    g2.setColor(new Color(255, 200, 100));
                    g2.fillRect((int)tizaX, (int)tizaY - 5, 3, 8);
                    
                    // Línea que está escribiendo
                    g2.setStroke(new BasicStroke(2f));
                    g2.setColor(new Color(255, 255, 255, 200));
                    g2.drawLine(x + 30, (int)tizaY, (int)tizaX, (int)tizaY);
                    
                    // Polvo de tiza
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
                    for (int i = 0; i < 5; i++) {
                        float px = tizaX + (float)(Math.random() * 10 - 5);
                        float py = tizaY + (float)(Math.random() * 10 - 5);
                        g2.fillOval((int)px, (int)py, 2, 2);
                    }
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
                }
            }

            // Bandeja para tizas
            g2.setColor(new Color(101, 67, 33));
            g2.fillRect(x, y + h, w, 12);
            
            // Tizas en la bandeja
            int[] tizaColors = {0xFFFFFF, 0xFFFF00, 0xFF6B6B, 0x4ECDC4, 0xFF8F5F};
            for (int i = 0; i < 5; i++) {
                g2.setColor(new Color(tizaColors[i]));
                g2.fillRoundRect(x + 20 + i * 50, y + h + 2, 30, 8, 4, 4);
            }

            // Borrador
            g2.setColor(new Color(80, 50, 50));
            g2.fillRoundRect(x + w - 50, y + h + 1, 40, 10, 3, 3);
            g2.setColor(new Color(200, 180, 160));
            g2.fillRoundRect(x + w - 48, y + h + 3, 36, 6, 2, 2);

            // Indicador de estado en esquina
            String estadoTexto;
            Color estadoColor;
            if (escritorActivo) {
                estadoTexto = "ESCRIBIENDO";
                estadoColor = new Color(255, 100, 100);
            } else if (lectoresActivos > 0) {
                estadoTexto = "LEYENDO";
                estadoColor = new Color(100, 255, 100);
            } else {
                estadoTexto = "LIBRE";
                estadoColor = new Color(200, 200, 200);
            }
            
            g2.setFont(new Font("Arial", Font.BOLD, 11));
            g2.setColor(estadoColor);
            g2.drawString(estadoTexto, x + 10, y + h - 10);
        }

        private void dibujarLector(Graphics2D g2, int x, int y, EstadoLector estado) {
            Color color = switch (estado) {
                case LEYENDO -> new Color(100, 200, 100);
                case ESPERANDO -> new Color(255, 200, 100);
                default -> new Color(200, 200, 200);
            };

            // Cuerpo
            g2.setColor(color);
            g2.fillRect(x - 15, y - 5, 30, 40);

            // Cabeza
            g2.setColor(new Color(255, 220, 180));
            g2.fillOval(x - 14, y - 26, 28, 28);

            // Cabello
            g2.setColor(new Color(80, 50, 20));
            g2.fillArc(x - 14, y - 28, 28, 20, 0, 180);

            // Ojos
            g2.setColor(Color.BLACK);
            if (estado == EstadoLector.LEYENDO) {
                // Ojos mirando hacia la pizarra
                g2.fillOval(x - 8, y - 18, 5, 5);
                g2.fillOval(x + 3, y - 18, 5, 5);
            } else {
                g2.fillOval(x - 8, y - 18, 4, 4);
                g2.fillOval(x + 4, y - 18, 4, 4);
            }

            // Boca
            if (estado == EstadoLector.LEYENDO) {
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawArc(x - 6, y - 12, 12, 8, 0, -180);
            } else {
                g2.drawLine(x - 4, y - 10, x + 4, y - 10);
            }

            // Libro si está leyendo
            if (estado == EstadoLector.LEYENDO) {
                g2.setColor(new Color(100, 150, 200));
                g2.fillRect(x - 8, y + 10, 16, 20);
                g2.setColor(Color.WHITE);
                g2.fillRect(x - 6, y + 12, 12, 16);
                g2.setColor(Color.DARK_GRAY);
                g2.setStroke(new BasicStroke(1f));
                g2.drawLine(x, y + 12, x, y + 28);
            }

            // Brazos
            g2.setColor(new Color(255, 220, 180));
            g2.setStroke(new BasicStroke(4f));
            if (estado == EstadoLector.LEYENDO) {
                // Brazos sosteniendo libro
                g2.drawLine(x - 15, y + 5, x - 10, y + 15);
                g2.drawLine(x + 15, y + 5, x + 10, y + 15);
            } else {
                g2.drawLine(x - 15, y + 5, x - 20, y + 20);
                g2.drawLine(x + 15, y + 5, x + 20, y + 20);
            }

            // Piernas
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(5f));
            g2.drawLine(x - 5, y + 35, x - 5, y + 55);
            g2.drawLine(x + 5, y + 35, x + 5, y + 55);

            // Etiqueta
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Arial", Font.BOLD, 12));
            g2.drawString("L0", x - 8, y + 70);
            
            g2.setFont(new Font("Arial", Font.PLAIN, 9));
            g2.setColor(new Color(100, 100, 100));
            String estadoStr = switch(estado) {
                case LEYENDO -> "LEY";
                case ESPERANDO -> "ESP";
                default -> "---";
            };
            g2.drawString(estadoStr, x - 10, y + 82);
        }

        private void dibujarEscritor(Graphics2D g2, int x, int y, EstadoEscritor estado) {
            Color color = switch (estado) {
                case ESCRIBIENDO -> new Color(200, 100, 100);
                case ESPERANDO -> new Color(255, 200, 100);
                default -> new Color(200, 200, 200);
            };

            // Cuerpo
            g2.setColor(color);
            g2.fillRect(x - 15, y - 5, 30, 40);

            // Cabeza
            g2.setColor(new Color(255, 220, 180));
            g2.fillOval(x - 14, y - 26, 28, 28);

            // Cabello
            g2.setColor(new Color(60, 40, 20));
            g2.fillArc(x - 14, y - 28, 28, 20, 0, 180);

            // Ojos
            g2.setColor(Color.BLACK);
            if (estado == EstadoEscritor.ESCRIBIENDO) {
                // Ojos concentrados
                g2.fillOval(x - 8, y - 18, 5, 5);
                g2.fillOval(x + 3, y - 18, 5, 5);
                // Cejas fruncidas
                g2.setStroke(new BasicStroke(2f));
                g2.drawLine(x - 10, y - 22, x - 5, y - 20);
                g2.drawLine(x + 5, y - 20, x + 10, y - 22);
            } else {
                g2.fillOval(x - 8, y - 18, 4, 4);
                g2.fillOval(x + 4, y - 18, 4, 4);
            }

            // Boca
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawLine(x - 4, y - 10, x + 4, y - 10);

            // Tiza en la mano si está escribiendo
            if (estado == EstadoEscritor.ESCRIBIENDO) {
                g2.setColor(Color.WHITE);
                g2.fillRect(x + 18, y + 8, 15, 6);
                g2.setColor(new Color(255, 200, 100));
                g2.fillRect(x + 18, y + 8, 3, 6);
            }

            // Brazos
            g2.setColor(new Color(255, 220, 180));
            g2.setStroke(new BasicStroke(4f));
            if (estado == EstadoEscritor.ESCRIBIENDO) {
                // Brazo derecho extendido escribiendo
                float wave = (float) Math.sin(frameCount * 0.1) * 3;
                g2.drawLine(x + 15, y + 5, x + 25, (int)(y + 10 + wave));
                g2.drawLine(x - 15, y + 5, x - 20, y + 15);
            } else {
                g2.drawLine(x - 15, y + 5, x - 20, y + 20);
                g2.drawLine(x + 15, y + 5, x + 20, y + 20);
            }

            // Piernas
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(5f));
            g2.drawLine(x - 5, y + 35, x - 5, y + 55);
            g2.drawLine(x + 5, y + 35, x + 5, y + 55);

            // Etiqueta
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Arial", Font.BOLD, 12));
            g2.drawString("E0", x - 8, y + 70);
            
            g2.setFont(new Font("Arial", Font.PLAIN, 9));
            g2.setColor(new Color(100, 100, 100));
            String estadoStr = switch(estado) {
                case ESCRIBIENDO -> "ESC";
                case ESPERANDO -> "ESP";
                default -> "---";
            };
            g2.drawString(estadoStr, x - 10, y + 82);
        }

        private void dibujarConexiones(Graphics2D g2, int W, int H) {
            int pizarraX = W / 2;
            int pizarraY = H / 2;
            
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            
            // Línea del lector
            if (lectoresActivos > 0) {
                g2.setColor(new Color(100, 200, 100, 150));
                int[] xPoints = {100, pizarraX - 160, pizarraX - 150};
                int[] yPoints = {H / 2, H / 2 - 10, pizarraY};
                g2.drawPolyline(xPoints, yPoints, 3);
                
                // Flecha
                dibujarFlecha(g2, pizarraX - 160, H / 2 - 10, pizarraX - 150, pizarraY);
            } else {
                g2.setColor(new Color(150, 150, 150, 100));
                g2.drawLine(100, H / 2, pizarraX - 150, pizarraY);
            }
            
            // Línea del escritor
            if (escritorActivo) {
                g2.setColor(new Color(200, 100, 100, 150));
                int[] xPoints = {W - 100, pizarraX + 160, pizarraX + 150};
                int[] yPoints = {H / 2, H / 2 - 10, pizarraY};
                g2.drawPolyline(xPoints, yPoints, 3);
                
                // Flecha
                dibujarFlecha(g2, pizarraX + 160, H / 2 - 10, pizarraX + 150, pizarraY);
            } else {
                g2.setColor(new Color(150, 150, 150, 100));
                g2.drawLine(W - 100, H / 2, pizarraX + 150, pizarraY);
            }
        }
        
        private void dibujarFlecha(Graphics2D g2, int x1, int y1, int x2, int y2) {
            double angle = Math.atan2(y2 - y1, x2 - x1);
            int arrowSize = 10;
            
            int[] xPoints = {
                x2,
                (int)(x2 - arrowSize * Math.cos(angle - Math.PI / 6)),
                (int)(x2 - arrowSize * Math.cos(angle + Math.PI / 6))
            };
            int[] yPoints = {
                y2,
                (int)(y2 - arrowSize * Math.sin(angle - Math.PI / 6)),
                (int)(y2 - arrowSize * Math.sin(angle + Math.PI / 6))
            };
            
            g2.fillPolygon(xPoints, yPoints, 3);
        }
    }
}