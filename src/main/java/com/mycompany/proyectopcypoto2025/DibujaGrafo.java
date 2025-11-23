package com.mycompany.proyectopcypoto2025;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class DibujaGrafo extends JPanel {

    private final List<Nodo> nodos = new ArrayList<>();
    private final List<Conexion> aristas = new ArrayList<>();
    private final Map<String, Nodo> by = new HashMap<>();

    private double z = 1.0;
    private Point2D.Double pan = new Point2D.Double(0, 0);
    private Nodo drag;
    private Point off = new Point();
    private boolean deadlock = false;

    private javax.swing.Timer animTimer;
    private int pasoActual = 0;
    private List<Conexion> pasos = new ArrayList<>();
    private String problemaActual = "";

    public DibujaGrafo() {
        setBackground(Color.white);
        setPreferredSize(new Dimension(900, 480));
        inicializarControles();
    }

    public void configurarProblema(String tipo) {
        nodos.clear();
        aristas.clear();
        by.clear();
        pan.x = 0;
        pan.y = 0;
        z = 1.0;
        repaint();
        
        problemaActual = tipo.toLowerCase().replace("á", "a").replace("-", " ").replace("ñ", "n").trim();

        if (animTimer != null && animTimer.isRunning()) animTimer.stop();

        switch (problemaActual) {
            case "productor-consumidor", "productor consumidor", "productor" -> {
                baseProductorConsumidor();
                animarSecuencia(Arrays.asList(
                    new String[]{"P0", "R1"}, new String[]{"R1", "P1"}
                ), false);
            }
            case "cena de filósofos", "filosofos" -> {
                baseFilosofos();
                animarSecuencia(Arrays.asList(
                    new String[]{"P0", "R0"}, new String[]{"P0", "R1"},
                    new String[]{"P1", "R1"}, new String[]{"P1", "R2"},
                    new String[]{"P2", "R2"}, new String[]{"P2", "R3"},
                    new String[]{"P3", "R3"}, new String[]{"P3", "R4"},
                    new String[]{"P4", "R4"}, new String[]{"P4", "R0"}
                ), false);
            }
            case "barbero dormilón", "barberodormilon" -> {
                baseBarberoDormilon();
                animarSecuenciaBarbero();
            }
            case "fumadores" -> {
                baseFumadores();
                animarSecuencia(Arrays.asList(
                    new String[]{"A", "R1"}, new String[]{"A", "R2"},
                    new String[]{"F3", "R1"}, new String[]{"F3", "R2"}
                ), false);
            }
            case "lectores-escritores", "lectores escritores" -> {
                baseLectoresEscritores();
                animarSecuenciaLectoresEscritores();
            }
            case "gpu_cluster", "clúster gpu", "cluster gpu" -> {
                baseGpuClusterMejorado();
                animarSecuenciaGpuCluster();
            }
            case "deadlock" -> {
                baseDeadlockGeneral();
                animarSecuencia(Arrays.asList(
                    new String[]{"P0", "R1"}, new String[]{"R1", "P1"},
                    new String[]{"P1", "R2"}, new String[]{"R2", "P2"},
                    new String[]{"P2", "R3"}, new String[]{"R3", "P0"}
                ), true);
            }
            default -> JOptionPane.showMessageDialog(this, "Tipo no reconocido: " + tipo);
        }
    }

    private void baseGpuClusterMejorado() {
        // Jobs (círculos verdes) a la izquierda
        addN("J1", 100, 100, Nodo.Tipo.PROCESO, new Color(100, 255, 150));
        addN("J2", 100, 180, Nodo.Tipo.PROCESO, new Color(100, 255, 150));
        addN("J3", 100, 260, Nodo.Tipo.PROCESO, new Color(100, 255, 150));
        addN("J4", 100, 340, Nodo.Tipo.PROCESO, new Color(100, 255, 150));
        
        // Islas GPU (rectángulos azules) en el centro
        addN("I0", 320, 120, Nodo.Tipo.RECURSO, new Color(150, 200, 255));
        addN("I1", 320, 220, Nodo.Tipo.RECURSO, new Color(150, 200, 255));
        addN("I2", 320, 320, Nodo.Tipo.RECURSO, new Color(150, 200, 255));
        
        // Recurso global G (rectángulo púrpura)
        addN("G", 580, 220, Nodo.Tipo.RECURSO, new Color(200, 150, 255));
        
        // Ventana K (rectángulo rosa)
        addN("K", 750, 100, Nodo.Tipo.RECURSO, new Color(255, 180, 200));
    }

    private void animarSecuenciaGpuCluster() {
        aristas.clear();
        pasos.clear();
        deadlock = false;

        // Conexiones de Islas a G (tokens globales)
        pasos.add(crearConexionConLabel(by.get("I0"), by.get("G"), "Tokens Globales"));
        pasos.add(crearConexionConLabel(by.get("I1"), by.get("G"), "Tokens Globales"));
        pasos.add(crearConexionConLabel(by.get("I2"), by.get("G"), "Tokens Globales"));
        
        // Asignaciones de Jobs a Islas
        pasos.add(crearConexionConLabel(by.get("J1"), by.get("I0"), "GPUs/Tokens"));
        pasos.add(crearConexionConLabel(by.get("J2"), by.get("I1"), "GPUs/Tokens"));
        pasos.add(crearConexionConLabel(by.get("J3"), by.get("I1"), "GPUs/Tokens"));
        pasos.add(crearConexionConLabel(by.get("J4"), by.get("I2"), "GPUs/Tokens"));
        
        // Conexión de G a K (ventanas)
        pasos.add(crearConexionConLabel(by.get("G"), by.get("K"), "Ventana"));

        iniciarAnimacion();
    }
    
    private Conexion crearConexionConLabel(Nodo a, Nodo b, String label) {
        Conexion c = new Conexion(a, b);
        c.setEtiqueta(label);
        return c;
    }

    private void animarSecuenciaBarbero() {
        aristas.clear();
        pasos.clear();
        deadlock = false;

        pasos.add(new Conexion(by.get("B"), by.get("S")));
        pasos.add(new Conexion(by.get("S"), by.get("B")));
        pasos.add(new Conexion(by.get("C1"), by.get("E")));
        pasos.add(new Conexion(by.get("E"), by.get("C1")));
        pasos.add(new Conexion(by.get("C2"), by.get("E")));
        pasos.add(new Conexion(by.get("E"), by.get("C2")));
        pasos.add(new Conexion(by.get("B"), by.get("C1")));
        pasos.add(new Conexion(by.get("C1"), by.get("B")));
        pasos.add(new Conexion(by.get("B"), by.get("C2")));
        pasos.add(new Conexion(by.get("C2"), by.get("B")));

        iniciarAnimacion();
    }

    private void animarSecuenciaLectoresEscritores() {
        aristas.clear();
        pasos.clear();
        deadlock = false;

        pasos.add(new Conexion(by.get("L1"), by.get("DB")));
        pasos.add(new Conexion(by.get("DB"), by.get("L1")));
        pasos.add(new Conexion(by.get("L2"), by.get("DB")));
        pasos.add(new Conexion(by.get("DB"), by.get("L2")));
        pasos.add(new Conexion(by.get("L1"), by.get("DB")));
        pasos.add(new Conexion(by.get("L2"), by.get("DB")));
        pasos.add(new Conexion(by.get("E1"), by.get("DB")));
        pasos.add(new Conexion(by.get("DB"), by.get("E1")));
        pasos.add(new Conexion(by.get("E1"), by.get("DB")));

        iniciarAnimacion();
    }

    private void iniciarAnimacion() {
        pasoActual = 0;
        if (animTimer != null && animTimer.isRunning()) animTimer.stop();

        animTimer = new javax.swing.Timer(800, e -> {
            if (pasoActual < pasos.size()) {
                aristas.add(pasos.get(pasoActual));
                pasoActual++;
                repaint();
            } else {
                ((javax.swing.Timer) e.getSource()).stop();
                if (!problemaActual.equals("deadlock") && !problemaActual.contains("gpu")) {
                    new javax.swing.Timer(2000, ev -> {
                        if (problemaActual.contains("barbero")) {
                            animarSecuenciaBarbero();
                        } else if (problemaActual.contains("lectores")) {
                            animarSecuenciaLectoresEscritores();
                        }
                    }).start();
                }
            }
        });
        animTimer.start();
    }

    private void addN(String n, int x, int y, Nodo.Tipo t, Color f) {
        Nodo nd = new Nodo(n, x, y, t, f);
        nodos.add(nd);
        by.put(n, nd);
    }

    private void baseProductorConsumidor() {
        addN("P0", 300, 200, Nodo.Tipo.PROCESO, new Color(255, 180, 180));
        addN("R1", 500, 200, Nodo.Tipo.RECURSO, new Color(180, 220, 255));
        addN("P1", 700, 200, Nodo.Tipo.PROCESO, new Color(200, 255, 200));
    }

    private void baseFilosofos() {
        int cx = 500, cy = 250, r = 150;
        for (int i = 0; i < 5; i++) {
            double ang = Math.toRadians(i * 72);
            addN("P" + i, (int) (cx + r * Math.cos(ang)), (int) (cy + r * Math.sin(ang)),
                    Nodo.Tipo.PROCESO, new Color(255, 240, 180));
        }
        for (int i = 0; i < 5; i++) {
            double ang = Math.toRadians(i * 72 + 36);
            addN("R" + i, (int) (cx + (r - 70) * Math.cos(ang)), (int) (cy + (r - 70) * Math.sin(ang)),
                    Nodo.Tipo.RECURSO, new Color(180, 200, 255));
        }
    }

    private void baseBarberoDormilon() {
        addN("B", 100, 200, Nodo.Tipo.PROCESO, new Color(150, 150, 255));
        addN("C1", 400, 100, Nodo.Tipo.PROCESO, new Color(255, 180, 180));
        addN("C2", 400, 300, Nodo.Tipo.PROCESO, new Color(255, 180, 180));
        addN("S", 250, 200, Nodo.Tipo.RECURSO, new Color(180, 220, 255));
        addN("E", 550, 200, Nodo.Tipo.RECURSO, new Color(200, 255, 200));
    }

    private void baseFumadores() {
        addN("A", 100, 250, Nodo.Tipo.PROCESO, new Color(200, 150, 255));
        addN("F1", 700, 100, Nodo.Tipo.PROCESO, new Color(255, 150, 150));
        addN("F2", 700, 250, Nodo.Tipo.PROCESO, new Color(150, 255, 150));
        addN("F3", 700, 400, Nodo.Tipo.PROCESO, new Color(150, 150, 255));
        addN("R1", 300, 150, Nodo.Tipo.RECURSO, new Color(255, 240, 180));
        addN("R2", 300, 350, Nodo.Tipo.RECURSO, new Color(180, 200, 255));
    }

    private void baseLectoresEscritores() {
        addN("L1", 300, 150, Nodo.Tipo.PROCESO, new Color(180, 255, 180));
        addN("L2", 300, 300, Nodo.Tipo.PROCESO, new Color(180, 255, 180));
        addN("E1", 300, 450, Nodo.Tipo.PROCESO, new Color(255, 200, 180));
        addN("DB", 550, 300, Nodo.Tipo.RECURSO, new Color(180, 200, 255));
    }

    private void baseDeadlockGeneral() {
        addN("P0", 520, 60, Nodo.Tipo.PROCESO, new Color(255, 200, 200));
        addN("P1", 660, 200, Nodo.Tipo.PROCESO, new Color(255, 240, 160));
        addN("P2", 520, 340, Nodo.Tipo.PROCESO, new Color(200, 240, 200));
        addN("R1", 420, 140, Nodo.Tipo.RECURSO, new Color(160, 210, 255));
        addN("R2", 620, 280, Nodo.Tipo.RECURSO, new Color(160, 200, 255));
        addN("R3", 420, 280, Nodo.Tipo.RECURSO, new Color(160, 190, 255));
    }

    private void animarSecuencia(List<String[]> conexiones, boolean deadlockMode) {
        aristas.clear();
        pasos.clear();
        deadlock = deadlockMode;

        for (String[] par : conexiones) {
            Nodo na = by.get(par[0]), nb = by.get(par[1]);
            if (na != null && nb != null) {
                pasos.add(new Conexion(na, nb));
            }
        }

        iniciarAnimacion();
    }

    private void inicializarControles() {
        MouseAdapter ma = new MouseAdapter() {
            Point last;

            @Override
            public void mousePressed(MouseEvent e) {
                last = e.getPoint();
                Point2D w = toWorld(e.getPoint());
                drag = find(w);
                if (drag != null) {
                    off.x = (int) (w.getX() - drag.x);
                    off.y = (int) (w.getY() - drag.y);
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (drag != null) {
                    Point2D w = toWorld(e.getPoint());
                    drag.x = (int) w.getX() - off.x;
                    drag.y = (int) w.getY() - off.y;
                    repaint();
                } else {
                    pan.x += (e.getX() - last.x) / z;
                    pan.y += (e.getY() - last.y) / z;
                    repaint();
                }
                last = e.getPoint();
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                double old = z;
                z *= (e.getWheelRotation() > 0 ? 0.9 : 1.1);
                z = Math.max(0.3, Math.min(z, 3));
                Point2D b = toWorld(e.getPoint(), old), a = toWorld(e.getPoint(), z);
                pan.x += (b.getX() - a.getX());
                pan.y += (b.getY() - a.getY());
                repaint();
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
        addMouseWheelListener(ma);
    }

    private Point2D toWorld(Point p) {
        return new Point2D.Double(p.x / z - pan.x, p.y / z - pan.y);
    }

    private Point2D toWorld(Point p, double zz) {
        return new Point2D.Double(p.x / zz - pan.x, p.y / zz - pan.y);
    }

    private Nodo find(Point2D p) {
        for (int i = nodos.size() - 1; i >= 0; i--) {
            Nodo n = nodos.get(i);
            double dx = p.getX() - n.x, dy = p.getY() - n.y;
            if (dx * dx + dy * dy <= Nodo.R * Nodo.R) return n;
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.scale(z, z);
        g2.translate(pan.x, pan.y);

        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(deadlock ? new Color(200, 60, 60) : new Color(60, 90, 200));
        for (Conexion c : aristas) c.dibujar(g2, aristas);

        for (Nodo n : nodos) {
            Shape s = n.shape();
            g2.setColor(n.fill);
            g2.fill(s);
            g2.setColor(Color.DARK_GRAY);
            g2.setStroke(new BasicStroke(1.8f));
            g2.draw(s);
            g2.setColor(Color.BLACK);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 12f));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(n.nombre, (int) (n.x - fm.stringWidth(n.nombre) / 2.0),
                    (int) (n.y + fm.getAscent() / 3.0));
        }
        g2.dispose();
    }
}