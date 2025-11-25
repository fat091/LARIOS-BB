package com.mycompany.proyectopcypoto2025;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class MetricasGpuPanel extends JPanel implements Reseteable, Demoable {

    public enum Mode { SCROLL, CARRUSEL, ACORDEON }

    private Mode mode = Mode.SCROLL;
    private int windowSize = 50;

    private final XYSeriesCollection dataset = new XYSeriesCollection();
    private final XYSeries serieMutex        = new XYSeries("Mutex");
    private final XYSeries serieSemaforos    = new XYSeries("Semáforos");
    private final XYSeries serieMonitores    = new XYSeries("Monitores");
    private final XYSeries serieBarreras     = new XYSeries("Barreras");
    private final XYSeries serieVarCondicion = new XYSeries("Variables de Condición");

    private MetricasGpuCollector collector;

    private JFreeChart chart;
    private ChartPanel chartPanel;
    private JSlider slider;

    public MetricasGpuPanel() {
        setLayout(new BorderLayout());
        initChart();
        initSlider();
    }

    public void setMetricasCollector(MetricasGpuCollector collector) {
        this.collector = collector;
    }

    private void initChart() {
        dataset.addSeries(serieMutex);
        dataset.addSeries(serieSemaforos);
        dataset.addSeries(serieMonitores);
        dataset.addSeries(serieBarreras);
        dataset.addSeries(serieVarCondicion);

        chart = ChartFactory.createXYLineChart(
                "Métricas de sincronización (GPU/MPJ)",
                "Tiempo / Iteración",
                "Valor / Tiempo (ms)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            renderer.setSeriesLinesVisible(i, true);
            renderer.setSeriesShapesVisible(i, false);
        }
        plot.setRenderer(renderer);

        NumberAxis domain = (NumberAxis) plot.getDomainAxis();
        domain.setAutoRange(false);
        domain.setRange(0, windowSize);

        NumberAxis range = (NumberAxis) plot.getRangeAxis();
        range.setAutoRange(true);

        chartPanel = new ChartPanel(chart);
        chartPanel.setMouseWheelEnabled(true);
        add(chartPanel, BorderLayout.CENTER);
    }

    private void initSlider() {
        slider = new JSlider(JSlider.HORIZONTAL, 0, windowSize, 0);
        slider.setPaintTicks(true);
        slider.setPaintLabels(false);
        slider.setMajorTickSpacing(windowSize / 5);
        slider.setEnabled(true);

        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (mode != Mode.SCROLL) return;
                int value = slider.getValue();
                updateDomainRange(value);
            }
        });

        add(slider, BorderLayout.SOUTH);
    }

    private void updateDomainRange(int from) {
        XYPlot plot = chart.getXYPlot();
        double start = Math.max(0, from);
        double end = start + windowSize;
        plot.getDomainAxis().setRange(start, end);
    }

    // ================= API pública =================

    public void setMode(Mode mode) {
        this.mode = mode;

        switch (mode) {
            case SCROLL -> {
                slider.setEnabled(true);
                setAllSeriesVisible(true);
            }
            case CARRUSEL -> {
                slider.setEnabled(false);
                mostrarSoloSerie(0);
            }
            case ACORDEON -> {
                slider.setEnabled(false);
                setAllSeriesVisible(true);
            }
        }
        revalidate();
        repaint();
    }

    @Override
    public void reset() {
        serieMutex.clear();
        serieSemaforos.clear();
        serieMonitores.clear();
        serieBarreras.clear();
        serieVarCondicion.clear();

        slider.setValue(0);
        slider.setMaximum(windowSize);
        updateDomainRange(0);
    }

    @Override
    public void demo() {
        // MPJ es la fuente de datos; aquí solo limpiamos
        reset();
    }

    public void setPaused(boolean paused) {
        // no usamos animación local en esta versión
    }

    /**
     * Carga los datos generados por SyncMetricsMPJ.
     * Lee mpj_metrics.csv desde el directorio del proyecto.
     */
    public void cargarDatosDesdeMPJ() {
        Path csvPath = Path.of(System.getProperty("user.dir"), "mpj_metrics.csv");

        if (!Files.exists(csvPath)) {
            System.err.println("MetricasGpuPanel: NO se encontró mpj_metrics.csv en: " + csvPath);
            return;
        }

        try {
            reset();

            List<String> lines = Files.readAllLines(csvPath, StandardCharsets.UTF_8);
            if (lines.isEmpty()) return;

            // Quitar encabezado si existe
            if (lines.get(0).toLowerCase().contains("iter")) {
                lines.remove(0);
            }

            int maxIter = 0;
            int count = 0;

            for (String line : lines) {
                String[] parts = line.split(",");
                if (parts.length != 6) continue;

                int iter = Integer.parseInt(parts[0].trim());
                double mutex   = Double.parseDouble(parts[1].trim());
                double semaf   = Double.parseDouble(parts[2].trim());
                double mon     = Double.parseDouble(parts[3].trim());
                double barr    = Double.parseDouble(parts[4].trim());
                double varCond = Double.parseDouble(parts[5].trim());

                serieMutex.add(iter, mutex);
                serieSemaforos.add(iter, semaf);
                serieMonitores.add(iter, mon);
                serieBarreras.add(iter, barr);
                serieVarCondicion.add(iter, varCond);

                if (iter > maxIter) maxIter = iter;
                count++;
            }

            System.out.println("MetricasGpuPanel: se cargaron " + count +
                    " filas desde " + csvPath);

            slider.setMaximum(Math.max(windowSize, maxIter));
            int from = Math.max(0, maxIter - windowSize);
            updateDomainRange(from);
            slider.setValue(from);

        } catch (IOException e) {
            System.err.println("MetricasGpuPanel: error leyendo CSV");
            e.printStackTrace();
        }
    }

    // ================= auxiliares =================

    private void setAllSeriesVisible(boolean visible) {
        XYPlot plot = chart.getXYPlot();
        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            plot.getRenderer().setSeriesVisible(i, visible);
        }
    }

    private void mostrarSoloSerie(int indexVisible) {
        XYPlot plot = chart.getXYPlot();
        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            plot.getRenderer().setSeriesVisible(i, i == indexVisible);
        }
    }
}
