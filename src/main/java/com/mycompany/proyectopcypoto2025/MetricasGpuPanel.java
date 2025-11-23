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
    private final XYSeries serieSemaforos = new XYSeries("Semáforos");
    private final XYSeries serieVarCondicion = new XYSeries("Variables de Condición");
    private final XYSeries serieMonitores = new XYSeries("Monitores");
    private final XYSeries serieMutex = new XYSeries("Mutex");
    private final XYSeries serieBarreras = new XYSeries("Barreras");
    
    private ChartPanel chartPanel;
    private JLabel infoLabel;
    private MetricasGpuCollector metricas;
    
    public MetricasGpuPanel() {
        setLayout(new BorderLayout());
        
        dataset.addSeries(serieSemaforos);
        dataset.addSeries(serieVarCondicion);
        dataset.addSeries(serieMonitores);
        dataset.addSeries(serieMutex);
        dataset.addSeries(serieBarreras);
        
        JFreeChart chart = crearGrafica();
        chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(900, 450));
        chartPanel.setMouseWheelEnabled(true);
        chartPanel.setDomainZoomable(true);
        chartPanel.setRangeZoomable(true);
        
        // Agregar scroll pane para la gráfica
        JScrollPane scrollPane = new JScrollPane(chartPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);
        
        // Panel informativo con estado actual
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBackground(new Color(240, 240, 240));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        infoLabel = new JLabel("<html><b>Métricas GPU Cluster - Comparación de Mecanismos de Sincronización</b> | Modo: CARRUSEL | Estado: EJECUTANDO</html>");
        infoLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        infoPanel.add(infoLabel, BorderLayout.WEST);
        
        // Controles de la gráfica
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controlPanel.setBackground(new Color(240, 240, 240));
        
        JButton pauseBtn = new JButton("⏸ Pausar");
        pauseBtn.addActionListener(e -> {
            paused = !paused;
            pauseBtn.setText(paused ? "▶ Reanudar" : "⏸ Pausar");
            actualizarInfoLabel();
        });
        
        JButton resetBtn = new JButton("🔄 Resetear");
        resetBtn.addActionListener(e -> {
            reset();
            setMode(mode);
        });
        
        controlPanel.add(pauseBtn);
        controlPanel.add(resetBtn);
        infoPanel.add(controlPanel, BorderLayout.EAST);
        
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
        
        renderer.setSeriesPaint(0, new Color(0, 120, 215));      // Semáforos - Azul
        renderer.setSeriesPaint(1, new Color(230, 50, 50));      // Var.Condición - Rojo
        renderer.setSeriesPaint(2, new Color(0, 180, 80));       // Monitores - Verde
        renderer.setSeriesPaint(3, new Color(255, 140, 0));      // Mutex - Naranja
        renderer.setSeriesPaint(4, new Color(140, 0, 180));      // Barreras - Morado
        
        renderer.setSeriesStroke(0, new BasicStroke(2.8f));
        renderer.setSeriesStroke(1, new BasicStroke(2.8f));
        renderer.setSeriesStroke(2, new BasicStroke(2.8f));
        renderer.setSeriesStroke(3, new BasicStroke(2.8f));
        renderer.setSeriesStroke(4, new BasicStroke(2.8f));
        
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
    
    public void setMode(Mode m) {
        mode = m;
        limpiarSeries();
        if (timer != null) timer.stop();
        paused = false;
        t = 0;
        
        actualizarInfoLabel();
        
        switch (m) {
            case SCROLL -> iniciarModoScroll();
            case CARRUSEL -> iniciarModoCarrusel();
            case ACORDEON -> iniciarModoAcordeon();
        }
    }
    
    private void actualizarInfoLabel() {
        String modoTexto = switch(mode) {
            case SCROLL -> "SCROLL (Tiempo Real)";
            case CARRUSEL -> "CARRUSEL (Cíclico)";
            case ACORDEON -> "ACORDEÓN (Amplitud Variable)";
        };
        
        String estadoTexto = paused ? "PAUSADO" : "EJECUTANDO";
        String color = paused ? "#FF6B6B" : "#51CF66";
        
        infoLabel.setText(String.format(
            "<html><b>Métricas GPU Cluster - Comparación de Mecanismos de Sincronización</b> | Modo: <b>%s</b> | Estado: <b style='color:%s'>%s</b> | Puntos: %d</html>",
            modoTexto, color, estadoTexto, t
        ));
    }
    
    private void limpiarSeries() {
        serieSemaforos.clear();
        serieVarCondicion.clear();
        serieMonitores.clear();
        serieMutex.clear();
        serieBarreras.clear();
    }
    
    private void iniciarModoScroll() {
        timer = new Timer(150, e -> {
            if (!paused) {
                agregarPuntoScroll();
                t++;
                actualizarInfoLabel();
                
                // Ajustar tamaño del chartPanel para permitir scroll
                if (t > 100) {
                    chartPanel.setPreferredSize(new Dimension(900 + (t - 100) * 5, 450));
                    chartPanel.revalidate();
                }
            }
        });
        timer.start();
    }
    
    private void agregarPuntoScroll() {
        // Obtener eficiencia actual o simular
        double efSem, efVar, efMon, efMut, efBar;
        
        if (metricas != null) {
            efSem = metricas.calcularEficiencia(SyncMode.SEMAFOROS);
            efVar = metricas.calcularEficiencia(SyncMode.VAR_CONDICION);
            efMon = metricas.calcularEficiencia(SyncMode.MONITORES);
            efMut = metricas.calcularEficiencia(SyncMode.MUTEX);
            efBar = metricas.calcularEficiencia(SyncMode.BARRERAS);
            
            // Si no hay datos aún, simular
            if (efSem == 0 && efVar == 0 && efMon == 0 && efMut == 0 && efBar == 0) {
                efSem = 75 + 15 * Math.sin(t * 0.15) + (Math.random() * 8 - 4);
                efVar = 70 + 20 * Math.sin(t * 0.18) + (Math.random() * 10 - 5);
                efMon = 85 + 10 * Math.sin(t * 0.12) + (Math.random() * 5 - 2.5);
                efMut = 65 + 25 * Math.sin(t * 0.20) + (Math.random() * 12 - 6);
                efBar = 60 + 30 * Math.sin(t * 0.25) + (Math.random() * 15 - 7.5);
            }
        } else {
            // Simulación por defecto
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
                
                // Reiniciar cuando llega al final
                if (t >= 200) {
                    t = 0;
                    limpiarSeries();
                }
            }
        });
        timer.start();
    }
    
    private void iniciarModoAcordeon() {
        timer = new Timer(60, e -> {
            if (!paused && t < 250) {
                double fase = t % 50;
                
                double ampSem = (fase < 25) ? 15 : 25;
                double ampVar = (fase < 25) ? 20 : 35;
                double ampMon = (fase < 25) ? 10 : 20;
                double ampMut = (fase < 25) ? 25 : 40;
                double ampBar = (fase < 25) ? 30 : 45;
                
                double angulo = t * 2 * Math.PI / 30.0;
                
                double valSem = 75 + ampSem * Math.sin(angulo) + (Math.random() * 5 - 2.5);
                double valVar = 70 + ampVar * Math.sin(angulo) + (Math.random() * 5 - 2.5);
                double valMon = 85 + ampMon * Math.sin(angulo) + (Math.random() * 5 - 2.5);
                double valMut = 65 + ampMut * Math.sin(angulo) + (Math.random() * 5 - 2.5);
                double valBar = 60 + ampBar * Math.sin(angulo) + (Math.random() * 5 - 2.5);
                
                serieSemaforos.add(t, Math.max(0, Math.min(100, valSem)));
                serieVarCondicion.add(t, Math.max(0, Math.min(100, valVar)));
                serieMonitores.add(t, Math.max(0, Math.min(100, valMon)));
                serieMutex.add(t, Math.max(0, Math.min(100, valMut)));
                serieBarreras.add(t, Math.max(0, Math.min(100, valBar)));
                
                t++;
                actualizarInfoLabel();
                
                // Reiniciar cuando llega al final
                if (t >= 250) {
                    t = 0;
                    limpiarSeries();
                }
            }
        });
        timer.start();
    }
    
    public void setPaused(boolean p) {
        this.paused = p;
        actualizarInfoLabel();
    }
    
    @Override
    public void reset() {
        if (timer != null) {
            timer.stop();
        }
        limpiarSeries();
        t = 0;
        paused = false;
        chartPanel.setPreferredSize(new Dimension(900, 450));
        chartPanel.revalidate();
        
        if (metricas != null) {
            metricas.reset();
        }
        
        JFreeChart newChart = crearGrafica();
        chartPanel.setChart(newChart);
        actualizarInfoLabel();
    }
    
    @Override
    public void demo() {
        setMode(mode);
    }
}