package com.mycompany.proyectopcypoto2025;

import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import java.awt.*;

public class MonolithStyle {
    public static JFreeChart linea(DefaultCategoryDataset ds, String t, String x, String y) {
        JFreeChart c = ChartFactory.createLineChart(t, x, y, ds, PlotOrientation.VERTICAL, true, true, false);
        c.setBackgroundPaint(Color.white);
        CategoryPlot p = c.getCategoryPlot();
        p.setBackgroundPaint(new Color(248, 248, 248));
        p.setRangeGridlinePaint(new Color(210, 210, 210));
        LineAndShapeRenderer r = new LineAndShapeRenderer(true, false);
        r.setDefaultStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        p.setRenderer(r);
        return c;
    }
}