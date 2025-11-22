package com.mycompany.proyectopcypoto2025;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class DibujaGrafo extends JPanel {

    private final java.util.List<Nodo> nodos = new ArrayList<>();
    private final java.util.List<Conexion> aristas = new ArrayList<>();
    private final Map<String, Nodo> by = new HashMap<>();

    private double z = 1.0;
    private Point2D.Double pan = new Point2D.Double(0, 0);
    private Nodo drag;
    private Point off = new Point();
    private boolean deadlock = false;

    // Animación
    private javax.swing.Timer animTimer;
    private int pasoActual = 0;
    private java.util.List<Conexion> pasos = new ArrayList<>();

    public DibujaGrafo() {
        setBackground(Color.white);
        setPreferredSize(new Dimension(900, 480));
        inicializarControles();
    }

    /* =================== API dinámica =================== */
    public void configurarProblema(String tipo) {
        nodos.clear(); aristas.clear(); by.clear(); repaint();

        if (animTimer != null && animTimer.isRunning()) animTimer.stop();


        String tipoLimpio = tipo.toLowerCase().replace("á", "a").replace("-", " ").replace("ñ", "n").trim();

        switch (tipoLimpio) {
            case "productor-consumidor", "productor consumidor", "productor" -> {
                baseProductorConsumidor();
                animarSecuencia(java.util.Arrays.asList(
                        new String[]{"P0","R1"}, new String[]{"R1","P1"}
                ), false);
            }
            case "cena de filósofos", "filosofos" -> {
                baseFilosofos();
                animarSecuencia(java.util.Arrays.asList(
                        new String[]{"P0","R0"}, new String[]{"P0","R1"},
                        new String[]{"P1","R1"}, new String[]{"P1","R2"},
                        new String[]{"P2","R2"}, new String[]{"P2","R3"},
                        new String[]{"P3","R3"}, new String[]{"P3","R4"},
                        new String[]{"P4","R4"}, new String[]{"P4","R0"}
                ), false);
            }
            case "barbero dormilón", "barberodormilon" -> {
                baseBarberoDormilon();
                animarSecuencia(java.util.Arrays.asList(
                        new String[]{"B","S"}, new String[]{"S","B"}, 
                        new String[]{"C1","E"}, new String[]{"C2","E"} 
                ), false);
            }
            case "fumadores" -> { 
                baseFumadores();
                animarSecuencia(java.util.Arrays.asList(
                        new String[]{"A","R1"}, new String[]{"A","R2"}, 
                        new String[]{"F3","R1"}, new String[]{"F3","R2"} 
                ), false);
            }
            case "lectores-escritores", "lectores escritores" -> {
                baseLectoresEscritores();
                animarSecuencia(java.util.Arrays.asList(
                        new String[]{"L1","DB"}, new String[]{"L2","DB"}, new String[]{"E1","DB"}
                ), false);
            }
            case "gpu_cluster", "clúster gpu" -> { 
                baseGpuCluster();
                animarSecuencia(java.util.Arrays.asList(
                        new String[]{"J1","I0"}, 
                        new String[]{"J1","G"},  
                        new String[]{"I0","J1"}, 
                        new String[]{"G","J1"},  
                        new String[]{"J1","K"}   
                ), false);

            }
            case "deadlock" -> {
                baseDeadlockGeneral();
                animarSecuencia(java.util.Arrays.asList(
                        new String[]{"P0","R1"}, new String[]{"R1","P1"},
                        new String[]{"P1","R2"}, new String[]{"R2","P2"},
                        new String[]{"P2","R3"}, new String[]{"R3","P0"}
                ), true);
            }
            default -> JOptionPane.showMessageDialog(this, "Tipo no reconocido: " + tipo);
        }
    }

    /* =================== Bases de nodos =================== */
    private void addN(String n, int x, int y, Nodo.Tipo t, Color f) {
        Nodo nd = new Nodo(n, x, y, t, f); nodos.add(nd); by.put(n, nd);
    }

    private void baseProductorConsumidor() {
        addN("P0", 300, 200, Nodo.Tipo.PROCESO, new Color(255,180,180));
        addN("R1", 500, 200, Nodo.Tipo.RECURSO, new Color(180,220,255)); 
        addN("P1", 700, 200, Nodo.Tipo.PROCESO, new Color(200,255,200));
    }

    private void baseFilosofos() {
        int cx = 500, cy = 250, r = 150;
        for (int i = 0; i < 5; i++) {
            double ang = Math.toRadians(i * 72);
            addN("P"+i, (int)(cx + r * Math.cos(ang)), (int)(cy + r * Math.sin(ang)),
                    Nodo.Tipo.PROCESO, new Color(255,240,180));
        }
        for (int i = 0; i < 5; i++) {
            double ang = Math.toRadians(i * 72 + 36);
            addN("R"+i, (int)(cx + (r-70) * Math.cos(ang)), (int)(cy + (r-70) * Math.sin(ang)),
                    Nodo.Tipo.RECURSO, new Color(180,200,255)); // Tenedores
        }
    }

    private void baseBarberoDormilon() {
        addN("B", 100, 200, Nodo.Tipo.PROCESO, new Color(150, 150, 255)); 
        addN("C1", 400, 100, Nodo.Tipo.PROCESO, new Color(255, 180, 180)); 
        addN("C2", 400, 300, Nodo.Tipo.PROCESO, new Color(255, 180, 180)); 
        addN("S", 250, 200, Nodo.Tipo.RECURSO, new Color(180, 220, 255)); 
        addN("E", 550, 200, Nodo.Tipo.RECURSO, new Color(200, 255, 200)); 
    }
    
    private void baseFumadores() {  // << CORREGIDO: ÚNICA DEFINICIÓN >>
        addN("A", 100, 250, Nodo.Tipo.PROCESO, new Color(200, 150, 255)); // Agente
        addN("F1", 700, 100, Nodo.Tipo.PROCESO, new Color(255, 150, 150)); // Fumador 1 (Papel)
        addN("F2", 700, 250, Nodo.Tipo.PROCESO, new Color(150, 255, 150)); // Fumador 2 (Tabaco)
        addN("F3", 700, 400, Nodo.Tipo.PROCESO, new Color(150, 150, 255)); // Fumador 3 (Cerillos)
        addN("R1", 300, 150, Nodo.Tipo.RECURSO, new Color(255, 240, 180)); // Recurso 1 (ej: Papel)
        addN("R2", 300, 350, Nodo.Tipo.RECURSO, new Color(180, 200, 255)); // Recurso 2 (ej: Tabaco)
    }


    private void baseLectoresEscritores() {
        addN("L1", 300, 150, Nodo.Tipo.PROCESO, new Color(180,255,180));
        addN("L2", 300, 300, Nodo.Tipo.PROCESO, new Color(180,255,180));
        addN("E1", 300, 450, Nodo.Tipo.PROCESO, new Color(255,200,180));
        addN("DB", 550, 300, Nodo.Tipo.RECURSO, new Color(180,200,255));
    }

    private void baseDeadlockGeneral() {
        addN("P0", 520,  60, Nodo.Tipo.PROCESO, new Color(255,200,200));
        addN("P1", 660, 200, Nodo.Tipo.PROCESO, new Color(255,240,160));
        addN("P2", 520, 340, Nodo.Tipo.PROCESO, new Color(200,240,200));
        addN("R1", 420, 140, Nodo.Tipo.RECURSO, new Color(160,210,255));
        addN("R2", 620, 280, Nodo.Tipo.RECURSO, new Color(160,200,255));
        addN("R3", 420, 280, Nodo.Tipo.RECURSO, new Color(160,190,255));
    }

    private void baseGpuCluster() {
        // Un Job representativo para la animación secuencial
        addN("J1", 100, 200, Nodo.Tipo.PROCESO, new Color(255, 150, 150)); 
        
        // Recursos Globales
        addN("G", 550, 200, Nodo.Tipo.RECURSO, new Color(200,180,255)); // Tokens Globales
        addN("K", 700, 200, Nodo.Tipo.RECURSO, new Color(255,200,200)); // Ventanas Colectivas (K)

        // Islas (Recursos de afinidad fuerte)
        addN("I0", 300, 100, Nodo.Tipo.RECURSO, new Color(180,220,255)); 
        addN("I1", 300, 200, Nodo.Tipo.RECURSO, new Color(180,220,255)); 
        addN("I2", 300, 300, Nodo.Tipo.RECURSO, new Color(180,220,255)); 

        // NOTA: Conexiones de recursos base se añaden en animarSecuencia
    }


    /* =================== Actualizador Dinámico GPU (Eliminado) =================== */

    /* =================== Animación =================== */
    private void animarSecuencia(java.util.List<String[]> conexiones, boolean deadlockMode) {
        aristas.clear(); 
        pasos.clear(); 
        deadlock = deadlockMode;
        
        // 1. Añadir las aristas base estructurales (solo para el Clúster GPU)
        if (by.containsKey("I0") && by.containsKey("G")) {
            pasos.add(new Conexion(by.get("I0"), by.get("G")));
            pasos.add(new Conexion(by.get("I1"), by.get("G")));
            pasos.add(new Conexion(by.get("I2"), by.get("G")));
        }
        
        // 2. Añadir la secuencia de animación del Job
        for (String[] par : conexiones) {
            Nodo na = by.get(par[0]), nb = by.get(par[1]);
            if (na != null && nb != null) {
                pasos.add(new Conexion(na, nb));
            }
        }

        pasoActual = 0;
        if (animTimer != null && animTimer.isRunning()) animTimer.stop();

        // El timer transfiere los pasos de uno en uno a la lista 'aristas'
        animTimer = new javax.swing.Timer(700, e -> {
            if (pasoActual < pasos.size()) {
                // Añadimos la siguiente arista al grafo y repintamos
                aristas.add(pasos.get(pasoActual));
                pasoActual++;
                repaint();
            } else {
                ((javax.swing.Timer) e.getSource()).stop();
            }
        });
        animTimer.start();
    }

    /* =================== Interacción y Pintado =================== */
    private void inicializarControles() {
        MouseAdapter ma = new MouseAdapter() {
            Point last;
            @Override public void mousePressed(MouseEvent e) {
                last = e.getPoint();
                Point2D w = toWorld(e.getPoint());
                drag = find(w);
                if (drag != null) {
                    off.x = (int)(w.getX() - drag.x);
                    off.y = (int)(w.getY() - drag.y);
                }
            }
            @Override public void mouseDragged(MouseEvent e) {
                if (drag != null) {
                    Point2D w = toWorld(e.getPoint());
                    drag.x = (int)w.getX() - off.x;
                    drag.y = (int)w.getY() - off.y;
                    repaint();
                } else {
                    pan.x += (e.getX() - last.x) / z;
                    pan.y += (e.getY() - last.y) / z;
                    repaint();
                }
                last = e.getPoint();
            }
            @Override public void mouseWheelMoved(MouseWheelEvent e) {
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

    private Point2D toWorld(Point p) { return new Point2D.Double(p.x / z - pan.x, p.y / z - pan.y); }
    private Point2D toWorld(Point p, double zz) { return new Point2D.Double(p.x / zz - pan.x, p.y / zz - pan.y); }

    private Nodo find(Point2D p) {
        for (int i = nodos.size()-1; i>=0; i--) {
            Nodo n = nodos.get(i);
            double dx = p.getX() - n.x, dy = p.getY() - n.y;
            if (dx*dx + dy*dy <= Nodo.R * Nodo.R) return n;
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
        g2.setColor(deadlock ? new Color(200,60,60) : new Color(60,90,200));
        for (Conexion c : aristas) c.dibujar(g2, aristas);

        for (Nodo n : nodos) {
            Shape s = n.shape();
            g2.setColor(n.fill); g2.fill(s);
            g2.setColor(Color.DARK_GRAY); g2.setStroke(new BasicStroke(1.8f)); g2.draw(s);
            g2.setColor(Color.BLACK);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(n.nombre, (int)(n.x - fm.stringWidth(n.nombre)/2.0),
                    (int)(n.y + fm.getAscent()/3.0));
        }
        g2.dispose();
    }
}