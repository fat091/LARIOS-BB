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
 * Panel de Métricas GPU con integración MPJ Express definitiva
 * Muestra gráficas de eficiencia de 5 mecanismos de sincronización
 */
public class MetricasGpuPanel extends JPanel implements Reseteable, Demoable {

    // Modos de visualización
    public enum Mode { SCROLL, CARRUSEL, ACORDEON }

    private Mode mode = Mode.SCROLL;
    private int windowSize = 50;

    // Dataset para las gráficas
    private final XYSeriesCollection dataset = new XYSeriesCollection();
    private final XYSeries serieSemaforos    = new XYSeries("Semáforos");
    private final XYSeries serieVarCondicion = new XYSeries("Variables de Condición");
    private final XYSeries serieMonitores    = new XYSeries("Monitores");
    private final XYSeries serieMutex        = new XYSeries("Mutex");
    private final XYSeries serieBarreras     = new XYSeries("Barreras");

    private JFreeChart chart;
    private ChartPanel chartPanel;
    private JSlider slider;
    private JLabel statusLabel;
    private Timer carruselTimer;

    // Estado de carga de datos
    private boolean datosRealesCargados = false;
    private int maxIteraciones = 0;

    public MetricasGpuPanel() {
        setLayout(new BorderLayout());
        initChart();
        initSlider();
        initStatusPanel();
    }

    private void initChart() {
        // Agregar series en orden específico para colores consistentes
        dataset.addSeries(serieSemaforos);
        dataset.addSeries(serieVarCondicion);
        dataset.addSeries(serieMonitores);
        dataset.addSeries(serieMutex);
        dataset.addSeries(serieBarreras);

        chart = ChartFactory.createXYLineChart(
                "Eficiencia de Mecanismos de Sincronización en GPU Cluster",
                "Tiempo (t)",
                "Eficiencia (%)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        // Configurar colores y estilos
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(new Color(200, 200, 200));
        plot.setRangeGridlinePaint(new Color(200, 200, 200));

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        
        // Colores que coinciden con tu gráfica original
        renderer.setSeriesPaint(0, new Color(0, 0, 255));      // Semáforos - Azul
        renderer.setSeriesPaint(1, new Color(255, 0, 0));      // Var Condición - Rojo
        renderer.setSeriesPaint(2, new Color(0, 200, 0));      // Monitores - Verde
        renderer.setSeriesPaint(3, new Color(255, 165, 0));    // Mutex - Naranja
        renderer.setSeriesPaint(4, new Color(148, 0, 211));    // Barreras - Púrpura
        
        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            renderer.setSeriesLinesVisible(i, true);
            renderer.setSeriesShapesVisible(i, false); // Sin puntos para mejor visualización
            renderer.setSeriesStroke(i, new BasicStroke(2.0f));
        }
        plot.setRenderer(renderer);

        // Configurar ejes
        NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
        domainAxis.setAutoRange(false);
        domainAxis.setRange(0, windowSize);

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setAutoRange(false);
        rangeAxis.setRange(0, 100); // Eficiencia en porcentaje

        chartPanel = new ChartPanel(chart);
        chartPanel.setMouseWheelEnabled(true);
        add(chartPanel, BorderLayout.CENTER);
    }

    private void initSlider() {
        JPanel sliderPanel = new JPanel(new BorderLayout());
        
        slider = new JSlider(JSlider.HORIZONTAL, 0, windowSize, 0);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setMajorTickSpacing(windowSize / 5);
        slider.setEnabled(true);

        slider.addChangeListener(e -> {
            if (mode == Mode.SCROLL) {
                int value = slider.getValue();
                updateDomainRange(value);
            }
        });

        sliderPanel.add(slider, BorderLayout.CENTER);
        add(sliderPanel, BorderLayout.SOUTH);
    }

