package com.mycompany.proyectopcypoto2025;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

public class LectoresEscritoresPanel extends JPanel implements Reseteable, Demoable, SyncAware {
    private final Biblioteca biblioteca = new Biblioteca();
    private SyncMode modo = SyncMode.SEMAFOROS;
    private volatile boolean running = false;
    private Thread[] lectores;
    private Thread[] escritores;

    public LectoresEscritoresPanel() {
        setLayout(new BorderLayout());
        add(biblioteca, BorderLayout.CENTER);
        start();
    }

    private void start() {
        if (running) return;
        running = true;

        lectores = new Thread[2];
        escritores = new Thread[1];

        for (int i = 0; i < lectores.length; i++) {
            final int id = i;
            lectores[i] = new Thread(() -> cicloLector(id), "Lector" + id);
            lectores[i].start();
        }

        for (int i = 0; i < escritores.length; i++) {
            final int id = i;
            escritores[i] = new Thread(() -> cicloEscritor(id), "Escritor" + id);
            escritores[i].start();
        }
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

        private final EstadoLector[] estadoLectores = new EstadoLector[2];
        private final EstadoEscritor[] estadoEscritores = new EstadoEscritor[1];
        private int lectoresActivos = 0;
        private boolean escritorActivo = false;
        private Timer animTimer;

        public Biblioteca() {
            setPreferredSize(new Dimension(560, 300));
            setBackground(Color.white);

            for (int i = 0; i < estadoLectores.length; i++) {
                estadoLectores[i] = EstadoLector.INACTIVO;
            }
            for (int i = 0; i < estadoEscritores.length; i++) {
                estadoEscritores[i] = EstadoEscritor.INACTIVO;
            }

            animTimer = new Timer(100, e -> repaint());
            animTimer.start();
        }

        public synchronized void setEstadoLector(int id, EstadoLector estado) {
            if (id < estadoLectores.length) {
                estadoLectores[id] = estado;
                if (estado == EstadoLector.LEYENDO) {
                    lectoresActivos++;
                } else if (estado == EstadoLector.INACTIVO) {
                    lectoresActivos = Math.max(0, lectoresActivos - 1);
                }
            }
        }

        public synchronized void setEstadoEscritor(int id, EstadoEscritor estado) {
            if (id < estadoEscritores.length) {
                estadoEscritores[id] = estado;
                escritorActivo = (estado == EstadoEscritor.ESCRIBIENDO);
            }
        }

        public synchronized void reset() {
            for (int i = 0; i < estadoLectores.length; i++) {
                estadoLectores[i] = EstadoLector.INACTIVO;
            }
            for (int i = 0; i < estadoEscritores.length; i++) {
                estadoEscritores[i] = EstadoEscritor.INACTIVO;
            }
            lectoresActivos = 0;
            escritorActivo = false;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Fondo de biblioteca
            g2.setColor(new Color(240, 240, 220));
            g2.fillRect(0, 0, getWidth(), getHeight());

            // Base de datos central
            dibujarBaseDatos(g2, getWidth() / 2, getHeight() / 2 - 20);

            // Lectores (izquierda)
            for (int i = 0; i < estadoLectores.length; i++) {
                dibujarLector(g2, 100, 80 + i * 100, i, estadoLectores[i]);
            }

            // Escritores (derecha)
            for (int i = 0; i < estadoEscritores.length; i++) {
                dibujarEscritor(g2, getWidth() - 100, 150 + i * 100, i, estadoEscritores[i]);
            }

            // Información del estado
            g2.setColor(Color.BLACK);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14f));
            g2.drawString("Lectores Activos: " + lectoresActivos, getWidth() / 2 - 80, 30);
            g2.drawString("Escritor Activo: " + (escritorActivo ? "Sí" : "No"), getWidth() / 2 - 80, 50);

