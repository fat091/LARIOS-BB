package com.mycompany.proyectopcypoto2025;

import java.awt.*;
import java.awt.geom.*;
import java.util.List;

public class Conexion {
    public final Nodo a, b;
    public String etiqueta;     // opcional

    public Conexion(Nodo a, Nodo b) { this.a = a; this.b = b; }
    public void setEtiqueta(String e) { this.etiqueta = e; }

    /** Dibuja la arista con clipping a bordes, paralelas y self-loop. */
    public void dibujar(Graphics2D g2, List<Conexion> todas) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (a == b) { dibujarSelfLoop(g2); return; }

        int[] idx = paraleloIndex(this, todas);
        int index = idx[0], count = idx[1];

        Point2D ca = new Point2D.Double(a.x, a.y);
        Point2D cb = new Point2D.Double(b.x, b.y);

        if (count <= 1) {
            Point2D pa = a.bordeHacia(cb);
            Point2D pb = b.bordeHacia(ca);
            g2.draw(new Line2D.Double(pa, pb));
            drawArrowHead(g2, pa, pb);
            drawLabel(g2, etiqueta, mid(pa, pb), -6);
        } else {
            double ang = Math.atan2(cb.getY() - ca.getY(), cb.getX() - ca.getX());
            double nx = -Math.sin(ang), ny = Math.cos(ang);
            double dist = 28.0 * index;

            Point2D ctrl = new Point2D.Double(
                    (ca.getX() + cb.getX()) / 2.0 + nx * dist,
                    (ca.getY() + cb.getY()) / 2.0 + ny * dist
            );

            Point2D pa = a.bordeHacia(ctrl);
            Point2D pb = b.bordeHacia(ctrl);

            QuadCurve2D q = new QuadCurve2D.Double();
            q.setCurve(pa, ctrl, pb);
            g2.draw(q);

            double angEnd = Math.atan2(pb.getY() - ctrl.getY(), pb.getX() - ctrl.getX());
            drawArrowHead(g2, pb, angEnd);
            drawLabel(g2, etiqueta, ctrl, -8);
        }
    }

    /* ---------- helpers ---------- */

    private void dibujarSelfLoop(Graphics2D g2) {
        int r = Nodo.R + 6;
        int w = (int)(1.6 * r), h = (int)(1.1 * r);
        int x = a.x + r / 2, y = a.y - r - h / 2;

        Arc2D arco = new Arc2D.Double(x, y, w, h, 10, 320, Arc2D.OPEN);
        g2.draw(arco);

        double ang = Math.toRadians(-10);
        Point2D tip = new Point2D.Double(x + w, y + h / 2.0);
        drawArrowHead(g2, tip, ang);
        drawLabel(g2, etiqueta, new Point2D.Double(x + w * 0.55, y - 4), 0);
    }

    private static int[] paraleloIndex(Conexion target, List<Conexion> todas) {
        int count = 0, idx = 0, seen = 0;
        for (Conexion c : todas) {
            if ((c.a == target.a && c.b == target.b) || (c.a == target.b && c.b == target.a)) {
                if (c == target) idx = seen;
                seen++;
            }
        }
        count = seen;
        idx -= (count - 1) / 2.0; // centra alrededor de 0
        return new int[]{idx, count};
    }

    private static Point2D mid(Point2D p, Point2D q) {
        return new Point2D.Double((p.getX() + q.getX()) / 2.0, (p.getY() + q.getY()) / 2.0);
    }

    private static void drawArrowHead(Graphics2D g2, Point2D from, Point2D to) {
        double ang = Math.atan2(to.getY() - from.getY(), to.getX() - from.getX());
        drawArrowHead(g2, to, ang);
    }

    private static void drawArrowHead(Graphics2D g2, Point2D tip, double ang) {
        int L = 12;
        int x = (int) Math.round(tip.getX());
        int y = (int) Math.round(tip.getY());
        int x1 = (int) (x - L * Math.cos(ang - Math.PI / 8));
        int y1 = (int) (y - L * Math.sin(ang - Math.PI / 8));
        int x2 = (int) (x - L * Math.cos(ang + Math.PI / 8));
        int y2 = (int) (y - L * Math.sin(ang + Math.PI / 8));
        g2.fillPolygon(new int[]{x, x1, x2}, new int[]{y, y1, y2}, 3);
    }

    private static void drawLabel(Graphics2D g2, String text, Point2D at, int dy) {
        if (text == null || text.isBlank()) return;
        Font f = g2.getFont();
        FontMetrics fm = g2.getFontMetrics(f);
        int w = fm.stringWidth(text) + 8, h = fm.getHeight();
        int bx = (int) Math.round(at.getX() - w / 2.0);
        int by = (int) Math.round(at.getY() - h + dy);

        Color old = g2.getColor();
        g2.setColor(new Color(255, 255, 210));
        g2.fillRoundRect(bx, by, w, h, 8, 8);
        g2.setColor(new Color(120, 120, 120));
        g2.drawRoundRect(bx, by, w, h, 8, 8);
        g2.setColor(Color.DARK_GRAY);
        g2.drawString(text, bx + 4, by + h - fm.getDescent());
        g2.setColor(old);
    }
}
