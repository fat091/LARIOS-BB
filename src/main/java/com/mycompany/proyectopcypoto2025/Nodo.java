package com.mycompany.proyectopcypoto2025;

import java.awt.*;
import java.awt.geom.*;

public class Nodo {
    public enum Tipo { PROCESO, RECURSO }

    public final String nombre;
    public int x, y;               // centro geométrico
    public final Tipo tipo;
    public Color fill;             // << CORREGIDO: ELIMINAMOS 'final' >>

    public static final int SIZE = 52;       // ancho/alto base
    public static final int R = SIZE / 2;    // radio “visual”

    public Nodo(String nombre, int x, int y, Tipo tipo, Color fill) {
        this.nombre = nombre;
        this.x = x;
        this.y = y;
        this.tipo = tipo;
        this.fill = fill;
    }

    /** Forma exacta del nodo para pintar (en coords de mundo). */
    public Shape shape() {
        int left = x - R, top = y - R;
        if (tipo == Tipo.PROCESO) {
            return new Ellipse2D.Double(left, top, SIZE, SIZE);
        } else {
            return new RoundRectangle2D.Double(left, top, SIZE, SIZE, 14, 14);
        }
    }

    /** Punto del borde donde el segmento (centro -> target) intersecta la forma. */
    public Point2D bordeHacia(Point2D target) {
        if (tipo == Tipo.PROCESO) {
            // elipse
            double rx = (SIZE - 2) / 2.0, ry = (SIZE - 2) / 2.0;
            double ang = Math.atan2(target.getY() - y, target.getX() - x);
            double bx = x + rx * Math.cos(ang);
            double by = y + ry * Math.sin(ang);
            return new Point2D.Double(bx, by);
        } else {
            // rect redondeado: aproximación por rectángulo interior con padding
            int pad = 6;
            double x1 = x - R + pad, y1 = y - R + pad;
            double x2 = x + R - pad, y2 = y + R - pad;

            double dx = target.getX() - x;
            double dy = target.getY() - y;

            double tMin = Double.POSITIVE_INFINITY;
            Point2D best = new Point2D.Double(x, y);

            if (dx != 0) {
                double tx = (dx > 0 ? x2 : x1);
                double t = (tx - x) / dx;
                double iy = y + t * dy;
                if (t > 0 && iy >= y1 && iy <= y2 && t < tMin) {
                    tMin = t; best = new Point2D.Double(tx, iy);
                }
            }
            if (dy != 0) {
                double ty = (dy > 0 ? y2 : y1);
                double t = (ty - y) / dy;
                double ix = x + t * dx;
                if (t > 0 && ix >= x1 && ix <= x2 && t < tMin) {
                    tMin = t; best = new Point2D.Double(ix, ty);
                }
            }
            return best;
        }
    }
}