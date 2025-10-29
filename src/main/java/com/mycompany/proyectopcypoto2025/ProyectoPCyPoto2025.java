package com.mycompany.proyectopcypoto2025;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import org.jfree.chart.*;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.chart.plot.PlotOrientation;

public class ProyectoPCyPoto2025 extends JFrame {
    private JMenu archivo, problemas, synchMenu, graficaMenu;
    private JMenuItem procon, filosofos, barbero, fumadores, lecesc, salir;
    private JRadioButtonMenuItem semaforoRadio, conditionRadio, monitorRadio, barreraRadio, mutexRadio;
    private JRadioButtonMenuItem carruselRadio, scrollRadio, acordeonRadio;
    private ButtonGroup techniqueGroup, graficaGroup;
    private JPanel panelIzquierdo;
    private JPanel panelDerecho;
    private JPopupMenu menuPanel;
    private int procesoCount = 1, recursoCount = 1;
    private final List<Nodo> nodos = new ArrayList<>();
    private final List<Flecha> flechas = new ArrayList<>();
    private Nodo nodoSeleccionado = null;
    private volatile List<Thread> hilosActivos = new ArrayList<>();
    private javax.swing.Timer graphAnimator;
    private Queue<Runnable> graphAnimationActions;
    
    private JScrollPane graficaScrollPane;
    private ChartPanel chartPanel;
    private JFreeChart currentChart;
    private String currentChartType = "carrusel";
    private javax.swing.Timer chartAnimator;
    private DefaultCategoryDataset chartDataset;

    private enum Technique { SEMAFORO, CONDITION, MONITOR, BARRERA, MUTEX }
    private Technique selectedTechnique = Technique.SEMAFORO;
    private String currentProblemName = null;

    // Interfaz comÃºn
    interface ProblemaSimulacion {
        void iniciarSimulacion();
        void detenerSimulacion();
        JPanel getPanelVisual();
        Thread[] getHilos();
        void notificarEventoGrafo(String evento, String detalles);
    }

    // ============================================================================
    // PRODUCTOR-CONSUMIDOR - TODAS LAS TÃ‰CNICAS
    // ============================================================================
    
    static class ProductorConsumidorBase implements ProblemaSimulacion {
        protected final int capacidad = 20;
        protected final Queue<String> almacen = new LinkedList<>();
        protected PanelProductorConsumidor panelVisual;
        protected Thread productor, consumidor;
        protected volatile boolean ejecutando = false;
        protected ProyectoPCyPoto2025 parent;
        
        public ProductorConsumidorBase(ProyectoPCyPoto2025 parent) {
            this.parent = parent;
            this.panelVisual = new PanelProductorConsumidor(this);
        }
        
        public int getNivel() { return almacen.size(); }
        public int getCapacidad() { return capacidad; }
        
        @Override
        public void iniciarSimulacion() {} // Implementado en subclases
        
        @Override
        public void detenerSimulacion() {
            ejecutando = false;
            if (productor != null) productor.interrupt();
            if (consumidor != null) consumidor.interrupt();
        }

        @Override
        public JPanel getPanelVisual() { return panelVisual; }
        
        @Override
        public Thread[] getHilos() { return new Thread[]{productor, consumidor}; }
        
        @Override
        public void notificarEventoGrafo(String evento, String detalles) {
            if (parent != null) {
                parent.resaltarNodoGrafo(evento, detalles);
            }
        }
    }
    
    static class ProductorConsumidorSemaforo extends ProductorConsumidorBase {
        private final Semaphore empty, full, mutex;
        public ProductorConsumidorSemaforo(ProyectoPCyPoto2025 parent) {
            super(parent);
            empty = new Semaphore(capacidad);
            full = new Semaphore(0);
            mutex = new Semaphore(1);
        }
        