            g2.dispose();
        }

        private void dibujarBaseDatos(Graphics2D g2, int x, int y) {
            // Servidor de base de datos
            g2.setColor(new Color(70, 70, 120));
            g2.fillRoundRect(x - 40, y - 60, 80, 120, 20, 20);

            // Luces de estado
            g2.setColor(lectoresActivos > 0 ? Color.GREEN : Color.RED);
            g2.fillOval(x - 30, y - 45, 10, 10);

            g2.setColor(escritorActivo ? Color.YELLOW : Color.RED);
            g2.fillOval(x + 20, y - 45, 10, 10);

            // Líneas de conexión
            g2.setColor(Color.GRAY);
            g2.setStroke(new BasicStroke(2f));
            for (int i = 0; i < 2; i++) {
                g2.drawLine(150, 100 + i * 100, x - 40, y - 20 + i * 40);
            }
            g2.drawLine(getWidth() - 150, 200, x + 40, y + 20);

            g2.setColor(Color.WHITE);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 12f));
            g2.drawString("BD", x - 15, y + 5);
        }

        private void dibujarLector(Graphics2D g2, int x, int y, int id, EstadoLector estado) {
            Color color = switch (estado) {
                case LEYENDO -> new Color(100, 200, 100);
                case ESPERANDO -> new Color(255, 200, 100);
                default -> new Color(200, 200, 200);
            };

            // Persona
            g2.setColor(color);
            g2.fillRect(x - 15, y - 5, 30, 35);

            g2.setColor(new Color(255, 220, 180));
            g2.fillOval(x - 12, y - 22, 24, 24);

            // Ojos
            g2.setColor(Color.BLACK);
            g2.fillOval(x - 8, y - 16, 4, 4);
            g2.fillOval(x + 4, y - 16, 4, 4);

            // Libro
            if (estado == EstadoLector.LEYENDO) {
                g2.setColor(new Color(200, 150, 100));
                g2.fillRect(x - 5, y + 5, 10, 15);
                g2.setColor(Color.BLACK);
                g2.drawRect(x - 5, y + 5, 10, 15);
            }

            // Brazo con libro
            g2.setColor(new Color(255, 220, 180));
            g2.setStroke(new BasicStroke(3f));
            if (estado == EstadoLector.LEYENDO) {
                g2.drawLine(x + 15, y + 5, x + 25, y - 5);
            } else {
                g2.drawLine(x + 15, y + 5, x + 25, y + 15);
            }

            // Piernas
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(4f));
            g2.drawLine(x - 5, y + 30, x - 5, y + 45);
            g2.drawLine(x + 5, y + 30, x + 5, y + 45);

            // Etiqueta
            g2.setColor(Color.BLACK);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 10f));
            g2.drawString("L" + id, x - 8, y + 60);
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 8f));
            g2.drawString(estado.toString().substring(0, 3), x - 10, y + 70);
        }

        private void dibujarEscritor(Graphics2D g2, int x, int y, int id, EstadoEscritor estado) {
            Color color = switch (estado) {
                case ESCRIBIENDO -> new Color(200, 100, 100);
                case ESPERANDO -> new Color(255, 200, 100);
                default -> new Color(200, 200, 200);
            };

            // Persona
            g2.setColor(color);
            g2.fillRect(x - 15, y - 5, 30, 35);

            g2.setColor(new Color(255, 220, 180));
            g2.fillOval(x - 12, y - 22, 24, 24);

            // Ojos
            g2.setColor(Color.BLACK);
            g2.fillOval(x - 8, y - 16, 4, 4);
            g2.fillOval(x + 4, y - 16, 4, 4);

            // Computadora/escritorio
            if (estado == EstadoEscritor.ESCRIBIENDO) {
                g2.setColor(new Color(80, 80, 80));
                g2.fillRect(x - 20, y + 10, 40, 4);
                g2.setColor(new Color(100, 100, 255));
                g2.fillRect(x - 15, y - 10, 30, 15);
            }

            // Brazos
            g2.setColor(new Color(255, 220, 180));
            g2.setStroke(new BasicStroke(3f));
            if (estado == EstadoEscritor.ESCRIBIENDO) {
                g2.drawLine(x - 15, y + 5, x - 25, y - 5);
                g2.drawLine(x + 15, y + 5, x + 25, y - 5);
            } else {
                g2.drawLine(x - 15, y + 5, x - 25, y + 15);
                g2.drawLine(x + 15, y + 5, x + 25, y + 15);
            }

            // Piernas
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(4f));
            g2.drawLine(x - 5, y + 30, x - 5, y + 45);
            g2.drawLine(x + 5, y + 30, x + 5, y + 45);

            // Etiqueta
            g2.setColor(Color.BLACK);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 10f));
            g2.drawString("E" + id, x - 8, y + 60);
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 8f));
            g2.drawString(estado.toString().substring(0, 3), x - 12, y + 70);
        }
    }
}