    private void initStatusPanel() {
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusLabel = new JLabel("📊 Esperando datos MPJ...");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 12));
        statusPanel.add(statusLabel);
        add(statusPanel, BorderLayout.NORTH);
    }

    private void updateDomainRange(int from) {
        XYPlot plot = chart.getXYPlot();
        double start = Math.max(0, from);
        double end = Math.min(start + windowSize, maxIteraciones);
        plot.getDomainAxis().setRange(start, end);
    }

    // ================= CARGA DE DATOS MPJ =================

    /**
     * Carga los datos reales generados por SyncMetricsMPJ
     */
    public void cargarDatosDesdeMPJ() {
        Path csvPath = Path.of(System.getProperty("user.dir"), "mpj_tiempos.csv");

        if (!Files.exists(csvPath)) {
            JOptionPane.showMessageDialog(this,
                "❌ No se encontró mpj_tiempos.csv\n\n" +
                "Ejecuta primero:\n" +
                "1. Compila: scripts/compilar_todo.bat\n" +
                "2. Ejecuta MPJ: scripts/ejecutar_mpj.bat\n\n" +
                "O usa el menú: 5 Cores MPJ → Ejecutar MPJ Express",
                "Datos No Encontrados", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            reset();

            List<String> lines = Files.readAllLines(csvPath, StandardCharsets.UTF_8);
            if (lines.isEmpty()) {
                throw new IOException("Archivo CSV vacío");
            }

            // Saltar encabezado
            if (lines.get(0).toLowerCase().contains("iter")) {
                lines.remove(0);
            }

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

                    // Agregar a las series
                    serieSemaforos.add(iter, semaforos);
                    serieVarCondicion.add(iter, varCond);
                    serieMonitores.add(iter, monitores);
                    serieMutex.add(iter, mutex);
                    serieBarreras.add(iter, barreras);

                    if (iter > maxIteraciones) {
                        maxIteraciones = iter;
                    }
                    count++;
                } catch (NumberFormatException e) {
                    System.err.println("⚠️ Error parseando línea: " + line);
                }
            }

            // Actualizar interfaz
            datosRealesCargados = true;
            slider.setMaximum(Math.max(windowSize, maxIteraciones));
            
            // Mostrar última ventana de datos
            int from = Math.max(0, maxIteraciones - windowSize);
            updateDomainRange(from);
            slider.setValue(from);

            statusLabel.setText("✅ Datos MPJ cargados: " + count + " iteraciones desde 5 cores");
            statusLabel.setForeground(new Color(0, 150, 0));

            System.out.println("✅ MetricasGpuPanel: Cargadas " + count + " filas desde " + csvPath);

            // Calcular y mostrar estadísticas
            mostrarEstadisticas();

        } catch (IOException e) {
            statusLabel.setText("❌ Error cargando datos MPJ");
            statusLabel.setForeground(Color.RED);
            
            JOptionPane.showMessageDialog(this,
                "❌ Error leyendo archivo MPJ:\n" + e.getMessage() + "\n\n" +
                "Verifica que el archivo esté correctamente generado.",
                "Error de Lectura", JOptionPane.ERROR_MESSAGE);
            
            e.printStackTrace();
        }
    }

    private void mostrarEstadisticas() {
        StringBuilder stats = new StringBuilder();
        stats.append("📊 ESTADÍSTICAS DE EFICIENCIA\n");
        stats.append("═".repeat(50)).append("\n\n");

        String[] nombres = {"Semáforos", "Var. Condición", "Monitores", "Mutex", "Barreras"};
        XYSeries[] series = {serieSemaforos, serieVarCondicion, serieMonitores, serieMutex, serieBarreras};

        for (int i = 0; i < series.length; i++) {
            XYSeries serie = series[i];
            if (serie.getItemCount() == 0) continue;

            double suma = 0, min = 100, max = 0;
            for (int j = 0; j < serie.getItemCount(); j++) {
                double val = serie.getY(j).doubleValue();
                suma += val;
                if (val < min) min = val;
                if (val > max) max = val;
            }
            double promedio = suma / serie.getItemCount();

            stats.append(String.format("%-20s: Promedio: %.2f%%, Min: %.2f%%, Max: %.2f%%\n",
                    nombres[i], promedio, min, max));
        }

        System.out.println(stats.toString());
    }

    // ================= MODOS DE VISUALIZACIÓN =================

    public void setMode(Mode mode) {
        this.mode = mode;

        // Detener carrusel anterior si existe
        if (carruselTimer != null && carruselTimer.isRunning()) {
            carruselTimer.stop();
        }

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
        carruselTimer = new Timer(3000, e -> {
            int currentVisible = getSerieVisible();
            int nextVisible = (currentVisible + 1) % dataset.getSeriesCount();
            mostrarSoloSerie(nextVisible);
            
            String[] nombres = {"Semáforos", "Variables de Condición", "Monitores", "Mutex", "Barreras"};
            statusLabel.setText("🔄 Carrusel: " + nombres[nextVisible]);
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

    // ================= INTERFAZ RESETEABLE Y DEMOABLE =================

    @Override
    public void reset() {
        serieSemaforos.clear();
        serieVarCondicion.clear();
        serieMonitores.clear();
        serieMutex.clear();
        serieBarreras.clear();

        datosRealesCargados = false;
        maxIteraciones = 0;
        slider.setValue(0);
        slider.setMaximum(windowSize);
        updateDomainRange(0);
        
        statusLabel.setText("📊 Esperando datos MPJ...");
        statusLabel.setForeground(Color.BLACK);
    }

    @Override
    public void demo() {
        if (!datosRealesCargados) {
            JOptionPane.showMessageDialog(this,
                "⚠️ No hay datos reales cargados\n\n" +
                "Para ver la demo:\n" +
                "1. Ejecuta MPJ Express desde el menú\n" +
                "2. O ejecuta manualmente: scripts/ejecutar_mpj.bat",
                "Demo Requiere Datos MPJ", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // ================= UTILIDADES =================

    public boolean tieneDatosCargados() {
        return datosRealesCargados;
    }

    public int getMaxIteraciones() {
        return maxIteraciones;
    }
}