        @Override
        public void iniciarSimulacion() {
            ejecutando = true;
            productor = new Thread(() -> {
                int i = 0;
                while (ejecutando && !Thread.currentThread().isInterrupted()) {
                    try {
                        panelVisual.setProductorStatus("Produciendo " + i);
                        notificarEventoGrafo("Prod", "Produciendo");
                        Thread.sleep(ThreadLocalRandom.current().nextInt(500, 1500));
                        
                        empty.acquire();
                        mutex.acquire();
                        almacen.add("Item-" + (++i));
                        panelVisual.setProductorStatus("AÃ±adido " + i);
                        notificarEventoGrafo("Buffer", "Agregado item");
                        panelVisual.animarProduccion();
                        SwingUtilities.invokeLater(panelVisual::repaint);
                        mutex.release();
                        full.release();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            consumidor = new Thread(() -> {
                while (ejecutando && !Thread.currentThread().isInterrupted()) {
                    try {
                        panelVisual.setConsumidorStatus("Esperando...");
                        notificarEventoGrafo("Cons", "Esperando");
                        full.acquire();
                        mutex.acquire();
                        String item = almacen.poll();
                        panelVisual.setConsumidorStatus("Consumido: " + item);
                        notificarEventoGrafo("Buffer", "Consumido item");
                        panelVisual.animarConsumo();
                        SwingUtilities.invokeLater(panelVisual::repaint);
                        mutex.release();
                        empty.release();
                        Thread.sleep(ThreadLocalRandom.current().nextInt(700, 2000));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            productor.start();
            consumidor.start();
        }
    }
    
    static class ProductorConsumidorCondition extends ProductorConsumidorBase {
        private final Lock lock = new ReentrantLock();
        private final Condition notFull = lock.newCondition();
        private final Condition notEmpty = lock.newCondition();
        public ProductorConsumidorCondition(ProyectoPCyPoto2025 parent) {
            super(parent);
        }
        
        @Override
        public void iniciarSimulacion() {
            ejecutando = true;
            productor = new Thread(() -> {
                int i = 0;
                while (ejecutando && !Thread.currentThread().isInterrupted()) {
                    try {
                        panelVisual.setProductorStatus("Produciendo " + i);
                        notificarEventoGrafo("Prod", "Produciendo");
                        Thread.sleep(ThreadLocalRandom.current().nextInt(500, 1500));
                        
                        lock.lock();
                        try {
                            while (almacen.size() >= capacidad) {
                                notFull.await();
                            }
                            almacen.add("Item-" + (++i));
                            panelVisual.setProductorStatus("AÃ±adido " + i);
                            notificarEventoGrafo("Buffer", "Agregado item");
                            panelVisual.animarProduccion();
                            SwingUtilities.invokeLater(panelVisual::repaint);
                            notEmpty.signal();
                        } finally {
                            lock.unlock();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            consumidor = new Thread(() -> {
                while (ejecutando && !Thread.currentThread().isInterrupted()) {
                    try {
                        panelVisual.setConsumidorStatus("Esperando...");
                        notificarEventoGrafo("Cons", "Esperando");
                        lock.lock();
                        try {
                            while (almacen.isEmpty()) {
                                notEmpty.await();
                            }
                            String item = almacen.poll();
                            panelVisual.setConsumidorStatus("Consumido: " + item);
                            notificarEventoGrafo("Buffer", "Consumido item");
                            panelVisual.animarConsumo();
                            SwingUtilities.invokeLater(panelVisual::repaint);
                            notFull.signal();
                        } finally {
                            lock.unlock();
                        }
                        Thread.sleep(ThreadLocalRandom.current().nextInt(700, 2000));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            productor.start();
            consumidor.start();
        }
    }
    
    static class ProductorConsumidorMonitor extends ProductorConsumidorBase {
        private final Object monitor = new Object();
        public ProductorConsumidorMonitor(ProyectoPCyPoto2025 parent) {
            super(parent);
        }
        
        @Override
        public void iniciarSimulacion() {
            ejecutando = true;
            productor = new Thread(() -> {
                int i = 0;
                while (ejecutando && !Thread.currentThread().isInterrupted()) {
                    try {
                        panelVisual.setProductorStatus("Produciendo " + i);
                        notificarEventoGrafo("Prod", "Produciendo");
                        Thread.sleep(ThreadLocalRandom.current().nextInt(500, 1500));
                        
                        synchronized (monitor) {
                            while (almacen.size() >= capacidad) {
                                monitor.wait();
                            }
                            almacen.add("Item-" + (++i));
                            panelVisual.setProductorStatus("AÃ±adido " + i);
                            notificarEventoGrafo("Buffer", "Agregado item");
                            panelVisual.animarProduccion();
                            SwingUtilities.invokeLater(panelVisual::repaint);
                            monitor.notifyAll();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            consumidor = new Thread(() -> {
                while (ejecutando && !Thread.currentThread().isInterrupted()) {
                    try {
                        panelVisual.setConsumidorStatus("Esperando...");
                        notificarEventoGrafo("Cons", "Esperando");
                        synchronized (monitor) {
                            while (almacen.isEmpty()) {
                                monitor.wait();
                            }
                            String item = almacen.poll();
                            panelVisual.setConsumidorStatus("Consumido: " + item);
                            notificarEventoGrafo("Buffer", "Consumido item");
                            panelVisual.animarConsumo();
                            SwingUtilities.invokeLater(panelVisual::repaint);
                            monitor.notifyAll();
                        }
                        Thread.sleep(ThreadLocalRandom.current().nextInt(700, 2000));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            productor.start();
            consumidor.start();
        }
    }

    static class PanelProductorConsumidor extends JPanel {
        private final ProductorConsumidorBase problema;
        private volatile String productorStatus = "...";
        private volatile String consumidorStatus = "...";
        private int anguloProductor = 0;
        private int anguloConsumidor = 0;
        private float alphaProduccion = 0.0f;
        private float alphaConsumo = 0.0f;
        private javax.swing.Timer animationTimer;
        
        public PanelProductorConsumidor(ProductorConsumidorBase problema) {
            this.problema = problema;
            setBackground(new Color(240, 245, 255));
            setPreferredSize(new Dimension(750, 650));
            
            animationTimer = new javax.swing.Timer(50, e -> {
                anguloProductor = (anguloProductor + 5) % 360;
                anguloConsumidor = (anguloConsumidor + 3) % 360;
                if (alphaProduccion > 0) alphaProduccion -= 0.05f;
                if (alphaConsumo > 0) alphaConsumo -= 0.05f;
                repaint();
            });
            animationTimer.start();
        }
        
        public void setProductorStatus(String status) { this.productorStatus = status; repaint(); }
        public void setConsumidorStatus(String status) { this.consumidorStatus = status; repaint(); }
        public void animarProduccion() { alphaProduccion = 1.0f; }
        public void animarConsumo() { alphaConsumo = 1.0f; }
        
        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            dibujarProductor(g, 80, 100);
            dibujarTanque(g, 280, 150, 160, 400);
            dibujarConsumidor(g, 550, 100);
            
            if (alphaProduccion > 0) {
                dibujarParticulas(g, 230, 250, alphaProduccion, Color.GREEN);
            }
            if (alphaConsumo > 0) {
                dibujarParticulas(g, 480, 250, alphaConsumo, Color.ORANGE);
            }
            
            g.setColor(new Color(20, 30, 70));
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            g.drawString("Productor: " + productorStatus, 30, 320);
            g.drawString("Consumidor: " + consumidorStatus, 500, 320);
        }
        
        private void dibujarProductor(Graphics2D g, int x, int y) {
            g.setColor(new Color(255, 220, 180));
            g.fillOval(x + 20, y, 40, 40);
            g.setColor(Color.BLACK);
            g.fillOval(x + 28, y + 15, 5, 5);
            g.fillOval(x + 47, y + 15, 5, 5);
            g.drawArc(x + 30, y + 20, 20, 15, 0, -180);
            g.setColor(new Color(0, 100, 200));
            g.fillRect(x + 25, y + 40, 30, 50);
            double angRad = Math.toRadians(anguloProductor);
            int brazoX = (int)(Math.cos(angRad) * 15);
            int brazoY = (int)(Math.sin(angRad) * 10);
            g.setStroke(new BasicStroke(4));
            g.drawLine(x + 25, y + 50, x + 10 + brazoX, y + 60 + brazoY);
            g.drawLine(x + 55, y + 50, x + 70 - brazoX, y + 60 - brazoY);
            g.drawLine(x + 30, y + 90, x + 25, y + 120);
            g.drawLine(x + 50, y + 90, x + 55, y + 120);
        }
        
        private void dibujarTanque(Graphics2D g, int x, int y, int w, int h) {
            g.setPaint(new GradientPaint(x, y, new Color(210, 210, 220),
                                          x + w, y + h, new Color(180, 190, 210)));
            g.fillRoundRect(x, y, w, h, 18, 18);
            g.setColor(new Color(70, 70, 80));
            g.setStroke(new BasicStroke(3));
            g.drawRoundRect(x, y, w, h, 18, 18);
            
            int nivel = problema.getNivel();
            int capacidad = problema.getCapacidad();
            int waterH = (int) ((double) nivel / capacidad * (h - 10));
            
            if (waterH > 0) {
                GradientPaint water = new GradientPaint(x, y + h - waterH,
                                                         new Color(140, 200, 255),
                                                         x, y + h, new Color(20, 120, 220));
                g.setPaint(water);
                
                GeneralPath waveShape = new GeneralPath();
                waveShape.moveTo(x + 6, y + h - waterH);
                for (int i = 0; i <= w - 12; i += 10) {
                    double waveY = y + h - waterH + Math.sin((i + anguloProductor) * 0.1) * 3;
                    waveShape.lineTo(x + 6 + i, waveY);
                }
                waveShape.lineTo(x + w - 6, y + h - 6);
                waveShape.lineTo(x + 6, y + h - 6);
                waveShape.closePath();
                
                Shape clip = new RoundRectangle2D.Float(x + 6, y + 6, w - 12, h - 12, 12, 12);
                Shape currentClip = g.getClip();
                g.setClip(clip);
                g.fill(waveShape);
                g.setClip(currentClip);
            }
            
            g.setColor(Color.BLACK);
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.drawString("Nivel: " + nivel + "/" + capacidad, x + 20, y + h + 30);
            
            g.setColor(new Color(100, 100, 100));
            for (int i = 0; i <= 10; i++) {
                int markY = y + h - (i * h / 10);
                g.drawLine(x - 5, markY, x, markY);
                if (i % 2 == 0) {
                    g.setFont(new Font("SansSerif", Font.PLAIN, 10));
                    g.drawString(String.valueOf(i * capacidad / 10), x - 30, markY + 3);
                }
            }
        }
        
        private void dibujarConsumidor(Graphics2D g, int x, int y) {
            g.setColor(new Color(255, 220, 180));
            g.fillOval(x + 20, y, 40, 40);
            g.setColor(Color.BLACK);
            g.fillOval(x + 28, y + 15, 5, 5);
            g.fillOval(x + 47, y + 15, 5, 5);
            g.fillOval(x + 37, y + 25, 8, 8);
            g.setColor(new Color(200, 50, 50));
            g.fillRect(x + 25, y + 40, 30, 50);
            double angRad = Math.toRadians(anguloConsumidor);
            int brazoX = (int)(Math.cos(angRad) * 12);
            int brazoY = (int)(Math.sin(angRad) * 8);
            g.setStroke(new BasicStroke(4));
            g.drawLine(x + 25, y + 50, x + 10 + brazoX, y + 60 + brazoY);
            g.drawLine(x + 55, y + 50, x + 70 - brazoX, y + 60 - brazoY);
            g.drawLine(x + 30, y + 90, x + 25, y + 120);
            g.drawLine(x + 50, y + 90, x + 55, y + 120);
        }
        
        private void dibujarParticulas(Graphics2D g, int x, int y, float alpha, Color color) {
            Composite originalComposite = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            Random rand = new Random(System.currentTimeMillis());
            for (int i = 0; i < 10; i++) {
                int px = x + rand.nextInt(60) - 30;
                int py = y + rand.nextInt(60) - 30;
                int size = rand.nextInt(8) + 3;
                g.setColor(color);
                g.fillOval(px, py, size, size);
            }
            g.setComposite(originalComposite);
        }
    }

    // ============================================================================
    // FILÃ“SOFOS - TODAS LAS TÃ‰CNICAS
    // ============================================================================
    
    static class FilosofosBase implements ProblemaSimulacion {
        protected final int numFilosofos = 5;
        protected PanelFilosofos panelVisual;
        protected Thread[] filosofos;
        protected volatile boolean ejecutando = false;
        protected volatile String[] estados;
        protected volatile boolean[] tenedoresOcupados;
        protected ProyectoPCyPoto2025 parent;
        
        public FilosofosBase(ProyectoPCyPoto2025 parent) {
            this.parent = parent;
            this.estados = new String[numFilosofos];
            this.tenedoresOcupados = new boolean[numFilosofos];
            Arrays.fill(estados, "PENSANDO");
            Arrays.fill(tenedoresOcupados, false);
            this.panelVisual = new PanelFilosofos(this);
        }
        
        @Override
        public JPanel getPanelVisual() { return panelVisual; }
        
        @Override
        public Thread[] getHilos() { return filosofos; }
        
        @Override
        public void detenerSimulacion() {
            ejecutando = false;
            if (filosofos != null) {
                for (Thread filosofo : filosofos) {
                    if (filosofo != null) filosofo.interrupt();
                }
            }
        }
        
        @Override
        public void notificarEventoGrafo(String evento, String detalles) {
            if (parent != null) {
                parent.resaltarNodoGrafo(evento, detalles);
            }
        }
        
        public String[] getEstados() { return estados; }
        public boolean[] getTenedoresOcupados() { return tenedoresOcupados; }
        public int getNumFilosofos() { return numFilosofos; }
        
        @Override
        public void iniciarSimulacion() {} // Implementado en subclases
    }
    
    static class FilosofosSemaforo extends FilosofosBase {
        private final Semaphore[] tenedores;
        private final Semaphore mutex;
        
        public FilosofosSemaforo(ProyectoPCyPoto2025 parent) {
            super(parent);
            this.tenedores = new Semaphore[numFilosofos];
            for (int i = 0; i < numFilosofos; i++) {
                tenedores[i] = new Semaphore(1);
            }
            this.mutex = new Semaphore(1);
        }
        
        @Override
        public void iniciarSimulacion() {
            ejecutando = true;
            filosofos = new Thread[numFilosofos];
            
            for (int i = 0; i < numFilosofos; i++) {
                final int id = i;
                filosofos[i] = new Thread(() -> {
                    while (ejecutando && !Thread.currentThread().isInterrupted()) {
                        try {
                            estados[id] = "PENSANDO";
                            notificarEventoGrafo("F" + id, "Pensando");
                            panelVisual.actualizarEstado(id, estados[id]);
                            Thread.sleep(ThreadLocalRandom.current().nextInt(1000, 2000));
                           
                            estados[id] = "HAMBRIENTO";
                            notificarEventoGrafo("F" + id, "Hambriento");
                            panelVisual.actualizarEstado(id, estados[id]);
         
                            mutex.acquire();
                            tenedores[id].acquire();
                            tenedoresOcupados[id] = true;
                            notificarEventoGrafo("T" + id, "Ocupado");
                            tenedores[(id + 1) % numFilosofos].acquire();
                            tenedoresOcupados[(id + 1) % numFilosofos] = true;
                            notificarEventoGrafo("T" + ((id + 1) % numFilosofos), "Ocupado");
                            mutex.release();
                            
                            estados[id] = "COMIENDO";
                            notificarEventoGrafo("F" + id, "Comiendo");
                            panelVisual.actualizarEstado(id, estados[id]);
                            Thread.sleep(ThreadLocalRandom.current().nextInt(1000, 2000));
                            
                            tenedores[id].release();
                            tenedoresOcupados[id] = false;
                            notificarEventoGrafo("T" + id, "Libre");
                            tenedores[(id + 1) % numFilosofos].release();
                            tenedoresOcupados[(id + 1) % numFilosofos] = false;
                            notificarEventoGrafo("T" + ((id + 1) % numFilosofos), "Libre");
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                });
                filosofos[i].start();
            }
        }
    }
    
    static class FilosofosCondition extends FilosofosBase {
        private final Lock lock = new ReentrantLock();
        private final Condition[] condiciones;
        
        public FilosofosCondition(ProyectoPCyPoto2025 parent) {
            super(parent);
            condiciones = new Condition[numFilosofos];
            for (int i = 0; i < numFilosofos; i++) {
                condiciones[i] = lock.newCondition();
            }
        }
        
        private boolean puedeComer(int id) {
            return !tenedoresOcupados[id] && !tenedoresOcupados[(id + 1) % numFilosofos];
        }
        
        @Override
        public void iniciarSimulacion() {
            ejecutando = true;
            filosofos = new Thread[numFilosofos];
            
            for (int i = 0; i < numFilosofos; i++) {
                final int id = i;
                filosofos[i] = new Thread(() -> {
                    while (ejecutando && !Thread.currentThread().isInterrupted()) {
                        try {
                            estados[id] = "PENSANDO";
                            notificarEventoGrafo("F" + id, "Pensando");
                            panelVisual.actualizarEstado(id, estados[id]);
                            Thread.sleep(ThreadLocalRandom.current().nextInt(1000, 2000));
                           
                            lock.lock();
                            try {
                                estados[id] = "HAMBRIENTO";
                                notificarEventoGrafo("F" + id, "Hambriento");
                                panelVisual.actualizarEstado(id, estados[id]);
                                
                                while (!puedeComer(id)) {
                                    condiciones[id].await();
                                }
                               
                                tenedoresOcupados[id] = true;
                                notificarEventoGrafo("T" + id, "Ocupado");
                                tenedoresOcupados[(id + 1) % numFilosofos] = true;
                                notificarEventoGrafo("T" + ((id + 1) % numFilosofos), "Ocupado");
                                estados[id] = "COMIENDO";
                                notificarEventoGrafo("F" + id, "Comiendo");
                                panelVisual.actualizarEstado(id, estados[id]);
                            } finally {
                                lock.unlock();
                            }
                            
                            Thread.sleep(ThreadLocalRandom.current().nextInt(1000, 2000));
                            
                            lock.lock();
                            try {
                                tenedoresOcupados[id] = false;
                                notificarEventoGrafo("T" + id, "Libre");
                                tenedoresOcupados[(id + 1) % numFilosofos] = false;
                                notificarEventoGrafo("T" + ((id + 1) % numFilosofos), "Libre");
                                condiciones[(id - 1 + numFilosofos) % numFilosofos].signal();
                                condiciones[(id + 1) % numFilosofos].signal();
                            } finally {
                                lock.unlock();
                            }
                            
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                });
                filosofos[i].start();
            }
        }
    }
    
    static class FilosofosMonitor extends FilosofosBase {
        private final Object monitor = new Object();
        
        public FilosofosMonitor(ProyectoPCyPoto2025 parent) {
            super(parent);
        }
        
        private boolean puedeComer(int id) {
            return !tenedoresOcupados[id] && !tenedoresOcupados[(id + 1) % numFilosofos];
        }
        
        @Override
        public void iniciarSimulacion() {
            // ESTA LÃ“GICA FUE CORREGIDA Y MOVIDA DESDE ProductorConsumidorBase
            ejecutando = true;
            filosofos = new Thread[numFilosofos];
            
            for (int i = 0; i < numFilosofos; i++) {
                final int id = i;
                filosofos[i] = new Thread(() -> {
                    while (ejecutando && !Thread.currentThread().isInterrupted()) {
                        try {
                            estados[id] = "PENSANDO";
                            notificarEventoGrafo("F" + id, "Pensando");
                            panelVisual.actualizarEstado(id, estados[id]);
                            Thread.sleep(ThreadLocalRandom.current().nextInt(1000, 2000));
                           
                            synchronized (monitor) {
                                estados[id] = "HAMBRIENTO";
                                notificarEventoGrafo("F" + id, "Hambriento");
                                panelVisual.actualizarEstado(id, estados[id]);
                                
                                while (!puedeComer(id)) {
                                    monitor.wait();
                                }
                                
                                tenedoresOcupados[id] = true;
                                notificarEventoGrafo("T" + id, "Ocupado");
                                tenedoresOcupados[(id + 1) % numFilosofos] = true;
                                notificarEventoGrafo("T" + ((id + 1) % numFilosofos), "Ocupado");
                                estados[id] = "COMIENDO";
                                notificarEventoGrafo("F" + id, "Comiendo");
                                panelVisual.actualizarEstado(id, estados[id]);
                            }
                            
                            Thread.sleep(ThreadLocalRandom.current().nextInt(1000, 2000));
                            
                            synchronized (monitor) {
                                tenedoresOcupados[id] = false;
                                notificarEventoGrafo("T" + id, "Libre");
                                tenedoresOcupados[(id + 1) % numFilosofos] = false;
                                notificarEventoGrafo("T" + ((id + 1) % numFilosofos), "Libre");
                                monitor.notifyAll();
                            }
                            
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                });
                filosofos[i].start();
            }
        }
    }

    static class PanelFilosofos extends JPanel {
        private final FilosofosBase cena;
        private final Color[] coloresFilosofos = {
            new Color(255, 100, 100), new Color(100, 255, 100),
            new Color(100, 100, 255), new Color(255, 255, 100),
            new Color(255, 100, 255)
        };
        private int[] angulosBoca = new int[5];
        private javax.swing.Timer animationTimer;
        
        public PanelFilosofos(FilosofosBase cena) {
            this.cena = cena;
            setBackground(new Color(240, 245, 255));
            setPreferredSize(new Dimension(750, 650));
            
            animationTimer = new javax.swing.Timer(100, e -> {
                for (int i = 0; i < 5; i++) {
                    if ("COMIENDO".equals(cena.getEstados()[i])) {
                        angulosBoca[i] = (angulosBoca[i] + 20) % 360;
                    }
                }
                repaint();
            });
            animationTimer.start();
        }
        
        public void actualizarEstado(int filosofo, String estado) {
            SwingUtilities.invokeLater(this::repaint);
        }
        
        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int centroX = getWidth() / 2;
            int centroY = getHeight() / 2;
            int radio = 150;
            int numFilosofos = cena.getNumFilosofos();
            
            dibujarMesa(g, centroX, centroY, radio);
            
            for (int i = 0; i < numFilosofos; i++) {
                double angulo = 2 * Math.PI * i / numFilosofos;
                int px = (int) (centroX + (radio - 30) * Math.cos(angulo));
                int py = (int) (centroY + (radio - 30) * Math.sin(angulo));
                
                g.setColor(Color.WHITE);
                g.fillOval(px - 15, py - 15, 30, 30);
                g.setColor(Color.GRAY);
                g.drawOval(px - 15, py - 15, 30, 30);
            }
            
            for (int i = 0; i < numFilosofos; i++) {
                double angulo = 2 * Math.PI * i / numFilosofos;
                int x = (int) (centroX + (radio + 80) * Math.cos(angulo));
                int y = (int) (centroY + (radio + 80) * Math.sin(angulo));
                
                dibujarFilosofo(g, x, y, i, cena.getEstados()[i]);
            }
            
            for (int i = 0; i < numFilosofos; i++) {
                double angulo = 2 * Math.PI * (i + 0.5) / numFilosofos;
                int tx = (int) (centroX + (radio) * Math.cos(angulo));
                int ty = (int) (centroY + (radio) * Math.sin(angulo));
                dibujarTenedor(g, tx, ty, cena.getTenedoresOcupados()[i], angulo);
            }
            
            g.setColor(Color.BLUE.darker());
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.drawString("Cena de los FilÃ³sofos", 20, 30);
        }
        
        private void dibujarMesa(Graphics2D g, int cx, int cy, int radio) {
            g.setColor(new Color(100, 100, 100, 50));
            g.fillOval(cx - radio - 5, cy - radio - 5, radio * 2 + 10, radio * 2 + 10);
            GradientPaint mesaGrad = new GradientPaint(
                cx - radio, cy - radio, new Color(210, 180, 140),
                cx + radio, cy + radio, new Color(160, 130, 90)
            );
            g.setPaint(mesaGrad);
            g.fillOval(cx - radio, cy - radio, radio * 2, radio * 2);
            
            g.setColor(new Color(139, 69, 19));
            g.setStroke(new BasicStroke(4));
            g.drawOval(cx - radio, cy - radio, radio * 2, radio * 2);
        }
        
        private void dibujarFilosofo(Graphics2D g, int x, int y, int id, String estado) {
            Color colorFilosofo = coloresFilosofos[id];
            if ("COMIENDO".equals(estado)) {
                colorFilosofo = colorFilosofo.brighter();
            } else if ("HAMBRIENTO".equals(estado)) {
                colorFilosofo = colorFilosofo.darker();
            }
            
            g.setColor(colorFilosofo);
            g.fillOval(x - 25, y - 15, 50, 40);
            
            g.setColor(new Color(255, 220, 180));
            g.fillOval(x - 20, y - 35, 40, 40);
            g.setColor(Color.BLACK);
            g.fillOval(x - 12, y - 25, 6, 6);
            g.fillOval(x + 6, y - 25, 6, 6);
            
            if ("COMIENDO".equals(estado)) {
                int apertura = (int)(Math.abs(Math.sin(Math.toRadians(angulosBoca[id]))) * 10);
                g.fillOval(x - 5, y - 10, 10, 5 + apertura);
            } else {
                g.drawArc(x - 8, y - 12, 16, 10, 0, -180);
            }
            
            g.setColor(Color.BLACK);
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            g.drawString("F" + id, x - 8, y + 40);
            
            g.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g.drawString(estado, x - 30, y + 55);
        }
        
        private void dibujarTenedor(Graphics2D g, int x, int y, boolean ocupado, double angulo) {
            g.setStroke(new BasicStroke(3));
            if (ocupado) {
                g.setColor(Color.RED);
            } else {
                g.setColor(new Color(180, 180, 180));
            }
            
            AffineTransform oldTransform = g.getTransform();
            g.translate(x, y);
            g.rotate(angulo + Math.PI / 2);
            
            g.fillRect(-2, -20, 4, 25);
            g.fillRect(-6, -20, 3, 8);
            g.fillRect(-1, -20, 3, 8);
            g.fillRect(4, -20, 3, 8);
            
            g.setTransform(oldTransform);
        }
    }

    // ============================================================================
    // BARBERO DORMILÃ“N - TODAS LAS TÃ‰CNICAS
    // ============================================================================
    
    static class BarberoBase implements ProblemaSimulacion {
        protected final int numSillas = 3;
        protected int sillasOcupadas = 0;
        protected PanelBarbero panelVisual;
        protected Thread hiloBarbero;
        protected Thread[] hilosClientes;
        protected volatile boolean ejecutando = false;
        protected volatile String estadoBarbero = "DURMIENDO";
        protected volatile int[] estadoSillas;
        protected ProyectoPCyPoto2025 parent;
        
        public BarberoBase(ProyectoPCyPoto2025 parent) {
            this.parent = parent;
            this.estadoSillas = new int[numSillas];
            Arrays.fill(estadoSillas, 0);
            this.panelVisual = new PanelBarbero(this);
        }
        
        @Override
        public JPanel getPanelVisual() { return panelVisual; }
        
        @Override
        public Thread[] getHilos() {
            Thread[] todos = new Thread[1 + (hilosClientes != null ? hilosClientes.length : 0)];
            todos[0] = hiloBarbero;
            if (hilosClientes != null) {
                System.arraycopy(hilosClientes, 0, todos, 1, hilosClientes.length);
            }
            return todos;
        }
        
        @Override
        public void detenerSimulacion() {
            ejecutando = false;
            if (hiloBarbero != null) hiloBarbero.interrupt();
            if (hilosClientes != null) {
                for (Thread cliente : hilosClientes) {
                    if (cliente != null) cliente.interrupt();
                }
            }
        }
        
        @Override
        public void notificarEventoGrafo(String evento, String detalles) {
            if (parent != null) {
                parent.resaltarNodoGrafo(evento, detalles);
            }
        }
        
        public String getEstadoBarbero() { return estadoBarbero; }
        public int[] getEstadoSillas() { return estadoSillas; }
        public int getNumSillas() { return numSillas; }
        
        @Override
        public void iniciarSimulacion() {} // Implementado en subclases
    }
    
    static class BarberoSemaforo extends BarberoBase {
        private final Semaphore barbero = new Semaphore(0);
        private final Semaphore clientes = new Semaphore(0);
        private final Semaphore mutex = new Semaphore(1);
        
        public BarberoSemaforo(ProyectoPCyPoto2025 parent) {
            super(parent);
        }
        
        @Override
        public void iniciarSimulacion() {
            ejecutando = true;
            hiloBarbero = new Thread(() -> {
                while (ejecutando && !Thread.currentThread().isInterrupted()) {
                    try {
                        estadoBarbero = "DURMIENDO (ZZZ)";
                        notificarEventoGrafo("Barbero", "Durmiendo");
                        panelVisual.actualizarEstado();
                        clientes.acquire();
                        
                        mutex.acquire();
                        sillasOcupadas--;
                        estadoBarbero = "CORTANDO CABELLO";
                        notificarEventoGrafo("Barbero", "Cortando");
                        panelVisual.actualizarEstado();
                        mutex.release();
                        
                        barbero.release();
                        Thread.sleep(ThreadLocalRandom.current().nextInt(2000, 4000));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            
            hilosClientes = new Thread[10];
            for (int i = 0; i < 10; i++) {
                final int id = i;
                hilosClientes[i] = new Thread(() -> {
                    try {
                        Thread.sleep(ThreadLocalRandom.current().nextInt(1000, 3000) * (id + 1));
                        
                        mutex.acquire();
                        if (sillasOcupadas < numSillas) {
                            sillasOcupadas++;
                            notificarEventoGrafo("Sillas", "Cliente esperando");
                            for (int j = 0; j < numSillas; j++) {
                                if (estadoSillas[j] == 0) {
                                    estadoSillas[j] = 1;
                                    break;
                                }
                            }
                            clientes.release();
                            mutex.release();
                            
                            barbero.acquire();
                            for (int j = 0; j < numSillas; j++) {
                                if (estadoSillas[j] == 1) {
                                    estadoSillas[j] = 2;
                                    break;
                                }
                            }
                            notificarEventoGrafo("Cliente", "Siendo atendido");
                            panelVisual.actualizarEstado();
                            Thread.sleep(1000);
                            
                            mutex.acquire();
                            for (int j = 0; j < numSillas; j++) {
                                if (estadoSillas[j] == 2) {
                                    estadoSillas[j] = 0;
                                    break;
                                }
                            }
                            mutex.release();
                        } else {
                            notificarEventoGrafo("Cliente", "Se fue (lleno)");
                            mutex.release();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            
            hiloBarbero.start();
            for (Thread cliente : hilosClientes) {
                cliente.start();
            }
        }
    }
    
    static class BarberoCondition extends BarberoBase {
        private final Lock lock = new ReentrantLock();
        private final Condition hayClientes = lock.newCondition();
        private final Condition barberoListo = lock.newCondition();
        
        public BarberoCondition(ProyectoPCyPoto2025 parent) {
            super(parent);
        }
        
        @Override
        public void iniciarSimulacion() {
            ejecutando = true;
            hiloBarbero = new Thread(() -> {
                while (ejecutando && !Thread.currentThread().isInterrupted()) {
                    try {
                        lock.lock();
                        try {
                            while (sillasOcupadas == 0) {
                                estadoBarbero = "DURMIENDO (ZZZ)";
                                notificarEventoGrafo("Barbero", "Durmiendo");
                                panelVisual.actualizarEstado();
                                hayClientes.await();
                            }
                            sillasOcupadas--;
                            estadoBarbero = "CORTANDO CABELLO";
                            notificarEventoGrafo("Barbero", "Cortando");
                            panelVisual.actualizarEstado();
                            barberoListo.signal();
                        } finally {
                            lock.unlock();
                        }
                        Thread.sleep(ThreadLocalRandom.current().nextInt(2000, 4000));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            
            hilosClientes = new Thread[10];
            for (int i = 0; i < 10; i++) {
                final int id = i;
                hilosClientes[i] = new Thread(() -> {
                    try {
                        Thread.sleep(ThreadLocalRandom.current().nextInt(1000, 3000) * (id + 1));
                        
                        lock.lock();
                        try {
                            if (sillasOcupadas < numSillas) {
                                sillasOcupadas++;
                                notificarEventoGrafo("Sillas", "Cliente esperando");
                                for (int j = 0; j < numSillas; j++) {
                                    if (estadoSillas[j] == 0) {
                                        estadoSillas[j] = 1;
                                        break;
                                    }
                                }
                                hayClientes.signal();
                                barberoListo.await();
                                
                                for (int j = 0; j < numSillas; j++) {
                                    if (estadoSillas[j] == 1) {
                                        estadoSillas[j] = 2;
                                        break;
                                    }
                                }
                                notificarEventoGrafo("Cliente", "Siendo atendido");
                                panelVisual.actualizarEstado();
                            } else {
                                notificarEventoGrafo("Cliente", "Se fue (lleno)");
                            }
                        } finally {
                            lock.unlock();
                        }
                        
                        Thread.sleep(1000);
                        
                        lock.lock();
                        try {
                            for (int j = 0; j < numSillas; j++) {
                                if (estadoSillas[j] == 2) {
                                    estadoSillas[j] = 0;
                                    break;
                                }
                            }
                        } finally {
                            lock.unlock();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            
            hiloBarbero.start();
            for (Thread cliente : hilosClientes) {
                cliente.start();
            }
        }
    }
    
    static class BarberoMonitor extends BarberoBase {
        private final Object monitor = new Object();
        
        public BarberoMonitor(ProyectoPCyPoto2025 parent) {
            super(parent);
        }
        
        @Override
        public void iniciarSimulacion() {
            ejecutando = true;
            hiloBarbero = new Thread(() -> {
                while (ejecutando && !Thread.currentThread().isInterrupted()) {
                    try {
                        synchronized (monitor) {
                            while (sillasOcupadas == 0) {
                                estadoBarbero = "DURMIENDO (ZZZ)";
                                notificarEventoGrafo("Barbero", "Durmiendo");
                                panelVisual.actualizarEstado();
                                monitor.wait();
                            }
                            sillasOcupadas--;
                            estadoBarbero = "CORTANDO CABELLO";
                            notificarEventoGrafo("Barbero", "Cortando");
                            panelVisual.actualizarEstado();
                            monitor.notifyAll();
                        }
                        Thread.sleep(ThreadLocalRandom.current().nextInt(2000, 4000));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            
            hilosClientes = new Thread[10];
            for (int i = 0; i < 10; i++) {
                final int id = i;
                hilosClientes[i] = new Thread(() -> {
                    try {
                        Thread.sleep(ThreadLocalRandom.current().nextInt(1000, 3000) * (id + 1));
                        
                        synchronized (monitor) {
                            if (sillasOcupadas < numSillas) {
                                sillasOcupadas++;
                                notificarEventoGrafo("Sillas", "Cliente esperando");
                                for (int j = 0; j < numSillas; j++) {
                                    if (estadoSillas[j] == 0) {
                                        estadoSillas[j] = 1;
                                        break;
                                    }
                                }
                                monitor.notifyAll();
                                monitor.wait();
                                
                                for (int j = 0; j < numSillas; j++) {
                                    if (estadoSillas[j] == 1) {
                                        estadoSillas[j] = 2;
                                        break;
                                    }
                                }
                                notificarEventoGrafo("Cliente", "Siendo atendido");
                                panelVisual.actualizarEstado();
                            } else {
                                notificarEventoGrafo("Cliente", "Se fue (lleno)");
                            }
                        }
                        
                        Thread.sleep(1000);
                        
                        synchronized (monitor) {
                            for (int j = 0; j < numSillas; j++) {
                                if (estadoSillas[j] == 2) {
                                    estadoSillas[j] = 0;
                                    break;
                                }
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            
            hiloBarbero.start();
            for (Thread cliente : hilosClientes) {
                cliente.start();
            }
        }
    }

    static class PanelBarbero extends JPanel {
        private final BarberoBase barbero;
        private int animacionTijeras = 0;
        private int zzz_offset = 0;
        private javax.swing.Timer animationTimer;
        
        public PanelBarbero(BarberoBase barbero) {
            this.barbero = barbero;
            setBackground(new Color(240, 245, 255));
            setPreferredSize(new Dimension(750, 650));
            
            animationTimer = new javax.swing.Timer(100, e -> {
                animacionTijeras = (animacionTijeras + 1) % 20;
                zzz_offset = (zzz_offset + 1) % 30;
                repaint();
            });
            animationTimer.start();
        }
        
        public void actualizarEstado() {
            SwingUtilities.invokeLater(this::repaint);
        }
        
        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            dibujarBarberia(g);
            dibujarBarbero(g, 120, 150);
            dibujarSillasEspera(g);
            dibujarSillaCorte(g, 350, 200);
            
            g.setColor(Color.BLUE.darker());
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.drawString("Barbero DormilÃ³n", 20, 30);
        }
        
        private void dibujarBarberia(Graphics2D g) {
            g.setColor(new Color(200, 200, 200));
            g.fillRect(0, getHeight() - 100, getWidth(), 100);
            g.setColor(new Color(150, 150, 150));
            for (int i = 0; i < getWidth(); i += 40) {
                g.drawLine(i, getHeight() - 100, i, getHeight());
            }
            g.setColor(new Color(230, 230, 200));
            g.fillRect(0, 0, getWidth(), getHeight() - 100);
            g.setColor(new Color(200, 220, 255));
            g.fillRect(300, 50, 200, 250);
            g.setColor(new Color(100, 100, 100));
            g.drawRect(300, 50, 200, 250);
        }
        
        private void dibujarBarbero(Graphics2D g, int x, int y) {
            String estado = barbero.getEstadoBarbero();
            g.setColor(Color.WHITE);
            g.fillRect(x, y, 40, 60);
            g.setColor(new Color(255, 220, 180));
            g.fillOval(x + 5, y - 30, 30, 30);
            g.setColor(new Color(80, 50, 20));
            g.fillArc(x + 5, y - 35, 30, 25, 0, 180);
            
            if (estado.contains("DURMIENDO")) {
                g.setColor(Color.BLACK);
                g.setStroke(new BasicStroke(2));
                g.drawLine(x + 12, y - 18, x + 18, y - 18);
                g.drawLine(x + 22, y - 18, x + 28, y - 18);
                g.setFont(new Font("SansSerif", Font.BOLD, 20));
                float alpha = (float)(Math.sin(zzz_offset * 0.2) * 0.3 + 0.7);
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g.drawString("Z", x + 50, y - 40 + (zzz_offset % 10));
                g.drawString("Z", x + 60, y - 50 + (zzz_offset % 15));
                g.drawString("Z", x + 70, y - 60 + (zzz_offset % 20));
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            } else {
                g.setColor(Color.WHITE);
                g.fillOval(x + 10, y - 22, 8, 8);
                g.fillOval(x + 22, y - 22, 8, 8);
                g.setColor(Color.BLACK);
                g.fillOval(x + 13, y - 19, 3, 3);
                g.fillOval(x + 25, y - 19, 3, 3);
                if (estado.contains("CORTANDO")) {
                    dibujarTijeras(g, x + 50, y + 10, animacionTijeras);
                }
            }
            
            g.setColor(new Color(80, 50, 20));
            g.fillArc(x + 10, y - 12, 10, 6, 0, 180);
            g.fillArc(x + 20, y - 12, 10, 6, 0, 180);
            g.setColor(new Color(255, 220, 180));
            g.setStroke(new BasicStroke(8));
            g.drawLine(x, y + 20, x - 20, y + 40);
            g.drawLine(x + 40, y + 20, x + 60, y + 40);
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(10));
            g.drawLine(x + 10, y + 60, x + 10, y + 90);
            g.drawLine(x + 30, y + 60, x + 30, y + 90);
            g.setColor(Color.BLUE);
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            g.drawString(estado, x - 20, y + 110);
        }
        
        private void dibujarTijeras(Graphics2D g, int x, int y, int anim) {
            g.setColor(new Color(180, 180, 180));
            g.setStroke(new BasicStroke(2));
            int apertura = (int)(Math.sin(anim * 0.3) * 10);
            int[] x1 = {x, x - 5 - apertura, x - 10 - apertura};
            int[] y1 = {y, y - 20, y - 15};
            g.fillPolygon(x1, y1, 3);
            int[] x2 = {x, x + 5 + apertura, x + 10 + apertura};
            int[] y2 = {y, y - 20, y - 15};
            g.fillPolygon(x2, y2, 3);
            g.setColor(Color.DARK_GRAY);
            g.fillOval(x - 3, y - 3, 6, 6);
        }
        
        private void dibujarSillasEspera(Graphics2D g) {
            g.setColor(Color.BLACK);
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            g.drawString("Sala de Espera:", 50, 400);
            int[] estadoSillas = barbero.getEstadoSillas();
            for (int i = 0; i < barbero.getNumSillas(); i++) {
                int x = 50 + i * 120;
                int y = 420;
                dibujarSilla(g, x, y, estadoSillas[i]);
                g.setFont(new Font("SansSerif", Font.PLAIN, 11));
                g.drawString("Silla " + (i + 1), x + 15, y + 80);
            }
        }
        
        private void dibujarSilla(Graphics2D g, int x, int y, int estado) {
            if (estado == 0) {
                g.setColor(new Color(100, 200, 100));
            } else if (estado == 1) {
                g.setColor(new Color(255, 200, 0));
            } else {
                g.setColor(new Color(255, 100, 100));
            }
            g.fillRoundRect(x, y, 60, 10, 5, 5);
            g.fillRect(x, y + 10, 60, 40);
            g.setColor(new Color(80, 50, 20));
            g.fillRect(x + 5, y + 50, 8, 20);
            g.fillRect(x + 47, y + 50, 8, 20);
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(2));
            g.drawRoundRect(x, y, 60, 50, 5, 5);
            if (estado == 1) {
                dibujarClienteMini(g, x + 15, y + 15);
            }
        }
        
        private void dibujarSillaCorte(Graphics2D g, int x, int y) {
            g.setColor(new Color(100, 100, 100));
            g.fillRect(x + 20, y + 60, 30, 40);
            g.setColor(new Color(150, 50, 50));
            g.fillRect(x, y + 40, 70, 20);
            g.fillRect(x + 10, y, 50, 50);
            g.fillRect(x - 10, y + 40, 10, 30);
            g.fillRect(x + 70, y + 40, 10, 30);
            g.setColor(Color.BLACK);
            g.drawString("Ãrea de Corte", x - 10, y + 120);
        }
        
        private void dibujarClienteMini(Graphics2D g, int x, int y) {
            g.setColor(new Color(255, 220, 180));
            g.fillOval(x, y, 20, 20);
            g.setColor(Color.BLACK);
            g.fillOval(x + 5, y + 8, 3, 3);
            g.fillOval(x + 12, y + 8, 3, 3);
        }
    }

    // ============================================================================
    // FUMADORES - TODAS LAS TÃ‰CNICAS
    // ============================================================================
    
    static class FumadoresBase implements ProblemaSimulacion {
        protected PanelFumadores panelVisual;
        protected Thread hiloAgente;
        protected Thread[] hilosFumadores;
        protected volatile boolean ejecutando = false;
        protected volatile String estadoAgente = "PREPARANDO";
        protected volatile String[] estadosFumadores;
        protected volatile boolean[] ingredientesDisponibles;
        protected volatile boolean cigarroEncendido = false;
        protected ProyectoPCyPoto2025 parent;
        
        public FumadoresBase(ProyectoPCyPoto2025 parent) {
            this.parent = parent;
            this.estadosFumadores = new String[3];
            this.ingredientesDisponibles = new boolean[3];
            Arrays.fill(estadosFumadores, "ESPERANDO");
            Arrays.fill(ingredientesDisponibles, false);
            this.panelVisual = new PanelFumadores(this);
        }
        
        @Override
        public JPanel getPanelVisual() { return panelVisual; }
        
        @Override
        public Thread[] getHilos() {
            Thread[] todos = new Thread[1 + (hilosFumadores != null ? hilosFumadores.length : 0)];
            todos[0] = hiloAgente;
            if (hilosFumadores != null) {
                System.arraycopy(hilosFumadores, 0, todos, 1, hilosFumadores.length);
            }
            return todos;
        }
        
        @Override
        public void detenerSimulacion() {
            ejecutando = false;
            if (hiloAgente != null) hiloAgente.interrupt();
            if (hilosFumadores != null) {
                for (Thread fumador : hilosFumadores) {
                    if (fumador != null) fumador.interrupt();
                }
            }
        }
        
        @Override
        public void notificarEventoGrafo(String evento, String detalles) {
            if (parent != null) {
                parent.resaltarNodoGrafo(evento, detalles);
            }
        }
        
        public String getEstadoAgente() { return estadoAgente; }
        public String[] getEstadosFumadores() { return estadosFumadores; }
        public boolean[] getIngredientesDisponibles() { return ingredientesDisponibles; }
        public boolean isCigarroEncendido() { return cigarroEncendido; }
        
        @Override
        public void iniciarSimulacion() {} // Implementado en subclases
        
        protected String getNombreIngrediente(int id) {
            switch(id) {
                case 0: return "Papel";
                case 1: return "Tabaco";
                case 2: return "Cerillos";
                default: return "";
            }
        }
    }
    
    static class FumadoresSemaforo extends FumadoresBase {
        private final Semaphore agente = new Semaphore(1);
        private final Semaphore[] fumadores;
        private final Semaphore[] ingredientes;
        
        public FumadoresSemaforo(ProyectoPCyPoto2025 parent) {
            super(parent);
            this.fumadores = new Semaphore[3];
            this.ingredientes = new Semaphore[3];
            for (int i = 0; i < 3; i++) {
                fumadores[i] = new Semaphore(0);
                ingredientes[i] = new Semaphore(0);
            }
        }
        
        @Override
        public void iniciarSimulacion() {
            ejecutando = true;
            hiloAgente = new Thread(() -> {
                while (ejecutando && !Thread.currentThread().isInterrupted()) {
                    try {
                        agente.acquire();
                        estadoAgente = "PREPARANDO INGREDIENTES";
                        notificarEventoGrafo("Agente", "Preparando");
                        panelVisual.actualizarEstado();
                        Thread.sleep(1500);
                        
                        int ingredienteFaltante = ThreadLocalRandom.current().nextInt(3);
                        for (int i = 0; i < 3; i++) {
                            if (i != ingredienteFaltante) {
                                ingredientes[i].release();
                                ingredientesDisponibles[i] = true;
                                notificarEventoGrafo(getNombreIngrediente(i), "Disponible");
                            }
                        }
                        
                        estadoAgente = "INGREDIENTES LISTOS";
                        panelVisual.actualizarEstado();
                        fumadores[ingredienteFaltante].release();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            
            hilosFumadores = new Thread[3];
            for (int i = 0; i < 3; i++) {
                final int id = i;
                hilosFumadores[i] = new Thread(() -> {
                    while (ejecutando && !Thread.currentThread().isInterrupted()) {
                        try {
                            estadosFumadores[id] = "ESPERANDO INGREDIENTE";
                            notificarEventoGrafo("Fumador " + getNombreIngrediente(id).substring(0, 1), "Esperando");
                            panelVisual.actualizarEstado();
                            fumadores[id].acquire();
                          
                            estadosFumadores[id] = "TOMA INGREDIENTES";
                            panelVisual.actualizarEstado();
                            Thread.sleep(1000);
                           
                            for (int j = 0; j < 3; j++) {
                                if (j != id) {
                                    ingredientes[j].acquire();
                                    ingredientesDisponibles[j] = false;
                                    notificarEventoGrafo(getNombreIngrediente(j), "Tomado");
                                }
                            }
                            
                            estadosFumadores[id] = "ENCIENDE CIGARRO";
                            panelVisual.actualizarEstado();
                            cigarroEncendido = true;
                            Thread.sleep(2000);
                            
                            estadosFumadores[id] = "FUMANDO";
                            notificarEventoGrafo("Fumador " + getNombreIngrediente(id).substring(0, 1), "Fumando");
                            panelVisual.actualizarEstado();
                            Thread.sleep(3000);
                            
                            estadosFumadores[id] = "TERMINÃ“ DE FUMAR";
                            cigarroEncendido = false;
                            panelVisual.actualizarEstado();
                            Thread.sleep(1000);
                            
                            agente.release();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                });
            }
            
            hiloAgente.start();
            for (Thread fumador : hilosFumadores) {
                fumador.start();
            }
        }
    }
    
    static class FumadoresCondition extends FumadoresBase {
        private final Lock lock = new ReentrantLock();
        private final Condition agenteCondition = lock.newCondition();
        private final Condition[] fumadoresConditions;
        
        public FumadoresCondition(ProyectoPCyPoto2025 parent) {
            super(parent);
            fumadoresConditions = new Condition[3];
            for (int i = 0; i < 3; i++) {
                fumadoresConditions[i] = lock.newCondition();
            }
        }
        
        @Override
        public void iniciarSimulacion() {
            ejecutando = true;
            hiloAgente = new Thread(() -> {
                while (ejecutando && !Thread.currentThread().isInterrupted()) {
                    try {
                        lock.lock();
                        try {
                            estadoAgente = "PREPARANDO INGREDIENTES";
                            notificarEventoGrafo("Agente", "Preparando");
                            panelVisual.actualizarEstado();
                        } finally {
                            lock.unlock();
                        }
                        
                        Thread.sleep(1500);
                        
                        lock.lock();
                        try {
                            int ingredienteFaltante = ThreadLocalRandom.current().nextInt(3);
                            for (int i = 0; i < 3; i++) {
                                if (i != ingredienteFaltante) {
                                    ingredientesDisponibles[i] = true;
                                    notificarEventoGrafo(getNombreIngrediente(i), "Disponible");
                                }
                            }
                            
                            estadoAgente = "INGREDIENTES LISTOS";
                            panelVisual.actualizarEstado();
                            fumadoresConditions[ingredienteFaltante].signal();
                            agenteCondition.await();
                        } finally {
                            lock.unlock();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            
            hilosFumadores = new Thread[3];
            for (int i = 0; i < 3; i++) {
                final int id = i;
                hilosFumadores[i] = new Thread(() -> {
                    while (ejecutando && !Thread.currentThread().isInterrupted()) {
                        try {
                            lock.lock();
                            try {
                                estadosFumadores[id] = "ESPERANDO INGREDIENTE";
                                notificarEventoGrafo("Fumador " + getNombreIngrediente(id).substring(0, 1), "Esperando");
                                panelVisual.actualizarEstado();
                                fumadoresConditions[id].await();
                                
                                estadosFumadores[id] = "TOMA INGREDIENTES";
                                panelVisual.actualizarEstado();
                            } finally {
                                lock.unlock();
                            }
                            
                            Thread.sleep(1000);
                            
                            lock.lock();
                            try {
                                for (int j = 0; j < 3; j++) {
                                    if (j != id) {
                                        ingredientesDisponibles[j] = false;
                                        notificarEventoGrafo(getNombreIngrediente(j), "Tomado");
                                    }
                                }
                                estadosFumadores[id] = "ENCIENDE CIGARRO";
                                panelVisual.actualizarEstado();
                                cigarroEncendido = true;
                            } finally {
                                lock.unlock();
                            }
                            
                            Thread.sleep(2000);
                            
                            lock.lock();
                            try {
                                estadosFumadores[id] = "FUMANDO";
                                notificarEventoGrafo("Fumador " + getNombreIngrediente(id).substring(0, 1), "Fumando");
                                panelVisual.actualizarEstado();
                            } finally {
                                lock.unlock();
                            }
                            
                            Thread.sleep(3000);
                            
                            lock.lock();
                            try {
                                estadosFumadores[id] = "TERMINÃ“ DE FUMAR";
                                cigarroEncendido = false;
                                panelVisual.actualizarEstado();
                                agenteCondition.signal();
                            } finally {
                                lock.unlock();
                            }
                            
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                });
            }
            
            hiloAgente.start();
            for (Thread fumador : hilosFumadores) {
                fumador.start();
            }
        }
    }
    
    static class FumadoresMonitor extends FumadoresBase {
        private final Object monitor = new Object();
        
        public FumadoresMonitor(ProyectoPCyPoto2025 parent) {
            super(parent);
        }
        
        @Override
        public void iniciarSimulacion() {
            ejecutando = true;
            hiloAgente = new Thread(() -> {
                while (ejecutando && !Thread.currentThread().isInterrupted()) {
                    try {
                        synchronized (monitor) {
                            estadoAgente = "PREPARANDO INGREDIENTES";
                            notificarEventoGrafo("Agente", "Preparando");
                            panelVisual.actualizarEstado();
                        }
                        
                        Thread.sleep(1500);
                        
                        synchronized (monitor) {
                            int ingredienteFaltante = ThreadLocalRandom.current().nextInt(3);
                            for (int i = 0; i < 3; i++) {
                                if (i != ingredienteFaltante) {
                                    ingredientesDisponibles[i] = true;
                                    notificarEventoGrafo(getNombreIngrediente(i), "Disponible");
                                }
                            }
                            estadoAgente = "INGREDIENTES LISTOS";
                            panelVisual.actualizarEstado();
                            monitor.notifyAll();
                            monitor.wait();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            
            hilosFumadores = new Thread[3];
            for (int i = 0; i < 3; i++) {
                final int id = i;
                hilosFumadores[i] = new Thread(() -> {
                    while (ejecutando && !Thread.currentThread().isInterrupted()) {
                        try {
                            synchronized (monitor) {
                                estadosFumadores[id] = "ESPERANDO INGREDIENTE";
                                notificarEventoGrafo("Fumador " + getNombreIngrediente(id).substring(0, 1), "Esperando");
                                panelVisual.actualizarEstado();
                                
                                while (!puedeTomarIngredientes(id)) {
                                    monitor.wait();
                                }
                                
                                estadosFumadores[id] = "TOMA INGREDIENTES";
                                panelVisual.actualizarEstado();
                            }
                            
                            Thread.sleep(1000);
                            
                            synchronized (monitor) {
                                for (int j = 0; j < 3; j++) {
                                    if (j != id) {
                                        ingredientesDisponibles[j] = false;
                                        notificarEventoGrafo(getNombreIngrediente(j), "Tomado");
                                    }
                                }
                                estadosFumadores[id] = "ENCIENDE CIGARRO";
                                panelVisual.actualizarEstado();
                                cigarroEncendido = true;
                            }
                            
                            Thread.sleep(2000);
                            
                            synchronized (monitor) {
                                estadosFumadores[id] = "FUMANDO";
                                notificarEventoGrafo("Fumador " + getNombreIngrediente(id).substring(0, 1), "Fumando");
                                panelVisual.actualizarEstado();
                            }
                            
                            Thread.sleep(3000);
                            
                            synchronized (monitor) {
                                estadosFumadores[id] = "TERMINÃ“ DE FUMAR";
                                cigarroEncendido = false;
                                panelVisual.actualizarEstado();
                                monitor.notifyAll();
                            }
                            
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                });
            }
            
            hiloAgente.start();
            for (Thread fumador : hilosFumadores) {
                fumador.start();
            }
        }
        
        private boolean puedeTomarIngredientes(int id) {
            int count = 0;
            for (int i = 0; i < 3; i++) {
                if (i != id && ingredientesDisponibles[i]) {
                    count++;
                }
            }
            return count == 2;
        }
    }

    static class PanelFumadores extends JPanel {
        private final FumadoresBase fumadores;
        private final String[] nombresIngredientes = {"PAPEL", "TABACO", "FÃ“SFOROS"};
        private final String[] nombresFumadores = {"Fumador Papel", "Fumador Tabaco", "Fumador FÃ³sforos"};
        private int humoAnimacion = 0;
        private int llamaAnimacion = 0;
        private javax.swing.Timer animationTimer;
        
        public PanelFumadores(FumadoresBase fumadores) {
            this.fumadores = fumadores;
            setBackground(new Color(240, 245, 255));
            setPreferredSize(new Dimension(750, 650));
            
            animationTimer = new javax.swing.Timer(50, e -> {
                humoAnimacion = (humoAnimacion + 1) % 100;
                llamaAnimacion = (llamaAnimacion + 1) % 40;
                repaint();
            });
            animationTimer.start();
        }
        
        public void actualizarEstado() {
            SwingUtilities.invokeLater(this::repaint);
        }
        
        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            dibujarAgente(g, 350, 50);
            dibujarIngredientes(g);
            
            String[] estados = fumadores.getEstadosFumadores();
            for (int i = 0; i < 3; i++) {
                int x = 100 + i * 250;
                int y = 350;
                dibujarFumador(g, x, y, i, estados[i]);
            }
            
            if (fumadores.isCigarroEncendido()) {
                dibujarEfectoCigarro(g);
            }
            
            g.setColor(Color.BLUE.darker());
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.drawString("Problema de los Fumadores", 20, 30);
        }
        
        private void dibujarAgente(Graphics2D g, int x, int y) {
            g.setColor(new Color(139, 69, 19));
            g.fillRect(x - 100, y + 60, 200, 15);
            g.fillRect(x - 90, y + 75, 10, 40);
            g.fillRect(x + 80, y + 75, 10, 40);
            
            g.setColor(new Color(255, 220, 180));
            g.fillOval(x - 20, y - 30, 40, 40);
            g.setColor(Color.BLACK);
            g.fillOval(x - 10, y - 18, 5, 5);
            g.fillOval(x + 5, y - 18, 5, 5);
            g.drawArc(x - 8, y - 10, 16, 10, 0, -180);
            
            g.setColor(new Color(100, 100, 200));
            g.fillRect(x - 25, y + 10, 50, 60);
            
            g.setColor(Color.BLUE);
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            g.drawString("Agente: " + fumadores.getEstadoAgente(), x - 100, y - 50);
        }
        
        private void dibujarIngredientes(Graphics2D g) {
            boolean[] ingredientes = fumadores.getIngredientesDisponibles();
            for (int i = 0; i < 3; i++) {
                int x = 200 + i * 150;
                int y = 150;
                
                if (ingredientes[i]) {
                    g.setColor(new Color(100, 255, 100));
                } else {
                    g.setColor(new Color(200, 200, 200));
                }
                
                g.fillRoundRect(x, y, 100, 50, 10, 10);
                g.setColor(Color.BLACK);
                g.drawRoundRect(x, y, 100, 50, 10, 10);
                
                dibujarIconoIngrediente(g, x + 10, y + 10, i);
                
                g.setFont(new Font("SansSerif", Font.BOLD, 12));
                g.drawString(nombresIngredientes[i], x + 10, y + 65);
                
                if (ingredientes[i]) {
                    g.setColor(new Color(255, 255, 150, 100));
                    g.fillRoundRect(x - 3, y - 3, 106, 56, 10, 10);
                }
            }
        }
        
        private void dibujarIconoIngrediente(Graphics2D g, int x, int y, int tipo) {
            g.setColor(Color.BLACK);
            switch(tipo) {
                case 0:
                    g.fillRect(x, y, 25, 30);
                    g.setColor(Color.WHITE);
                    for (int i = 0; i < 5; i++) {
                        g.drawLine(x + 3, y + 5 + i * 5, x + 22, y + 5 + i * 5);
                    }
                    break;
                case 1:
                    g.setColor(new Color(139, 69, 19));
                    g.fillOval(x + 5, y + 5, 20, 20);
                    g.setColor(new Color(101, 67, 33));
                    for (int i = 0; i < 3; i++) {
                        g.fillOval(x + 8 + i * 5, y + 8, 4, 15);
                    }
                    break;
                case 2:
                    g.setColor(new Color(200, 150, 100));
                    g.fillRect(x + 8, y + 10, 3, 15);
                    g.setColor(Color.RED);
                    g.fillOval(x + 6, y + 7, 7, 7);
                    break;
            }
        }
        
        private void dibujarFumador(Graphics2D g, int x, int y, int id, String estado) {
            Color[] colores = {
                new Color(255, 150, 150),
                new Color(150, 255, 150),
                new Color(150, 150, 255)
            };
            g.setColor(colores[id]);
            g.fillRect(x - 20, y, 40, 60);
            
            g.setColor(new Color(255, 220, 180));
            g.fillOval(x - 25, y - 35, 50, 50);
            
            g.setColor(Color.BLACK);
            g.fillOval(x - 15, y - 20, 8, 8);
            g.fillOval(x + 7, y - 20, 8, 8);
            
            if (estado.contains("FUMANDO")) {
                g.setColor(Color.WHITE);
                g.fillRect(x + 20, y - 10, 20, 3);
                g.setColor(Color.ORANGE);
                g.fillOval(x + 38, y - 12, 6, 6);
                dibujarHumo(g, x + 42, y - 15);
            }
            
            if (estado.contains("FUMANDO")) {
                g.setColor(Color.BLACK);
                g.fillOval(x - 5, y - 5, 10, 8);
            } else {
                g.drawArc(x - 10, y - 8, 20, 10, 0, -180);
            }
            
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(8));
            g.drawLine(x - 10, y + 60, x - 10, y + 90);
            g.drawLine(x + 10, y + 60, x + 10, y + 90);
            
            g.setColor(Color.BLACK);
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            g.drawString(nombresFumadores[id], x - 50, y + 110);
            g.setFont(new Font("SansSerif", Font.PLAIN, 10));
            
            String estadoCorto = estado.length() > 18 ?
                estado.substring(0, 15) + "..." : estado;
            g.drawString(estadoCorto, x - 50, y + 125);
        }
        
        private void dibujarHumo(Graphics2D g, int x, int y) {
            Composite oldComposite = g.getComposite();
            for (int i = 0; i < 5; i++) {
                float alpha = 0.5f - (i * 0.08f) - (humoAnimacion % 20) * 0.01f;
                if (alpha > 0) {
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                    g.setColor(new Color(200, 200, 200));
                    int offsetX = (int)(Math.sin((humoAnimacion + i * 10) * 0.1) * 10);
                    int offsetY = -i * 15 - (humoAnimacion % 20);
                    int size = 15 + i * 3;
                    g.fillOval(x + offsetX, y + offsetY, size, size);
                }
            }
            g.setComposite(oldComposite);
        }
        
        private void dibujarEfectoCigarro(Graphics2D g) {
            int cx = getWidth() / 2;
            int cy = 280;
            
            Composite oldComposite = g.getComposite();
            float alpha = (float)(Math.sin(llamaAnimacion * 0.15) * 0.3 + 0.5);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g.setColor(Color.ORANGE);
            g.fillOval(cx - 20, cy - 20, 40, 40);
            g.setColor(Color.RED);
            g.fillOval(cx - 15, cy - 15, 30, 30);
            g.setColor(Color.YELLOW);
            g.fillOval(cx - 10, cy - 10, 20, 20);
            g.setComposite(oldComposite);
            
            g.setColor(Color.BLACK);
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.drawString("Â¡CIGARRO ENCENDIDO!", cx - 80, cy + 40);
        }
    }

    // ============================================================================
    // LECTORES-ESCRITORES - TODAS LAS TÃ‰CNICAS
    // ============================================================================
    
    static class LectoresEscritoresBase implements ProblemaSimulacion {
        protected PanelLectoresEscritores panelVisual;
        protected Thread[] hilosLectores;
        protected Thread[] hilosEscritores;
        protected volatile boolean ejecutando = false;
        protected volatile String[] estadosLectores;
        protected volatile String[] estadosEscritores;
        protected volatile String contenidoPizarra = "INICIO";
        protected volatile boolean pizarraOcupada = false;
        protected ProyectoPCyPoto2025 parent;
        
        public LectoresEscritoresBase(ProyectoPCyPoto2025 parent) {
            this.parent = parent;
            this.estadosLectores = new String[3];
            this.estadosEscritores = new String[2];
            Arrays.fill(estadosLectores, "ESPERANDO");
            Arrays.fill(estadosEscritores, "ESPERANDO");
            this.panelVisual = new PanelLectoresEscritores(this);
        }
        
        @Override
        public JPanel getPanelVisual() { return panelVisual; }
        
        @Override
        public Thread[] getHilos() {
            int lenLectores = (hilosLectores != null) ? hilosLectores.length : 0;
            int lenEscritores = (hilosEscritores != null) ? hilosEscritores.length : 0;
            Thread[] todos = new Thread[lenLectores + lenEscritores];
            if (hilosLectores != null) {
                System.arraycopy(hilosLectores, 0, todos, 0, lenLectores);
            }
            if (hilosEscritores != null) {
                System.arraycopy(hilosEscritores, 0, todos, lenLectores, lenEscritores);
            }
            return todos;
        }
        
        @Override
        public void detenerSimulacion() {
            ejecutando = false;
            if (hilosLectores != null) {
                for (Thread lector : hilosLectores) {
                    if (lector != null) lector.interrupt();
                }
            }
            if (hilosEscritores != null) {
                for (Thread escritor : hilosEscritores) {
                    if (escritor != null) escritor.interrupt();
                }
            }
        }
        
        @Override
        public void notificarEventoGrafo(String evento, String detalles) {
            if (parent != null) {
                parent.resaltarNodoGrafo(evento, detalles);
            }
        }
        
        public String[] getEstadosLectores() { return estadosLectores; }
        public String[] getEstadosEscritores() { return estadosEscritores; }
        public String getContenidoPizarra() { return contenidoPizarra; }
        public boolean isPizarraOcupada() { return pizarraOcupada; }
        
        @Override
        public void iniciarSimulacion() {} // Implementado en subclases
    }
    
    static class LectoresEscritoresSemaforo extends LectoresEscritoresBase {
        private final Semaphore mutex = new Semaphore(1);
        private final Semaphore escritura = new Semaphore(1);
        private int lectores = 0;
        
        public LectoresEscritoresSemaforo(ProyectoPCyPoto2025 parent) {
            super(parent);
        }
        
        @Override
        public void iniciarSimulacion() {
            ejecutando = true;
            hilosLectores = new Thread[3];
            for (int i = 0; i < 3; i++) {
                final int id = i;
                hilosLectores[i] = new Thread(() -> {
                    while (ejecutando && !Thread.currentThread().isInterrupted()) {
                        try {
                            estadosLectores[id] = "ESPERANDO PARA LEER";
                            notificarEventoGrafo("Lector", "Esperando");
                            panelVisual.actualizarEstado();
                            
                            mutex.acquire();
                            lectores++;
                            if (lectores == 1) {
                                escritura.acquire();
                            }
                            mutex.release();
                            
                            estadosLectores[id] = "LEYENDO: " + contenidoPizarra;
                            notificarEventoGrafo("Recurso", "Siendo leÃ­do");
                            panelVisual.actualizarEstado();
                            Thread.sleep(ThreadLocalRandom.current().nextInt(2000, 4000));
                            
                            mutex.acquire();
                            lectores--;
                            if (lectores == 0) {
                                escritura.release();
                            }
                            mutex.release();
                            
                            estadosLectores[id] = "TERMINÃ“ DE LEER";
                            panelVisual.actualizarEstado();
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                });
            }
            
            hilosEscritores = new Thread[2];
            for (int i = 0; i < 2; i++) {
                final int id = i;
                hilosEscritores[i] = new Thread(() -> {
                    while (ejecutando && !Thread.currentThread().isInterrupted()) {
                        try {
                            estadosEscritores[id] = "ESPERANDO PARA ESCRIBIR";
                            notificarEventoGrafo("Escritor", "Esperando");
                            panelVisual.actualizarEstado();
                            
                            escritura.acquire();
                            pizarraOcupada = true;
                            
                            String nuevoTexto = "Texto-" + id + "-" + (System.currentTimeMillis() % 1000);
                            estadosEscritores[id] = "ESCRIBIENDO";
                            notificarEventoGrafo("Recurso", "Siendo escrito");
                            panelVisual.actualizarEstado();
                            Thread.sleep(ThreadLocalRandom.current().nextInt(3000, 5000));
                            
                            contenidoPizarra = nuevoTexto;
                            pizarraOcupada = false;
                            estadosEscritores[id] = "TERMINÃ“ DE ESCRIBIR";
                            panelVisual.actualizarEstado();
                            
                            escritura.release();
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                });
            }
            
            for (Thread lector : hilosLectores) {
                lector.start();
            }
            for (Thread escritor : hilosEscritores) {
                escritor.start();
            }
        }
    }
    
    static class LectoresEscritoresCondition extends LectoresEscritoresBase {
        private final Lock lock = new ReentrantLock();
        private final Condition puedeEscribir = lock.newCondition();
        // private final Condition puedeLeer = lock.newCondition(); // Opcional, dependiendo de la polÃ­tica
        private int lectores = 0;
        
        public LectoresEscritoresCondition(ProyectoPCyPoto2025 parent) {
            super(parent);
        }
        
        @Override
        public void iniciarSimulacion() {
            ejecutando = true;
            hilosLectores = new Thread[3];
            for (int i = 0; i < 3; i++) {
                final int id = i;
                hilosLectores[i] = new Thread(() -> {
                    while (ejecutando && !Thread.currentThread().isInterrupted()) {
                        try {
                            lock.lock();
                            try {
                                // En esta implementaciÃ³n simple (preferencia de lector), 
                                // los escritores esperan si hay lectores o un escritor.
                                // Los lectores solo esperan si hay un escritor.
                                // PodrÃ­as aÃ±adir un `while(pizarraOcupada) puedeLeer.await();`
                                // si quisieras que los lectores esperen activamente.
                                
                                estadosLectores[id] = "ESPERANDO PARA LEER";
                                notificarEventoGrafo("Lector", "Esperando");
                                panelVisual.actualizarEstado();
                                
                                // Bucle de espera si hay un escritor (simplificado)
                                while(pizarraOcupada) {
                                    // En una polÃ­tica justa, esperarÃ­an en `puedeLeer`
                                    // AquÃ­, simplemente re-intentan el lock (no es ideal, pero simple)
                                    // Una implementaciÃ³n mÃ¡s robusta usarÃ­a `puedeLeer.await()`
                                    // y los escritores harÃ­an `puedeLeer.signalAll()`
                                    puedeEscribir.await(); // Espera general
                                }
                                
                                lectores++;
                                estadosLectores[id] = "LEYENDO: " + contenidoPizarra;
                                notificarEventoGrafo("Recurso", "Siendo leÃ­do");
                                panelVisual.actualizarEstado();
                            } finally {
                                lock.unlock();
                            }
                            
                            Thread.sleep(ThreadLocalRandom.current().nextInt(2000, 4000));
                            
                            lock.lock();
                            try {
                                lectores--;
                                if (lectores == 0) {
                                    puedeEscribir.signalAll(); // Avisa a escritores que ya no hay lectores
                                }
                                estadosLectores[id] = "TERMINÃ“ DE LEER";
                                panelVisual.actualizarEstado();
                            } finally {
                                lock.unlock();
                            }
                            
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                });
            }
            
            hilosEscritores = new Thread[2];
            for (int i = 0; i < 2; i++) {
                final int id = i;
                hilosEscritores[i] = new Thread(() -> {
                    while (ejecutando && !Thread.currentThread().isInterrupted()) {
                        try {
                            lock.lock();
                            try {
                                estadosEscritores[id] = "ESPERANDO PARA ESCRIBIR";
                                notificarEventoGrafo("Escritor", "Esperando");
                                panelVisual.actualizarEstado();
                                
                                while (lectores > 0 || pizarraOcupada) {
                                    puedeEscribir.await();
                                }
                                
                                pizarraOcupada = true;
                                estadosEscritores[id] = "ESCRIBIENDO";
                                notificarEventoGrafo("Recurso", "Siendo escrito");
                                panelVisual.actualizarEstado();
                            } finally {
                                lock.unlock();
                            }
                            
                            Thread.sleep(ThreadLocalRandom.current().nextInt(3000, 5000));
                            
                            lock.lock();
                            try {
                                String nuevoTexto = "Texto-" + id + "-" + (System.currentTimeMillis() % 1000);
                                contenidoPizarra = nuevoTexto;
                                pizarraOcupada = false;
                                estadosEscritores[id] = "TERMINÃ“ DE ESCRIBIR";
                                panelVisual.actualizarEstado();
                                puedeEscribir.signalAll(); // Avisa a otros escritores o lectores
                                // En una polÃ­tica mÃ¡s estricta, podrÃ­a ser:
                                // puedeLeer.signalAll();
                                // puedeEscribir.signal();
                            } finally {
                                lock.unlock();
                            }
                            
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                });
            }
            
            for (Thread lector : hilosLectores) {
                lector.start();
            }
            for (Thread escritor : hilosEscritores) {
                escritor.start();
            }
        }
    }
    
    static class LectoresEscritoresMonitor extends LectoresEscritoresBase {
        private final Object monitor = new Object();
        private int lectores = 0;
        
        public LectoresEscritoresMonitor(ProyectoPCyPoto2025 parent) {
            super(parent);
        }
        
        @Override
        public void iniciarSimulacion() {
            ejecutando = true;
            hilosLectores = new Thread[3];
            for (int i = 0; i < 3; i++) {
                final int id = i;
                hilosLectores[i] = new Thread(() -> {
                    while (ejecutando && !Thread.currentThread().isInterrupted()) {
                        try {
                            synchronized (monitor) {
                                estadosLectores[id] = "ESPERANDO PARA LEER";
                                notificarEventoGrafo("Lector", "Esperando");
                                panelVisual.actualizarEstado();
                                
                                while (pizarraOcupada) {
                                    monitor.wait();
                                }
                                
                                lectores++;
                                estadosLectores[id] = "LEYENDO: " + contenidoPizarra;
                                notificarEventoGrafo("Recurso", "Siendo leÃ­do");
                                panelVisual.actualizarEstado();
                            }
                            
                            Thread.sleep(ThreadLocalRandom.current().nextInt(2000, 4000));
                            
                            synchronized (monitor) {
                                lectores--;
                                estadosLectores[id] = "TERMINÃ“ DE LEER";
                                panelVisual.actualizarEstado();
                                if (lectores == 0) {
                                    monitor.notifyAll();
                                }
                            }
                            
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                });
            }
            
            hilosEscritores = new Thread[2];
            for (int i = 0; i < 2; i++) {
                final int id = i;
                hilosEscritores[i] = new Thread(() -> {
                    while (ejecutando && !Thread.currentThread().isInterrupted()) {
                        try {
                            synchronized (monitor) {
                                estadosEscritores[id] = "ESPERANDO PARA ESCRIBIR";
                                notificarEventoGrafo("Escritor", "Esperando");
                                panelVisual.actualizarEstado();
                                
                                while (lectores > 0 || pizarraOcupada) {
                                    monitor.wait();
                                }
                                
                                pizarraOcupada = true;
                                estadosEscritores[id] = "ESCRIBIENDO";
                                notificarEventoGrafo("Recurso", "Siendo escrito");
                                panelVisual.actualizarEstado();
                            }
                            
                            Thread.sleep(ThreadLocalRandom.current().nextInt(3000, 5000));
                            
                            synchronized (monitor) {
                                String nuevoTexto = "Texto-" + id + "-" + (System.currentTimeMillis() % 1000);
                                contenidoPizarra = nuevoTexto;
                                pizarraOcupada = false;
                                estadosEscritores[id] = "TERMINÃ“ DE ESCRIBIR";
                                panelVisual.actualizarEstado();
                                monitor.notifyAll();
                            }
                            
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                });
            }
            
            for (Thread lector : hilosLectores) {
                lector.start();
            }
            for (Thread escritor : hilosEscritores) {
                escritor.start();
            }
        }
    }

    static class PanelLectoresEscritores extends JPanel {
        private final LectoresEscritoresBase lecEsc;
        private int brilloPizarra = 0;
        private int animacionEscritura = 0;
        private javax.swing.Timer animationTimer;
        
        public PanelLectoresEscritores(LectoresEscritoresBase lecEsc) {
            this.lecEsc = lecEsc;
            setBackground(new Color(240, 245, 255));
            setPreferredSize(new Dimension(750, 650));
            
            animationTimer = new javax.swing.Timer(50, e -> {
                brilloPizarra = (brilloPizarra + 1) % 100;
                animacionEscritura = (animacionEscritura + 1) % 60;
                repaint();
            });
            animationTimer.start();
        }
        
        public void actualizarEstado() {
            SwingUtilities.invokeLater(this::repaint);
        }
        
        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            dibujarPizarra(g, 200, 50);
            
            String[] estadosLectores = lecEsc.getEstadosLectores();
            for (int i = 0; i < 3; i++) {
                int x = 50 + i * 230;
                int y = 320;
                dibujarPersona(g, x, y, estadosLectores[i], true, i);
            }
            
            String[] estadosEscritores = lecEsc.getEstadosEscritores();
            for (int i = 0; i < 2; i++) {
                int x = 180 + i * 300;
                int y = 500;
                dibujarPersona(g, x, y, estadosEscritores[i], false, i);
            }
            
            g.setColor(Color.BLUE.darker());
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.drawString("Lectores-Escritores", 20, 30);
        }
        
        private void dibujarPizarra(Graphics2D g, int x, int y) {
            g.setColor(new Color(139, 69, 19));
            g.fillRoundRect(x - 10, y - 10, 370, 160, 15, 15);
            
            if (lecEsc.isPizarraOcupada()) {
                float alpha = (float)(Math.sin(brilloPizarra * 0.1) * 0.2 + 0.8);
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g.setColor(new Color(50, 100, 50));
            } else {
                g.setColor(new Color(34, 80, 34));
            }
            g.fillRoundRect(x, y, 350, 140, 8, 8);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            
            g.setColor(Color.WHITE);
            g.setFont(new Font("Monospaced", Font.BOLD, 16));
            
            String contenido = lecEsc.getContenidoPizarra();
            if (contenido.length() > 30) {
                String linea1 = contenido.substring(0, Math.min(30, contenido.length()));
                String linea2 = contenido.length() > 30 ? contenido.substring(30) : "";
                g.drawString(linea1, x + 20, y + 50);
                if (!linea2.isEmpty()) {
                    g.drawString(linea2, x + 20, y + 75);
                }
            } else {
                g.drawString(contenido, x + 20, y + 60);
            }
            
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            if (lecEsc.isPizarraOcupada()) {
                g.setColor(Color.RED);
                g.drawString("OCUPADA - ESCRIBIENDO", x + 20, y + 120);
                int marcadorX = x + 20 + (animacionEscritura % 250);
                g.setColor(Color.YELLOW);
                g.fillRect(marcadorX, y + 95, 3, 20);
            } else {
                g.setColor(Color.GREEN);
                g.drawString("DISPONIBLE PARA LECTURA", x + 20, y + 120);
            }
            
            g.setColor(new Color(200, 200, 200));
            g.fillRect(x + 300, y + 145, 40, 15);
            g.setColor(Color.BLUE);
            g.fillRect(x + 260, y + 145, 8, 18);
            g.setColor(Color.RED);
            g.fillRect(x + 270, y + 145, 8, 18);
            g.setColor(Color.GREEN);
            g.fillRect(x + 280, y + 145, 8, 18);
        }
        
        private void dibujarPersona(Graphics2D g, int x, int y, String estado, boolean esLector, int id) {
            Color colorBase = esLector ?
                new Color(100, 150, 255) : new Color(255, 100, 100);
            
            if (estado.contains("LEYENDO") || estado.contains("ESCRIBIENDO")) {
                colorBase = colorBase.brighter();
            } else if (estado.contains("ESPERANDO")) {
                colorBase = colorBase.darker();
            }
            
            g.setColor(colorBase);
            g.fillRect(x - 20, y, 40, 60);
            
            g.setColor(new Color(255, 220, 180));
            g.fillOval(x - 25, y - 35, 50, 50);
            
            if (esLector) {
                g.setColor(new Color(50, 30, 20));
                g.fillArc(x - 25, y - 40, 50, 35, 0, 180);
            } else {
                g.setColor(new Color(100, 70, 40));
                for (int i = 0; i < 5; i++) {
                    g.fillOval(x - 20 + i * 10, y - 38, 8, 15);
                }
            }
            
            if (esLector) {
                g.setColor(Color.BLACK);
                g.setStroke(new BasicStroke(2));
                g.drawOval(x - 18, y - 20, 12, 12);
                g.drawOval(x + 6, y - 20, 12, 12);
                g.drawLine(x - 6, y - 14, x + 6, y - 14);
            }
            
            g.setColor(Color.WHITE);
            g.fillOval(x - 15, y - 20, 10, 10);
            g.fillOval(x + 5, y - 20, 10, 10);
            g.setColor(Color.BLACK);
            g.fillOval(x - 12, y - 17, 5, 5);
            g.fillOval(x + 8, y - 17, 5, 5);
            
            if (estado.contains("LEYENDO") || estado.contains("ESCRIBIENDO")) {
                g.drawArc(x - 10, y - 8, 20, 12, 0, -180);
            } else {
                g.drawLine(x - 8, y - 5, x + 8, y - 5);
            }
            
            g.setColor(new Color(255, 220, 180));
            g.setStroke(new BasicStroke(6));
            if (!esLector && estado.contains("ESCRIBIENDO")) {
                g.drawLine(x + 20, y + 20, x + 40, y + 10);
                g.setColor(Color.BLUE);
                g.fillRect(x + 38, y + 8, 3, 15);
            } else if (esLector && estado.contains("LEYENDO")) {
                g.drawLine(x - 20, y + 30, x - 10, y + 40);
                g.drawLine(x + 20, y + 30, x + 10, y + 40);
                g.setColor(new Color(200, 150, 100));
                g.fillRect(x - 15, y + 35, 30, 20);
                g.setColor(Color.BLACK);
                g.drawRect(x - 15, y + 35, 30, 20);
                g.drawLine(x, y + 35, x, y + 55);
            } else {
                g.drawLine(x - 20, y + 20, x - 30, y + 40);
                g.drawLine(x + 20, y + 20, x + 30, y + 40);
            }
            
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(8));
            g.drawLine(x - 10, y + 60, x - 10, y + 90);
            g.drawLine(x + 10, y + 60, x + 10, y + 90);
            
            g.setColor(Color.BLACK);
            g.setFont(new Font("SansSerif", Font.BOLD, 11));
            String label = esLector ? "Lector " + (id + 1) : "Escritor " + (id + 1);
            g.drawString(label, x - 30, y + 110);
            
            g.setFont(new Font("SansSerif", Font.PLAIN, 9));
            String estadoCorto = estado.length() > 20 ?
                estado.substring(0, 17) + "..." : estado;
            g.drawString(estadoCorto, x - 40, y + 125);
        }
    }

    // ============================================================================
    // CLASES DE NODO Y FLECHA PARA EL GRAFO
    // ============================================================================
    
    class Nodo extends JComponent {
        String nombre;
        boolean esProceso;
        Point dragOffset;
        volatile boolean resaltado = false;
        
        Nodo(String nombre, boolean esProceso) {
            this.nombre = nombre;
            this.esProceso = esProceso;
            setBounds(100, 100, 72, 72);
            setOpaque(false);
            
            JPopupMenu menu = new JPopupMenu();
            JMenuItem asignar = new JMenuItem("Asignar Recurso");
            JMenuItem solicitar = new JMenuItem("Solicitar Recurso");
            JMenuItem eliminar = new JMenuItem("Eliminar");
            menu.add(asignar);
            menu.add(solicitar);
            menu.add(eliminar);
            
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    dragOffset = e.getPoint();
                    if(e.isPopupTrigger()) menu.show(Nodo.this, e.getX(), e.getY());
                }
                public void mouseReleased(MouseEvent e) {
                    if(e.isPopupTrigger()) menu.show(Nodo.this, e.getX(), e.getY());
                }
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1) {
                        if (nodoSeleccionado == Nodo.this) {
                            nodoSeleccionado = null;
                        } else {
                            nodoSeleccionado = Nodo.this;
                        }
                        panelDerecho.repaint();
                    }
                }
            });
            
            addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseDragged(MouseEvent e) {
                    Point p = getLocation();
                    p.translate(e.getX() - dragOffset.x, e.getY() - dragOffset.y);
                    setLocation(p);
                    panelDerecho.repaint();
                    actualizarTamanioPanelGrafo(); // <-- AÃ‘ADIDO (SCROLL GRAFO)
                }
            });
            
            asignar.addActionListener(e -> crearFlecha(nodoSeleccionado, Nodo.this, Color.MAGENTA));
            solicitar.addActionListener(e -> crearFlecha(Nodo.this, nodoSeleccionado, Color.BLUE));
            eliminar.addActionListener(e -> eliminarNodo(Nodo.this));
        }
        
        private void crearFlecha(Nodo origen, Nodo destino, Color color) {
            if (origen != null && destino != null && origen != destino) {
                addFlecha(origen, destino, color);
                nodoSeleccionado = null;
                panelDerecho.repaint();
            } else {
                nodoSeleccionado = this;
            }
        }
        
        private void eliminarNodo(Nodo nodo) {
            flechas.removeIf(f -> f.origen == nodo || f.destino == nodo);
            panelDerecho.remove(nodo);
            nodos.remove(nodo);
            panelDerecho.repaint();
            actualizarTamanioPanelGrafo(); // <-- AÃ‘ADIDO (SCROLL GRAFO)
        }
        
        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            GradientPaint grad;
            
            if (esProceso) {
                if (resaltado) {
                    grad = new GradientPaint(0, 0, new Color(255, 255, 100), getWidth(), getHeight(), new Color(255, 200, 0));
                } else {
                    grad = new GradientPaint(0, 0, new Color(255, 200, 210), getWidth(), getHeight(), new Color(240, 140, 190));
                }
                g.setPaint(grad);
                g.fillOval(0, 0, getWidth()-1, getHeight()-1);
                g.setColor(resaltado ? Color.ORANGE : new Color(150, 20, 80));
                g.setStroke(new BasicStroke(resaltado ? 4 : 2));
                g.drawOval(0, 0, getWidth()-1, getHeight()-1);
            } else {
                if (resaltado) {
                    grad = new GradientPaint(0, 0, new Color(100, 255, 100), getWidth(), getHeight(), new Color(0, 200, 0));
                } else {
                    grad = new GradientPaint(0, 0, new Color(255, 200, 120), getWidth(), getHeight(), new Color(255, 140, 40));
                }
                g.setPaint(grad);
                g.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 16, 16);
                g.setColor(resaltado ? Color.GREEN.darker() : new Color(140, 60, 20));
                g.setStroke(new BasicStroke(resaltado ? 4 : 2));
                g.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 16, 16);
            }
            
            g.setColor(Color.DARK_GRAY);
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(nombre);
            g.drawString(nombre, (getWidth() - textWidth) / 2, getHeight() / 2 + 5);
            
            if (nodoSeleccionado == this) {
                g.setColor(Color.CYAN);
                g.setStroke(new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{9}, 0.0f));
                if (esProceso) {
                    g.drawOval(2, 2, getWidth()-5, getHeight()-5);
                } else {
                    g.drawRoundRect(2, 2, getWidth()-5, getHeight()-5, 16, 16);
                }
            }
        }
        
        public void setResaltado(boolean resaltado) {
            this.resaltado = resaltado;
            repaint();
        }
    }
    
    class Flecha {
        Nodo origen, destino;
        Color color;
        volatile boolean resaltada = false;
        
        Flecha(Nodo o, Nodo d, Color c) {
            this.origen = o;
            this.destino = d;
            this.color = c;
        }
        
        void dibujar(Graphics2D g) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Point p1 = new Point(origen.getX() + origen.getWidth() / 2, origen.getY() + origen.getHeight() / 2);
            Point p2 = new Point(destino.getX() + destino.getWidth() / 2, destino.getY() + destino.getHeight() / 2);
            
            g.setColor(resaltada ? Color.YELLOW : color);
            g.setStroke(new BasicStroke(resaltada ? 4.0f : 2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            
            double ang = Math.atan2(p2.y - p1.y, p2.x - p1.x);
            double sin = Math.sin(ang);
            double cos = Math.cos(ang);
            int al = 12;
            int w2 = destino.getWidth() / 2;
            int h2 = destino.getHeight() / 2;
            Point target = new Point((int) (p2.x - (w2 * cos)), (int) (p2.y - (h2 * sin)));
            Point lineEnd = new Point((int) (target.x - (al/2) * cos), (int) (target.y - (al/2) * sin));
            g.drawLine(p1.x, p1.y, lineEnd.x, lineEnd.y);
            
            Point pa = new Point((int) (target.x - al * Math.cos(ang - Math.PI / 6)), (int) (target.y - al * Math.sin(ang - Math.PI / 6)));
            Point pb = new Point((int) (target.x - al * Math.cos(ang + Math.PI / 6)), (int) (target.y - al * Math.sin(ang + Math.PI / 6)));
            int[] xPoints = {target.x, pa.x, pb.x};
            int[] yPoints = {target.y, pa.y, pb.y};
            g.fillPolygon(xPoints, yPoints, 3);
        }
        
        public void setResaltada(boolean resaltada) {
            this.resaltada = resaltada;
        }
    }

    // ============================================================================
    // CONSTRUCTOR Y MÃ‰TODOS PRINCIPALES
    // ============================================================================
    
    public ProyectoPCyPoto2025() {
        setSize(1200, 900);
        setTitle("Proyecto PCyP OtoÃ±o 2025 - Simulaciones Animadas");
        setLayout(new BorderLayout());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        JMenuBar barra = new JMenuBar();
        
        archivo = new JMenu("Archivo");
        salir = new JMenuItem("Salir");
        salir.addActionListener(e -> System.exit(0));
        archivo.add(salir);
        barra.add(archivo);
        
        synchMenu = new JMenu("SincronizaciÃ³n");
        techniqueGroup = new ButtonGroup();
        semaforoRadio = new JRadioButtonMenuItem("SemÃ¡foros", true);
        conditionRadio = new JRadioButtonMenuItem("Variable Condicional (Lock)");
        monitorRadio = new JRadioButtonMenuItem("Monitor (synchronized)");
        mutexRadio = new JRadioButtonMenuItem("Mutex");
        barreraRadio = new JRadioButtonMenuItem("Barreras");
        
        ActionListener techniqueListener = e -> {
            if (semaforoRadio.isSelected()) selectedTechnique = Technique.SEMAFORO;
            else if (conditionRadio.isSelected()) selectedTechnique = Technique.CONDITION;
            else if (monitorRadio.isSelected()) selectedTechnique = Technique.MONITOR;
            else if (mutexRadio.isSelected()) selectedTechnique = Technique.MUTEX;
            else if (barreraRadio.isSelected()) selectedTechnique = Technique.BARRERA;
            
            if (currentProblemName != null) {
                loadProblem(currentProblemName);
            }
        };
        
        semaforoRadio.addActionListener(techniqueListener);
        conditionRadio.addActionListener(techniqueListener);
        monitorRadio.addActionListener(techniqueListener);
        mutexRadio.addActionListener(techniqueListener);
        barreraRadio.addActionListener(techniqueListener);
        
        techniqueGroup.add(semaforoRadio);
        techniqueGroup.add(conditionRadio);
        techniqueGroup.add(monitorRadio);
        techniqueGroup.add(mutexRadio);
        techniqueGroup.add(barreraRadio);
        
        synchMenu.add(semaforoRadio);
        synchMenu.add(conditionRadio);
        synchMenu.add(monitorRadio);
        synchMenu.add(mutexRadio);
        synchMenu.add(barreraRadio);
        barra.add(synchMenu);
        
        graficaMenu = new JMenu("GrÃ¡fica");
        graficaGroup = new ButtonGroup();
        
        carruselRadio = new JRadioButtonMenuItem("Carrusel", true);
        scrollRadio = new JRadioButtonMenuItem("Scroll");
        acordeonRadio = new JRadioButtonMenuItem("AcordeÃ³n");
        
        ActionListener graficaListener = e -> {
            if (carruselRadio.isSelected()) currentChartType = "carrusel";
            else if (scrollRadio.isSelected()) currentChartType = "scroll";
            else if (acordeonRadio.isSelected()) currentChartType = "acordeon";
            
            updateChartDisplay();
        };
        
        carruselRadio.addActionListener(graficaListener);
        scrollRadio.addActionListener(graficaListener);
        acordeonRadio.addActionListener(graficaListener);
        
        graficaGroup.add(carruselRadio);
        graficaGroup.add(scrollRadio);
        graficaGroup.add(acordeonRadio);
        graficaMenu.add(carruselRadio);
        graficaMenu.add(scrollRadio);
        graficaMenu.add(acordeonRadio);
        barra.add(graficaMenu);
        
        problemas = new JMenu("Problemas");
        procon = new JMenuItem("Productor-Consumidor");
        filosofos = new JMenuItem("Cena de los FilÃ³sofos");
        barbero = new JMenuItem("Barbero DormilÃ³n");
        fumadores = new JMenuItem("Fumadores");
        lecesc = new JMenuItem("Lectores-Escritores");
        
        problemas.add(procon);
        problemas.add(filosofos);
        problemas.add(barbero);
        problemas.add(fumadores);
        problemas.add(lecesc);
        barra.add(problemas);
        
        setJMenuBar(barra);
        
        JPanel mainPanel = new JPanel(new GridLayout(1,2));
        add(mainPanel, BorderLayout.CENTER);
        
        panelIzquierdo = new JPanel(new BorderLayout());
        panelIzquierdo.setBorder(BorderFactory.createTitledBorder("SimulaciÃ³n"));
        JScrollPane scrollIzq = new JScrollPane(panelIzquierdo);
        scrollIzq.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollIzq.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        mainPanel.add(scrollIzq);
        
        JSplitPane splitDerecho = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitDerecho.setResizeWeight(0.7);
        
        panelDerecho = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D gg = (Graphics2D) g;
                gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                for (Flecha f : flechas) f.dibujar(gg);
            }
        };
        panelDerecho.setBorder(BorderFactory.createTitledBorder("Grafo de Recursos"));
        panelDerecho.setBackground(Color.WHITE);
        
        JPanel panelGrafica = new JPanel(new BorderLayout());
        panelGrafica.setBorder(BorderFactory.createTitledBorder("AnÃ¡lisis de Deadlock - FireChart"));
        
        createFireChart();
        graficaScrollPane = new JScrollPane(chartPanel);
        graficaScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        graficaScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        panelGrafica.add(graficaScrollPane, BorderLayout.CENTER);
        
        // <-- INICIO DE MODIFICACIÃ“N (SCROLL GRAFO) -->
        JScrollPane scrollGrafo = new JScrollPane(panelDerecho);
        scrollGrafo.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollGrafo.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        splitDerecho.setTopComponent(scrollGrafo); // Ahora agregas el scroll pane
        // <-- FIN DE MODIFICACIÃ“N (SCROLL GRAFO) -->
        
        splitDerecho.setBottomComponent(panelGrafica);
        mainPanel.add(splitDerecho);

        procon.addActionListener(e -> loadProblem("productor"));
        filosofos.addActionListener(e -> loadProblem("filosofos"));
        barbero.addActionListener(e -> loadProblem("barbero"));
        fumadores.addActionListener(e -> loadProblem("fumadores"));
        lecesc.addActionListener(e -> loadProblem("lectores"));

        setupContextMenu();
        setupWelcomeScreen();
        updateChartDisplay();
        startChartAnimation();
    }
    
    private ProblemaSimulacion problemaActual = null;
    
    private void setupWelcomeScreen() {
        detenerSimulacionActual();
        currentProblemName = null;
        setupGraphForProblem("none");
        mostrarPanel(new JPanel() {{
            setLayout(new GridBagLayout());
            add(new JLabel("<html><center><h2>Simulaciones de Concurrencia</h2><br>Selecciona TÃ©cnica y Problema del menÃº.</center></html>") {{
                setFont(new Font("SansSerif", Font.PLAIN, 18));
                setHorizontalAlignment(SwingConstants.CENTER);
            }});
        }});
    }
    
    private void setupContextMenu(){
        menuPanel = new JPopupMenu();
        JMenuItem agregarProceso = new JMenuItem("Agregar Proceso");
        JMenuItem agregarRecurso = new JMenuItem("Agregar Recurso");
        menuPanel.add(agregarProceso);
        menuPanel.add(agregarRecurso);
        
        panelDerecho.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger() && panelDerecho.getComponentAt(e.getPoint()) == panelDerecho) {
                    menuPanel.show(e.getComponent(), e.getX(), e.getY());
                }
            }
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger() && panelDerecho.getComponentAt(e.getPoint()) == panelDerecho) {
                    menuPanel.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        
        agregarProceso.addActionListener(e -> addNode(true));
        agregarRecurso.addActionListener(e -> addNode(false));
    }
    
    private void addNode(boolean esProceso) {
        Point p = panelDerecho.getMousePosition();
        if (p == null) p = new Point(100, 100);
        String nombre = (esProceso ? "P" : "R") + (esProceso ? procesoCount++ : recursoCount++);
        Nodo n = createNode(nombre, esProceso);
        n.setBounds(p.x - 36, p.y - 36, 72, 72);
        panelDerecho.revalidate();
        panelDerecho.repaint();
        actualizarTamanioPanelGrafo(); // <-- AÃ‘ADIDO (SCROLL GRAFO)
    }
    
    private void mostrarPanel(JPanel panel) {
        panelIzquierdo.removeAll();
        panelIzquierdo.add(panel, BorderLayout.CENTER);
        panelIzquierdo.revalidate();
        panelIzquierdo.repaint();
    }
    
    private void detenerSimulacionActual() {
        if (problemaActual != null) {
            problemaActual.detenerSimulacion();
            problemaActual = null;
        }
        hilosActivos.clear();
    }
    
    private void addFlecha(Nodo origen, Nodo destino, Color color) {
        if (origen != null && destino != null) {
            flechas.add(new Flecha(origen, destino, color));
            panelDerecho.repaint();
        }
    }

    private void loadProblem(String problemName) {
        detenerSimulacionActual();
        this.currentProblemName = problemName;
        
        try {
            problemaActual = crearProblema(problemName, selectedTechnique);
            
            if (problemaActual != null) {
                mostrarPanel(problemaActual.getPanelVisual());
                problemaActual.iniciarSimulacion();
                setupGraphForProblem(problemName);
                updateFireChart(problemName);
            } else {
                JOptionPane.showMessageDialog(this,
                    "ImplementaciÃ³n no disponible para: " + problemName + " con " + selectedTechnique,
                    "Falta ImplementaciÃ³n", JOptionPane.WARNING_MESSAGE);
                setupWelcomeScreen();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
            setupWelcomeScreen();
        }
    }
    
    private ProblemaSimulacion crearProblema(String problemName, Technique technique) {
        switch (problemName.toLowerCase()) {
            case "productor":
                if (technique == Technique.SEMAFORO) return new ProductorConsumidorSemaforo(this);
                else if (technique == Technique.CONDITION) return new ProductorConsumidorCondition(this);
                else if (technique == Technique.MONITOR) return new ProductorConsumidorMonitor(this);
                break;
            case "filosofos":
                if (technique == Technique.SEMAFORO) return new FilosofosSemaforo(this);
                else if (technique == Technique.CONDITION) return new FilosofosCondition(this);
                else if (technique == Technique.MONITOR) return new FilosofosMonitor(this);
                break;
            case "barbero":
                if (technique == Technique.SEMAFORO) return new BarberoSemaforo(this);
                else if (technique == Technique.CONDITION) return new BarberoCondition(this);
                else if (technique == Technique.MONITOR) return new BarberoMonitor(this);
                break;
            case "fumadores":
                if (technique == Technique.SEMAFORO) return new FumadoresSemaforo(this);
                else if (technique == Technique.CONDITION) return new FumadoresCondition(this);
                else if (technique == Technique.MONITOR) return new FumadoresMonitor(this);
                break;
            case "lectores":
                if (technique == Technique.SEMAFORO) return new LectoresEscritoresSemaforo(this);
                else if (technique == Technique.CONDITION) return new LectoresEscritoresCondition(this);
                else if (technique == Technique.MONITOR) return new LectoresEscritoresMonitor(this);
                break;
        }
        // Mutex y Barrera no estÃ¡n implementados en el cÃ³digo fuente
        return null;
    }

    // <-- INICIO DE MÃ‰TODO AÃ‘ADIDO (SCROLL GRAFO) -->
    /**
     * Calcula el tamaÃ±o necesario para contener todos los nodos
     * y actualiza el PreferredSize del panelDerecho para que
     * el JScrollPane muestre las barras de scroll.
     */
    private void actualizarTamanioPanelGrafo() {
        int maxX = 0;
        int maxY = 0;
        
        // Revisa las coordenadas de todos los componentes (nodos) en el panel
        for (Component comp : panelDerecho.getComponents()) {
            if (comp instanceof Nodo) {
                maxX = Math.max(maxX, comp.getX() + comp.getWidth());
                maxY = Math.max(maxY, comp.getY() + comp.getHeight());
            }
        }
        
        // Damos un pequeÃ±o margen adicional
        Dimension nuevoTamanio = new Dimension(maxX + 50, maxY + 50);
        
        // Si el tamaÃ±o nuevo es diferente al actual, lo actualizamos
        if (!nuevoTamanio.equals(panelDerecho.getPreferredSize())) {
            panelDerecho.setPreferredSize(nuevoTamanio);
            
            // Esto le dice al JScrollPane que re-evalÃºe las barras de scroll
            panelDerecho.revalidate();
        }
    }
    // <-- FIN DE MÃ‰TODO AÃ‘ADIDO (SCROLL GRAFO) -->
    
    // ============================================================================
    // MÃ‰TODOS PARA GRÃFICAS FIRECHART
    // ============================================================================
    
    private void createFireChart() {
        chartDataset = new DefaultCategoryDataset();
        currentChart = ChartFactory.createLineChart(
            "ANÃLISIS DE DEADLOCK - FIRECHART",
            "Tiempo (segundos)",
            "Recursos Bloqueados",
            chartDataset,
            PlotOrientation.VERTICAL,
            true, true, false
        );
        
        currentChart.getCategoryPlot().setBackgroundPaint(new Color(30, 30, 30));
        currentChart.getCategoryPlot().setRangeGridlinePaint(Color.RED);
        currentChart.getCategoryPlot().setDomainGridlinePaint(Color.DARK_GRAY);
        
        chartPanel = new ChartPanel(currentChart);
        chartPanel.setPreferredSize(new Dimension(400, 800)); // <-- MODIFICADO (SCROLL GRÃFICA)
        chartPanel.setBackground(new Color(20, 20, 20));
    }
    
    private void updateFireChart(String problemName) {
        chartDataset.clear();
        Random rand = new Random();
        String[] metricas = {"Recursos Ocupados", "Procesos Bloqueados", "Deadlock Risk", "Throughput"};
        for (String metrica : metricas) {
            for (int i = 0; i < 10; i++) {
                double valor = rand.nextDouble() * 100;
                chartDataset.addValue(valor, metrica, String.valueOf(i));
            }
        }
        currentChart.setTitle("DEADLOCK ANALYSIS - " + problemName.toUpperCase());
        chartPanel.repaint();
    }
    
    private void startChartAnimation() {
        chartAnimator = new javax.swing.Timer(1000, e -> {
            if (currentProblemName != null && chartDataset != null) {
                Random rand = new Random();
                String[] metricas = {"Recursos Ocupados", "Procesos Bloqueados", "Deadlock Risk", "Throughput"};
                
                for (String metrica : metricas) {
                    double valor = rand.nextDouble() * 100;
                    chartDataset.addValue(valor, metrica, String.valueOf(System.currentTimeMillis() / 1000));
                }
                
                // <-- INICIO MODIFICACIÃ“N (SCROLL GRÃFICA) -->
                // Se elimina el bloque que borraba datos antiguos para permitir el scroll horizontal
                
                // if (chartDataset.getColumnCount() > 20) {
                //     // Mantiene solo las Ãºltimas 20 columnas
                //     List<Comparable> keys = new ArrayList<>(chartDataset.getColumnKeys());
                //     for (int i = 0; i < keys.size() - 20; i++) {
                //         chartDataset.removeColumn(keys.get(i));
                //     }
                // }
                // <-- FIN MODIFICACIÃ“N (SCROLL GRÃFICA) -->
            }
        });
        chartAnimator.start();
    }
    
    private void updateChartDisplay() {
        if (graficaScrollPane == null) return;
        
        switch (currentChartType) {
            case "carrusel":
                graficaScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
                graficaScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                // chartPanel.setPreferredSize(new Dimension(400, 250)); // <-- MODIFICADO (SCROLL GRÃFICA)
                break;
            case "scroll":
                graficaScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
                graficaScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                // chartPanel.setPreferredSize(new Dimension(600, 400)); // <-- MODIFICADO (SCROLL GRÃFICA)
                break;
            case "acordeon":
                graficaScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
                graficaScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                // chartPanel.setPreferredSize(new Dimension(400, 150)); // <-- MODIFICADO (SCROLL GRÃFICA)
                break;
        }
        
        graficaScrollPane.revalidate();
        graficaScrollPane.repaint();
    }

    // ============================================================================
    // MÃ‰TODOS DE GRAFO CON SINCRONIZACIÃ“N
    // ============================================================================
    
    public void resaltarNodoGrafo(String nombreNodo, String estado) {
        SwingUtilities.invokeLater(() -> {
            for (Nodo nodo : nodos) {
                if (nodo.nombre.equals(nombreNodo)) {
                    nodo.setResaltado(true);
                    
                    for (Flecha flecha : flechas) {
                        if (flecha.origen == nodo || flecha.destino == nodo) {
                            flecha.setResaltada(true);
                        }
                    }
                    
                    panelDerecho.repaint();
                    
                    // Timer para apagar el resaltado
                    javax.swing.Timer timer = new javax.swing.Timer(500, e -> {
                        nodo.setResaltado(false);
                        for (Flecha flecha : flechas) {
                            if (flecha.origen == nodo || flecha.destino == nodo) {
                                flecha.setResaltada(false);
                            }
                        }
                        panelDerecho.repaint();
                    });
                    timer.setRepeats(false);
                    timer.start();
                    break;
                }
            }
        });
    }
    
    private void setupGraphForProblem(String problema) {
        panelDerecho.removeAll();
        nodos.clear();
        flechas.clear();
        procesoCount = 1;
        recursoCount = 1;
        nodoSeleccionado = null;
        panelDerecho.revalidate();
        panelDerecho.repaint();
        actualizarTamanioPanelGrafo(); // <-- AÃ‘ADIDO (SCROLL GRAFO)
        
        if (graphAnimator != null && graphAnimator.isRunning()) {
            graphAnimator.stop();
        }
        graphAnimationActions = new LinkedList<>();
        
        switch (problema.toLowerCase()) {
            case "productor":
                graphAnimationActions.add(() -> createNode("Prod", true, 80, 200));
                graphAnimationActions.add(() -> createNode("Buffer", false, 300, 200));
                graphAnimationActions.add(() -> createNode("Cons", true, 520, 200));
                graphAnimationActions.add(() -> addFlecha(nodos.get(0), nodos.get(1), Color.BLUE));
                graphAnimationActions.add(() -> addFlecha(nodos.get(1), nodos.get(2), Color.MAGENTA));
                break;
            case "filosofos":
                int cx = 350, cy = 250, radio = 180, num = 5;
                for (int i = 0; i < num; i++) {
                    final int id = i;
                    double ang = 2 * Math.PI * id / num - Math.PI/2;
                    graphAnimationActions.add(() -> createNode("F" + id, true,
                        (int) (cx + radio * Math.cos(ang)), (int) (cy + radio * Math.sin(ang))));
                }
                for (int i = 0; i < num; i++) {
                    final int id = i;
                    double ang = 2 * Math.PI * (id + 0.5) / num - Math.PI/2;
                    graphAnimationActions.add(() -> {
                        Nodo fork = createNode("T" + id, false,
                            (int) (cx + (radio - 80) * Math.cos(ang)), (int) (cy + (radio - 80) * Math.sin(ang)));
                        if (id < nodos.size() && (id + 1) % num < nodos.size()) {
                            addFlecha(nodos.get(id), fork, Color.BLUE);
                            addFlecha(nodos.get((id + 1) % num), fork, Color.BLUE);
                        }
                    });
                }
                break;
            case "barbero":
                graphAnimationActions.add(() -> createNode("Barbero", true, 120, 180));
                graphAnimationActions.add(() -> createNode("Sillas", false, 350, 180));
                graphAnimationActions.add(() -> createNode("Cliente", true, 550, 180));
                graphAnimationActions.add(() -> addFlecha(nodos.get(2), nodos.get(1), Color.BLUE));
                graphAnimationActions.add(() -> addFlecha(nodos.get(1), nodos.get(0), Color.MAGENTA));
                break;
            case "fumadores":
                graphAnimationActions.add(() -> createNode("Agente", true, 100, 200));
                graphAnimationActions.add(() -> createNode("Papel", false, 300, 80));
                graphAnimationActions.add(() -> createNode("Tabaco", false, 300, 200));
                graphAnimationActions.add(() -> createNode("Cerillos", false, 300, 320));
                graphAnimationActions.add(() -> createNode("Fumador P", true, 500, 80));
                graphAnimationActions.add(() -> createNode("Fumador T", true, 500, 200));
                graphAnimationActions.add(() -> createNode("Fumador C", true, 500, 320));
                graphAnimationActions.add(() -> addFlecha(nodos.get(0), nodos.get(1), Color.MAGENTA));
                graphAnimationActions.add(() -> addFlecha(nodos.get(0), nodos.get(2), Color.MAGENTA));
                graphAnimationActions.add(() -> addFlecha(nodos.get(0), nodos.get(3), Color.MAGENTA));
                break;
            case "lectores":
                graphAnimationActions.add(() -> createNode("Recurso", false, 300, 200));
                graphAnimationActions.add(() -> createNode("Lector", true, 80, 200));
                graphAnimationActions.add(() -> createNode("Escritor", true, 520, 200));
                graphAnimationActions.add(() -> addFlecha(nodos.get(1), nodos.get(0), Color.BLUE));
                graphAnimationActions.add(() -> addFlecha(nodos.get(2), nodos.get(0), Color.RED));
                break;
            default:
                panelDerecho.revalidate();
                panelDerecho.repaint();
                return;
        }
        
        graphAnimator = new javax.swing.Timer(300, e -> {
            Runnable action = graphAnimationActions.poll();
            if (action != null) {
                action.run();
                panelDerecho.revalidate();
                panelDerecho.repaint();
                actualizarTamanioPanelGrafo(); // <-- AÃ‘ADIDO (SCROLL GRAFO)
            } else {
                ((javax.swing.Timer) e.getSource()).stop();
            }
        });
        graphAnimator.start();
    }
    
    private Nodo createNode(String nombre, boolean esProceso) {
        Nodo n = new Nodo(nombre, esProceso);
        nodos.add(n);
        panelDerecho.add(n);
        return n;
    }
    
    private Nodo createNode(String nombre, boolean esProceso, int x, int y) {
        Nodo n = createNode(nombre, esProceso);
        n.setBounds(x-36, y-36, 72, 72);
        return n;
    }

    // ============================================================================
    // MÃ‰TODO MAIN
    // ============================================================================

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> {
            ProyectoPCyPoto2025 frame = new ProyectoPCyPoto2025();
            frame.setVisible(true);
        });
    }
}