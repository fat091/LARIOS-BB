package com.mycompany.proyectopcypoto2025;

import javax.swing.*;
import java.awt.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class MetricasGpuPanel extends JPanel implements Reseteable, Demoable {

    public enum Mode { SCROLL, CARRUSEL, ACORDEON }

    private Mode mode = Mode.CARRUSEL;
    private Timer timer;
    private boolean paused = false;
    private int t = 0;

    private final XYSeriesCollection dataset = new XYSeriesCollection();
    private final XYSeries serieSemaforos    = new XYSeries("Semáforos");
    private final XYSeries serieVarCondicion = new XYSeries("Variables de Condición");
    private final XYSeries serieMonitores    = new XYSeries("Monitores");
    private final XYSeries serieMutex        = new XYSeries("Mutex");
    private final XYSeries serieBarreras     = new XYSeries("Barreras");

    private ChartPanel chartPanel;
    private JScrollPane scrollPane;
    private JLabel infoLabel;
    private MetricasGpuCollector metricas;

    private static final Dimension BASE_CHART_SIZE = new Dimension(900, 260);

    public MetricasGpuPanel() {
        setLayout(new BorderLayout());

        // Series al dataset
        dataset.addSeries(serieSemaforos);
        dataset.addSeries(serieVarCondicion);
        dataset.addSeries(serieMonitores);
        dataset.addSeries(serieMutex);
        dataset.addSeries(serieBarreras);

        // Gráfica
        JFreeChart chart = crearGrafica();
        chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(BASE_CHART_SIZE));
        chartPanel.setMinimumSize(new Dimension(BASE_CHART_SIZE));
        chartPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, BASE_CHART_SIZE.height));

        // Sin zoom propio, solo usamos scroll externo
        chartPanel.setMouseWheelEnabled(false);
        chartPanel.setMouseZoomable(false);
        chartPanel.setDomainZoomable(false);
        chartPanel.setRangeZoomable(false);

        // ScrollPane que contiene la gráfica
        scrollPane = new JScrollPane(chartPanel);

        // Por defecto sin barras (se activará solo en SCROLL)
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

        // Eliminar espacio y bordes de scrollbars
        scrollPane.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 0));
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
        scrollPane.setViewportBorder(null);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBorder(null);

        add(scrollPane, BorderLayout.CENTER);

        // Pie de imagen (info)
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBackground(new Color(240, 240, 240));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        infoLabel = new JLabel();
        infoLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        infoPanel.add(infoLabel, BorderLayout.CENTER);

        // Altura fija para evitar cambios de layout al actualizar texto
        infoPanel.setPreferredSize(new Dimension(10, 40));
        infoPanel.setMinimumSize(new Dimension(10, 40));

        add(infoPanel, BorderLayout.SOUTH);

        setMode(Mode.CARRUSEL);
    }

    private JFreeChart crearGrafica() {
        JFreeChart chart = ChartFactory.createXYLineChart(
                "Eficiencia de Mecanismos de Sincronización en GPU Cluster",
                "Tiempo (t)",
                "Eficiencia (%)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        chart.setBackgroundPaint(Color.WHITE);
        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);

        renderer.setSeriesPaint(0, new Color(0, 120, 215));   // Semáforos
        renderer.setSeriesPaint(1, new Color(230, 50, 50));   // Var Condición
        renderer.setSeriesPaint(2, new Color(0, 180, 80));    // Monitores
        renderer.setSeriesPaint(3, new Color(255, 140, 0));   // Mutex
        renderer.setSeriesPaint(4, new Color(140, 0, 180));   // Barreras

        for (int i = 0; i < 5; i++) {
            renderer.setSeriesStroke(i, new BasicStroke(2.8f));
        }

        plot.setRenderer(renderer);
        plot.setBackgroundPaint(new Color(250, 250, 250));
        plot.setRangeGridlinePaint(new Color(200, 200, 200));
        plot.setDomainGridlinePaint(new Color(200, 200, 200));
        plot.getRangeAxis().setRange(0, 100);

        return chart;
    }

    public void setMetricasCollector(MetricasGpuCollector metricas) {
        this.metricas = metricas;
    }

    // ==================== CAMBIO DE MODO ====================
    public void setMode(Mode m) {
        mode = m;
        limpiarSeries();
        if (timer != null) timer.stop();
        paused = false;
        t = 0;

        // Config del scroll para cada modo
        if (m == Mode.SCROLL) {
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            // Barra visible solo cuando se necesite, sin ocupar espacio extra fuera de SCROLL
            scrollPane.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 12));
        } else {
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scrollPane.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 0));
            chartPanel.setPreferredSize(new Dimension(BASE_CHART_SIZE));
            chartPanel.setMinimumSize(new Dimension(BASE_CHART_SIZE));
            chartPanel.revalidate();
        }

        // En Acordeón la info se actualiza solo una vez (para estabilidad)
        actualizarInfoLabel();

        switch (m) {
            case SCROLL   -> iniciarModoScroll();
            case CARRUSEL -> iniciarModoCarrusel();
            case ACORDEON -> iniciarModoAcordeon();
        }
    }

    private void actualizarInfoLabel() {
        String modoTexto = switch (mode) {
            case SCROLL   -> "SCROLL (Tiempo Real)";
            case CARRUSEL -> "CARRUSEL (Cíclico)";
            case ACORDEON -> "ACORDEÓN (Amplitud Variable)";
        };
        String estadoTexto = paused ? "PAUSADO" : "EJECUTANDO";
        String color = paused ? "#FF6B6B" : "#51CF66";

        String leyendaColores =
                "Colores: Azul = Semáforos, Rojo = Var. Condición, Verde = Monitores, " +
                "Naranja = Mutex, Morado = Barreras";

        infoLabel.setText(String.format(
                "<html><b>Métricas GPU Cluster - Comparación de Mecanismos de Sincronización</b>" +
                " | Modo: <b>%s</b>" +
                " | Estado: <b style='color:%s'>%s</b>" +
                " | Puntos: %d" +
                "<br>%s</html>",
                modoTexto, color, estadoTexto, t, leyendaColores
        ));
    }

    private void limpiarSeries() {
        serieSemaforos.clear();
        serieVarCondicion.clear();
        serieMonitores.clear();
        serieMutex.clear();
        serieBarreras.clear();
    }

    // ==================== SCROLL ====================
    private void iniciarModoScroll() {
        timer = new Timer(150, e -> {
            if (!paused) {
                agregarPuntoScroll();
                t++;
                actualizarInfoLabel();
                if (t > 100) {
                    int extraWidth = (t - 100) * 5;
                    chartPanel.setPreferredSize(
                            new Dimension(BASE_CHART_SIZE.width + extraWidth, BASE_CHART_SIZE.height)
                    );
                    chartPanel.revalidate();
                }
            }
        });
        timer.start();
    }

    private void agregarPuntoScroll() {
        double efSem, efVar, efMon, efMut, efBar;

        if (metricas != null) {
            efSem = metricas.calcularEficiencia(SyncMode.SEMAFOROS);
            efVar = metricas.calcularEficiencia(SyncMode.VAR_CONDICION);
            efMon = metricas.calcularEficiencia(SyncMode.MONITORES);
            efMut = metricas.calcularEficiencia(SyncMode.MUTEX);
            efBar = metricas.calcularEficiencia(SyncMode.BARRERAS);

            if (efSem == 0 && efVar == 0 && efMon == 0 && efMut == 0 && efBar == 0) {
                efSem = 75 + 15 * Math.sin(t * 0.15) + (Math.random() * 8 - 4);
                efVar = 70 + 20 * Math.sin(t * 0.18) + (Math.random() * 10 - 5);
                efMon = 85 + 10 * Math.sin(t * 0.12) + (Math.random() * 5 - 2.5);
                efMut = 65 + 25 * Math.sin(t * 0.20) + (Math.random() * 12 - 6);
                efBar = 60 + 30 * Math.sin(t * 0.25) + (Math.random() * 15 - 7.5);
            }
        } else {
            efSem = 75 + 15 * Math.sin(t * 0.15) + (Math.random() * 8 - 4);
            efVar = 70 + 20 * Math.sin(t * 0.18) + (Math.random() * 10 - 5);
            efMon = 85 + 10 * Math.sin(t * 0.12) + (Math.random() * 5 - 2.5);
            efMut = 65 + 25 * Math.sin(t * 0.20) + (Math.random() * 12 - 6);
            efBar = 60 + 30 * Math.sin(t * 0.25) + (Math.random() * 15 - 7.5);
        }

        serieSemaforos.add(t, Math.max(0, Math.min(100, efSem)));
        serieVarCondicion.add(t, Math.max(0, Math.min(100, efVar)));
        serieMonitores.add(t, Math.max(0, Math.min(100, efMon)));
        serieMutex.add(t, Math.max(0, Math.min(100, efMut)));
        serieBarreras.add(t, Math.max(0, Math.min(100, efBar)));
    }

    // ==================== CARRUSEL ====================
    private void iniciarModoCarrusel() {
        timer = new Timer(80, e -> {
            if (!paused && t < 200) {
                double angulo = t * 2 * Math.PI / 40.0;

                double valSem = 75 + 15 * Math.sin(angulo) + (Math.random() * 8 - 4);
                double valVar = 70 + 20 * Math.sin(angulo * 1.14) + (Math.random() * 10 - 5);
                double valMon = 85 + 10 * Math.sin(angulo * 0.89) + (Math.random() * 5 - 2.5);
                double valMut = 65 + 25 * Math.sin(angulo * 1.33) + (Math.random() * 12 - 6);
                double valBar = 60 + 30 * Math.sin(angulo * 1.6) + (Math.random() * 15 - 7.5);

                serieSemaforos.add(t, Math.max(0, Math.min(100, valSem)));
                serieVarCondicion.add(t, Math.max(0, Math.min(100, valVar)));
                serieMonitores.add(t, Math.max(0, Math.min(100, valMon)));
                serieMutex.add(t, Math.max(0, Math.min(100, valMut)));
                serieBarreras.add(t, Math.max(0, Math.min(100, valBar)));

                t++;
                actualizarInfoLabel();

                if (t >= 200) {
                    t = 0;
                    limpiarSeries();
                }
            }
        });
        timer.start();
    }

    // ==================== ACORDEÓN ====================
    private void iniciarModoAcordeon() {
        timer = new Timer(80, e -> {
            if (!paused && t < 200) {

                double mod = 1.0 + 0.5 * Math.sin(t * 2 * Math.PI / 160.0);
                double angulo = t * 2 * Math.PI / 40.0;

                double valSem = 75 + mod * 15 * Math.sin(angulo)        + (Math.random() * 5 - 2.5);
                double valVar = 70 + mod * 20 * Math.sin(angulo * 1.10) + (Math.random() * 5 - 2.5);
                double valMon = 85 + mod * 10 * Math.sin(angulo * 0.92) + (Math.random() * 5 - 2.5);
                double valMut = 65 + mod * 25 * Math.sin(angulo * 1.25) + (Math.random() * 5 - 2.5);
                double valBar = 60 + mod * 30 * Math.sin(angulo * 1.45) + (Math.random() * 5 - 2.5);

                serieSemaforos.add(t, Math.max(0, Math.min(100, valSem)));
                serieVarCondicion.add(t, Math.max(0, Math.min(100, valVar)));
                serieMonitores.add(t, Math.max(0, Math.min(100, valMon)));
                serieMutex.add(t, Math.max(0, Math.min(100, valMut)));
                serieBarreras.add(t, Math.max(0, Math.min(100, valBar)));

                t++;
                // Aquí NO llamamos actualizarInfoLabel() para que el layout no cambie

                if (t >= 200) {
                    t = 0;
                    limpiarSeries();
                }
            }
        });
        timer.start();
    }

    // ==================== CONTROL EXTERNO ====================
    public void setPaused(boolean p) {
        this.paused = p;
        actualizarInfoLabel();
    }

    @Override
    public void reset() {
        if (timer != null) timer.stop();
        limpiarSeries();
        t = 0;
        paused = false;

        chartPanel.setPreferredSize(new Dimension(BASE_CHART_SIZE));
        chartPanel.setMinimumSize(new Dimension(BASE_CHART_SIZE));
        chartPanel.revalidate();

        if (metricas != null) metricas.reset();

        JFreeChart newChart = crearGrafica();
        chartPanel.setChart(newChart);
        actualizarInfoLabel();
    }

    @Override
    public void demo() {
        setMode(mode);
    }
}
