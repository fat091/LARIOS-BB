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
                "Métricas de sincronización (GPU/MPJ) - 5 Cores en Paralelo",
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
        
        // Configurar colores y estilos para cada serie
        renderer.setSeriesPaint(0, Color.RED);        // Mutex
        renderer.setSeriesPaint(1, Color.BLUE);       // Semáforos
        renderer.setSeriesPaint(2, Color.GREEN);      // Monitores
        renderer.setSeriesPaint(3, Color.ORANGE);     // Barreras
        renderer.setSeriesPaint(4, Color.MAGENTA);    // Variables Condición
        
        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            renderer.setSeriesLinesVisible(i, true);
            renderer.setSeriesShapesVisible(i, true);
            renderer.setSeriesStroke(i, new BasicStroke(2.0f));
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
        slider.setPaintLabels(true);
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
                iniciarCarrusel();
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
        // Simular datos de demo si no hay datos MPJ
        if (serieMutex.getItemCount() == 0) {
            simularDatosDemo();
        }
    }

    public void setPaused(boolean paused) {
        // no usamos animación local en esta versión
    }

    /**
     * Carga los datos generados por SyncMetricsMPJ.
     * Lee mpj_tiempos.csv desde el directorio del proyecto.
     */
    public void cargarDatosDesdeMPJ() {
        Path csvPath = Path.of(System.getProperty("user.dir"), "mpj_tiempos.csv");

        if (!Files.exists(csvPath)) {
            System.err.println("MetricasGpuPanel: NO se encontró mpj_tiempos.csv en: " + csvPath);
            
            // Intentar con el archivo antiguo
            csvPath = Path.of(System.getProperty("user.dir"), "mpj_metrics.csv");
            if (!Files.exists(csvPath)) {
                JOptionPane.showMessageDialog(this,
                    "❌ No se encontraron archivos MPJ\n\n" +
                    "Ejecuta primero: MPJ Express (5 Cores)\n" +
                    "para generar los datos.",
                    "Datos No Encontrados", JOptionPane.WARNING_MESSAGE);
                return;
            }
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
                if (parts.length < 6) continue;

                try {
                    int iter = Integer.parseInt(parts[0].trim());
                    double semaforos = Double.parseDouble(parts[1].trim());
                    double varCond = Double.parseDouble(parts[2].trim());
                    double monitores = Double.parseDouble(parts[3].trim());
                    double mutex = Double.parseDouble(parts[4].trim());
                    double barreras = Double.parseDouble(parts[5].trim());

                    serieSemaforos.add(iter, semaforos);
                    serieVarCondicion.add(iter, varCond);
                    serieMonitores.add(iter, monitores);
                    serieMutex.add(iter, mutex);
                    serieBarreras.add(iter, barreras);

                    if (iter > maxIter) maxIter = iter;
                    count++;
                } catch (NumberFormatException e) {
                    System.err.println("Error parseando línea: " + line);
                }
            }

            System.out.println("MetricasGpuPanel: se cargaron " + count +
                    " filas desde " + csvPath);

            // Ajustar slider y dominio
            slider.setMaximum(Math.max(windowSize, maxIter));
            int from = Math.max(0, maxIter - windowSize);
            updateDomainRange(from);
            slider.setValue(from);

            JOptionPane.showMessageDialog(this,
                "✅ Datos MPJ cargados correctamente\n\n" +
                "• " + count + " iteraciones cargadas\n" +
                "• 5 cores distribuidos\n" +
                "• Gráficas actualizadas",
                "Datos Cargados", JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException e) {
            System.err.println("MetricasGpuPanel: error leyendo CSV");
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "❌ Error leyendo archivo MPJ: " + e.getMessage(),
                "Error de Lectura", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ================= Métodos auxiliares =================

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

    private void iniciarCarrusel() {
        Timer carruselTimer = new Timer(3000, e -> {
            int currentVisible = getSerieVisible();
            int nextVisible = (currentVisible + 1) % dataset.getSeriesCount();
            mostrarSoloSerie(nextVisible);
        });
        carruselTimer.start();
    }

    private int getSerieVisible() {
        XYPlot plot = chart.getXYPlot();
        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            if (plot.getRenderer().isSeriesVisible(i)) {
                return i;
            }
        }
        return 0;
    }

    private void simularDatosDemo() {
        reset();
        
        for (int i = 0; i < 100; i++) {
            serieSemaforos.add(i, 10 + Math.sin(i * 0.1) * 5 + Math.random() * 2);
            serieVarCondicion.add(i, 12 + Math.cos(i * 0.15) * 4 + Math.random() * 3);
            serieMonitores.add(i, 8 + Math.sin(i * 0.2) * 3 + Math.random() * 1);
            serieMutex.add(i, 15 + Math.cos(i * 0.25) * 6 + Math.random() * 4);
            serieBarreras.add(i, 11 + Math.sin(i * 0.3) * 4 + Math.random() * 2);
        }
        
        slider.setMaximum(100);
        updateDomainRange(50);
        slider.setValue(50);
    }

    /**
     * Método para actualización en tiempo real desde los cores
     */
    public void actualizarMetricasTiempoReal(int core, int exitosas, int conflictos) {
        double eficiencia = (exitosas + conflictos) > 0 ? 
            (100.0 * exitosas / (exitosas + conflictos)) : 0.0;
        
        int tiempo = (int) (System.currentTimeMillis() / 1000); // Timestamp simplificado
        
        switch (core) {
            case 0 -> serieSemaforos.add(tiempo, eficiencia);
            case 1 -> serieVarCondicion.add(tiempo, eficiencia);
            case 2 -> serieMonitores.add(tiempo, eficiencia);
            case 3 -> serieMutex.add(tiempo, eficiencia);
            case 4 -> serieBarreras.add(tiempo, eficiencia);
        }
        
        repaint();
    }
}