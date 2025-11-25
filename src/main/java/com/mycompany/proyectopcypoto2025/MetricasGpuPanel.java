package com.mycompany.proyectopcypoto2025;

import javax.swing.*;
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

/**
 * Panel que muestra las métricas de sincronización GPU/MPJ en una sola gráfica
 * con 5 series (Mutex, Semáforos, Monitores, Barreras, Variables de Condición)
 * y un JSlider para recorrer la ventana temporal.
 *
 * Formato de archivo esperado:
 *
 *   iter;mutex;semaforos;monitores;barreras;varCond
 */
public class MetricasGpuPanel extends JPanel {

    private static final int DEFAULT_WINDOW_SIZE = 50;

    private final XYSeries serieMutex        = new XYSeries("Mutex");
    private final XYSeries serieSemaforos    = new XYSeries("Semáforos");
    private final XYSeries serieMonitores    = new XYSeries("Monitores");
    private final XYSeries serieBarreras     = new XYSeries("Barreras");
    private final XYSeries serieVarCondicion = new XYSeries("Variables de Condición");

    private final XYSeriesCollection dataset = new XYSeriesCollection();

    private JFreeChart chart;
    private ChartPanel chartPanel;
    private JSlider slider;
    private JLabel statusLabel;

    private int windowSize = DEFAULT_WINDOW_SIZE;
    private int maxIteraciones = 0;
    private boolean datosRealesCargados = false;
    private SyncMode modoActual = SyncMode.MONITORES;

    public MetricasGpuPanel() {
        setLayout(new BorderLayout(4, 4));
        setBorder(BorderFactory.createTitledBorder("Métricas de sincronización (GPU / MPJ)"));

        dataset.addSeries(serieMutex);
        dataset.addSeries(serieSemaforos);
        dataset.addSeries(serieMonitores);
        dataset.addSeries(serieBarreras);
        dataset.addSeries(serieVarCondicion);

        chart = crearChart(dataset);
        chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(600, 260));

        slider = new JSlider();
        slider.setMinimum(0);
        slider.setMaximum(0);
        slider.setValue(0);
        slider.addChangeListener(e -> actualizarRangoVisible());

        statusLabel = new JLabel("Sin datos de MPJ cargados.");
        statusLabel.setHorizontalAlignment(SwingConstants.LEFT);

        JPanel south = new JPanel(new BorderLayout());
        south.add(slider, BorderLayout.CENTER);
        south.add(statusLabel, BorderLayout.SOUTH);

        add(chartPanel, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
    }

    private JFreeChart crearChart(XYSeriesCollection dataset) {
        JFreeChart chart = ChartFactory.createXYLineChart(
                "Métricas de sincronización (GPU/MPJ)",
                "Tiempo / Iteración",
                "Valor / Tiempo (ms)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(new Color(30, 30, 30));
        plot.setDomainGridlinePaint(Color.GRAY);
        plot.setRangeGridlinePaint(Color.GRAY);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setDefaultShapesVisible(false);
        renderer.setDefaultStroke(new BasicStroke(2.0f));
        plot.setRenderer(renderer);

        NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
        domainAxis.setAutoRange(false);
        domainAxis.setRange(0, DEFAULT_WINDOW_SIZE);

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setAutoRange(true);

        return chart;
    }

    /** Cambia el modo actual (solo se usa para el subtítulo de la gráfica). */
    public void setModoActual(SyncMode modo) {
        this.modoActual = modo;
        chart.setSubtitle(0, new org.jfree.chart.title.TextTitle("Modo actual: " + modo.name()));
        repaint();
    }

    /**
     * Carga datos desde un archivo de texto generado por MPJ.
     * Cada línea debe tener la forma:
     *
     *   iter;mutex;semaforos;monitores;barreras;varCond
     */
    public void cargarDatosDesdeArchivo(Path ruta) throws IOException {
        limpiarDatos();

        List<String> lineas = Files.readAllLines(ruta, StandardCharsets.UTF_8);
        for (String linea : lineas) {
            linea = linea.trim();
            if (linea.isEmpty() || linea.startsWith("#")) {
                continue;
            }

            String[] partes = linea.split("[;,]");
            if (partes.length < 6) {
                continue; // línea inválida
            }

            try {
                int iter = Integer.parseInt(partes[0].trim());
                double mutex = Double.parseDouble(partes[1].trim());
                double sem = Double.parseDouble(partes[2].trim());
                double mon = Double.parseDouble(partes[3].trim());
                double barr = Double.parseDouble(partes[4].trim());
                double vc = Double.parseDouble(partes[5].trim());

                serieMutex.add(iter, mutex);
                serieSemaforos.add(iter, sem);
                serieMonitores.add(iter, mon);
                serieBarreras.add(iter, barr);
                serieVarCondicion.add(iter, vc);

                maxIteraciones = Math.max(maxIteraciones, iter);
            } catch (NumberFormatException ex) {
                // Ignorar líneas con valores no numéricos
            }
        }

        datosRealesCargados = true;

        // Ajustar slider y rango inicial
        slider.setMaximum(Math.max(0, maxIteraciones - windowSize));
        slider.setValue(Math.max(0, maxIteraciones - windowSize));
        actualizarRangoVisible();

        statusLabel.setText("Datos cargados. Iteraciones: " + maxIteraciones);
    }

    /** Actualiza el rango visible del eje X según la posición del slider. */
    private void actualizarRangoVisible() {
        XYPlot plot = (XYPlot) chart.getPlot();
        int start = slider.getValue();
        int end = start + windowSize;
        plot.getDomainAxis().setRange(start, end);
    }

    private void limpiarDatos() {
        serieMutex.clear();
        serieSemaforos.clear();
        serieMonitores.clear();
        serieBarreras.clear();
        serieVarCondicion.clear();
        maxIteraciones = 0;
        datosRealesCargados = false;
        slider.setMaximum(0);
        slider.setValue(0);
        statusLabel.setText("Sin datos de MPJ cargados.");
    }

    public boolean tieneDatosCargados() {
        return datosRealesCargados;
    }

    public int getMaxIteraciones() {
        return maxIteraciones;
    }
}
