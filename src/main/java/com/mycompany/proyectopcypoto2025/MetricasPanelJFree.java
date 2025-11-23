package com.mycompany.proyectopcypoto2025;

import javax.swing.*;
import java.awt.*;
import org.jfree.chart.ChartPanel;
import org.jfree.data.category.DefaultCategoryDataset;

public class MetricasPanelJFree extends JPanel implements Reseteable, Demoable {
    public enum Mode { SCROLL, CARRUSEL, ACORDEON }
    private DefaultCategoryDataset ds = new DefaultCategoryDataset();
    private ChartPanel cp;
    private Mode mode = Mode.CARRUSEL;
    private int t = 0;
    private Timer timer;
    private boolean paused = false;

    public MetricasPanelJFree() {
        setLayout(new BorderLayout());
        cp = new ChartPanel(MonolithStyle.linea(ds, "Métricas", "t", "v"));
        add(cp, BorderLayout.CENTER);
    }

    public void setMode(Mode m) {
        mode = m;
        reset();
        if (timer != null) timer.stop();
        paused = false;

        switch (m) {
            case SCROLL -> {
                timer = new Timer(80, e -> {
                    if (!paused) {
                        ds.addValue(80 + 18 * Math.sin(t * 0.25) + Math.random() * 5, "Scroll", Integer.valueOf(t++));
                    }
                });
                timer.start();
            }
            case CARRUSEL -> {
                for (int i = 0; i < 120; i++) {
                    ds.addValue(80 + 50 * Math.sin(i * 2 * Math.PI / 35.0) + Math.random() * 6, "Carrusel", Integer.valueOf(i));
                }
            }
            case ACORDEON -> {
                double amp = 20;
                for (int i = 0; i < 140; i++) {
                    if (i % 20 == 0) amp = (amp == 20 ? 50 : 20);
                    ds.addValue(70 + amp * Math.sin(i * 2 * Math.PI / 30.0) + Math.random() * 5, "Acordeón", Integer.valueOf(i));
                }
            }
        }
    }

    public void setPaused(boolean p) {
        this.paused = p;
        if (timer != null && p) timer.stop();
        else if (timer != null && !p && mode == Mode.SCROLL) timer.start();
    }

    @Override
    public void reset() {
        ds = new DefaultCategoryDataset();
        cp.setChart(MonolithStyle.linea(ds, "Métricas", "t", "v"));
        t = 0;
    }

    @Override
    public void demo() {
        setMode(mode);
    }
}