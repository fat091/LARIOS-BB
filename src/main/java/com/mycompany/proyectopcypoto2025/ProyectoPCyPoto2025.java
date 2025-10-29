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
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.axis.NumberAxis;

public class ProyectoPCyPoto2025 extends JFrame {
    private JMenu archivo, problemas, synchMenu, graficaMenu;
    private JMenuItem procon, filosofos, barbero, fumadores, lecesc, salir;
    private JRadioButtonMenuItem semaforoRadio, conditionRadio, monitorRadio, barreraRadio, mutexRadio;
    private JRadioButtonMenuItem carruselRadio, scrollRadio, acordeonRadio;
    private ButtonGroup techniqueGroup, graficaGroup;
    private JPanel panelIzquierdo;
    private JPanel panelDerecho;
    private JPanel panelGrafo;
    private int procesoCount = 1, recursoCount = 1;
    private final List<Nodo> nodos = new ArrayList<>();
    private final List<Flecha> flechas = new ArrayList<>();
    private Nodo nodoSeleccionado = null;
    private volatile List<Thread> hilosActivos = new ArrayList<>();
    private javax.swing.Timer graphAnimator;
    
    private JScrollPane graficaScrollPane;
    private ChartPanel chartPanel;
    private JFreeChart currentChart;
    private String currentChartType = "carrusel";
    private javax.swing.Timer chartAnimator;
    private DefaultCategoryDataset chartDataset;
    private JLabel lblDeadlock;
    private Map<String, Queue<Number>> seriesData;
    private int timeIndex = 0;

    private enum Technique { SEMAFORO, CONDITION, MONITOR, BARRERA, MUTEX }
    private Technique selectedTechnique = Technique.SEMAFORO;
    private String currentProblemName = null;
    private ProblemaSimulacion simulacionActual = null;

    interface ProblemaSimulacion {
        void iniciarSimulacion();
        void detenerSimulacion();
        JPanel getPanelVisual();
        Thread[] getHilos();
        void notificarEventoGrafo(String evento, String detalles);
    }

    // ============================================================================
    // PRODUCTOR-CONSUMIDOR - TODAS LAS TÉCNICAS
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
        public void iniciarSimulacion() {}
        
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
                parent.actualizarGraficaTiempoReal(evento, almacen.size());
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
            hiloBarbero = new Thread(() -> {
                while (ejecutando && !Thread.currentThread().isInterrupted()) {
                    try {
                        mutexBarbero.lock();
                        try {
                            if (sillasOcupadas == 0) {
                                estadoBarbero = "DURMIENDO (ZZZ)";
                                notificarEventoGrafo("Barbero", "Durmiendo");
                                panelVisual.actualizarEstado();
                            } else {
                                sillasOcupadas--;
                                estadoBarbero = "CORTANDO CABELLO";
                                notificarEventoGrafo("Barbero", "Cortando");
                                panelVisual.actualizarEstado();
                            }
                        } finally {
                            mutexBarbero.unlock();
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
                        
                        mutexSillas.lock();
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
                                panelVisual.actualizarEstado();
                                
                                Thread.sleep(2000);
                                
                                for (int j = 0; j < numSillas; j++) {
                                    if (estadoSillas[j] == 1) {
                                        estadoSillas[j] = 2;
                                        break;
                                    }
                                }
                                notificarEventoGrafo("Cliente", "Siendo atendido");
                                panelVisual.actualizarEstado();
                                
                                Thread.sleep(1000);
                                
                                for (int j = 0; j < numSillas; j++) {
                                    if (estadoSillas[j] == 2) {
                                        estadoSillas[j] = 0;
                                        break;
                                    }
                                }
                            } else {
                                notificarEventoGrafo("Cliente", "Se fue (lleno)");
                            }
                        } finally {
                            mutexSillas.unlock();
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

    // ==========================================================================
    // FUMADORES - COMPLETAR BARRERA Y MUTEX
    // ==========================================================================
    
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
                int fumando = cigarroEncendido ? 1 : 0;
                parent.actualizarGraficaTiempoReal("Fumando", fumando);
            }
        }
        
        public String getEstadoAgente() { return estadoAgente; }
        public String[] getEstadosFumadores() { return estadosFumadores; }
        public boolean[] getIngredientesDisponibles() { return ingredientesDisponibles; }
        public boolean isCigarroEncendido() { return cigarroEncendido; }
        
        @Override
        public void iniciarSimulacion() {}
        
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
            fumadores = new Semaphore[3];
            ingredientes = new Semaphore[3];
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
                        estadoAgente = "PREPARANDO";
                        notificarEventoGrafo("Agente", "Preparando");
                        panelVisual.actualizarEstado();
                        Thread.sleep(ThreadLocalRandom.current().nextInt(1000, 2000));
                        
                        agente.acquire();
                        int ingrediente1 = ThreadLocalRandom.current().nextInt(3);
                        int ingrediente2;
                        do {
                            ingrediente2 = ThreadLocalRandom.current().nextInt(3);
                        } while (ingrediente2 == ingrediente1);
                        
                        estadoAgente = "PONIENDO: " + getNombreIngrediente(ingrediente1) + " y " + getNombreIngrediente(ingrediente2);
                        notificarEventoGrafo("Agente", "Poniendo ingredientes");
                        panelVisual.actualizarEstado();
                        
                        ingredientesDisponibles[ingrediente1] = true;
                        ingredientesDisponibles[ingrediente2] = true;
                        
                        ingredientes[ingrediente1].release();
                        ingredientes[ingrediente2].release();
                        
                        int fumadorQueFalta = 3 - ingrediente1 - ingrediente2;
                        fumadores[fumadorQueFalta].acquire();
                        
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
                            estadosFumadores[id] = "ESPERANDO";
                            notificarEventoGrafo("Fumador" + id, "Esperando");
                            panelVisual.actualizarEstado();
                            
                            ingredientes[id].acquire();
                            
                            estadosFumadores[id] = "FUMANDO";
                            notificarEventoGrafo("Fumador" + id, "Fumando");
                            cigarroEncendido = true;
                            panelVisual.actualizarEstado();
                            
                            Thread.sleep(ThreadLocalRandom.current().nextInt(2000, 3000));
                            
                            estadosFumadores[id] = "TERMINÓ";
                            notificarEventoGrafo("Fumador" + id, "Terminó");
                            cigarroEncendido = false;
                            ingredientesDisponibles[0] = false;
                            ingredientesDisponibles[1] = false;
                            ingredientesDisponibles[2] = false;
                            panelVisual.actualizarEstado();
                            
                            fumadores[id].release();
                            agente.release();
                            
                            Thread.sleep(500);
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
        private final Condition agenteListo = lock.newCondition();
        private final Condition[] fumadoresListos;
        private volatile boolean agentePuedeTrabajar = true;
        
        public FumadoresCondition(ProyectoPCyPoto2025 parent) {
            super(parent);
            fumadoresListos = new Condition[3];
            for (int i = 0; i < 3; i++) {
                fumadoresListos[i] = lock.newCondition();
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
                            while (!agentePuedeTrabajar) {
                                agenteListo.await();
                            }
                        } finally {
                            lock.unlock();
                        }
                        
                        estadoAgente = "PREPARANDO";
                        notificarEventoGrafo("Agente", "Preparando");
                        panelVisual.actualizarEstado();
                        Thread.sleep(ThreadLocalRandom.current().nextInt(1000, 2000));
                        
                        int ingrediente1 = ThreadLocalRandom.current().nextInt(3);
                        int ingrediente2;
                        do {
                            ingrediente2 = ThreadLocalRandom.current().nextInt(3);
                        } while (ingrediente2 == ingrediente1);
                        
                        lock.lock();
                        try {
                            estadoAgente = "PONIENDO: " + getNombreIngrediente(ingrediente1) + " y " + getNombreIngrediente(ingrediente2);
                            notificarEventoGrafo("Agente", "Poniendo ingredientes");
                            panelVisual.actualizarEstado();
                            
                            ingredientesDisponibles[ingrediente1] = true;
                            ingredientesDisponibles[ingrediente2] = true;
                            agentePuedeTrabajar = false;
                            
                            int fumadorQueFalta = 3 - ingrediente1 - ingrediente2;
                            fumadoresListos[fumadorQueFalta].signal();
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
                                estadosFumadores[id] = "ESPERANDO";
                                notificarEventoGrafo("Fumador" + id, "Esperando");
                                panelVisual.actualizarEstado();
                                
                                while (!(ingredientesDisponibles[0] && ingredientesDisponibles[1] && ingredientesDisponibles[2] && 
                                       !ingredientesDisponibles[id])) {
                                    fumadoresListos[id].await();
                                }
                                
                                estadosFumadores[id] = "FUMANDO";
                                notificarEventoGrafo("Fumador" + id, "Fumando");
                                cigarroEncendido = true;
                                panelVisual.actualizarEstado();
                            } finally {
                                lock.unlock();
                            }
                            
                            Thread.sleep(ThreadLocalRandom.current().nextInt(2000, 3000));
                            
                            lock.lock();
                            try {
                                estadosFumadores[id] = "TERMINÓ";
                                notificarEventoGrafo("Fumador" + id, "Terminó");
                                cigarroEncendido = false;
                                ingredientesDisponibles[0] = false;
                                ingredientesDisponibles[1] = false;
                                ingredientesDisponibles[2] = false;
                                agentePuedeTrabajar = true;
                                agenteListo.signal();
                                panelVisual.actualizarEstado();
                            } finally {
                                lock.unlock();
                            }
                            
                            Thread.sleep(500);
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
        private volatile boolean agentePuedeTrabajar = true;
        
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
                            while (!agentePuedeTrabajar) {
                                monitor.wait();
                            }
                        }
                        
                        estadoAgente = "PREPARANDO";
                        notificarEventoGrafo("Agente", "Preparando");
                        panelVisual.actualizarEstado();
                        Thread.sleep(ThreadLocalRandom.current().nextInt(1000, 2000));
                        
                        int ingrediente1 = ThreadLocalRandom.current().nextInt(3);
                        int ingrediente2;
                        do {
                            ingrediente2 = ThreadLocalRandom.current().nextInt(3);
                        } while (ingrediente2 == ingrediente1);
                        
                        synchronized (monitor) {
                            estadoAgente = "PONIENDO: " + getNombreIngrediente(ingrediente1) + " y " + getNombreIngrediente(ingrediente2);
                            notificarEventoGrafo("Agente", "Poniendo ingredientes");
                            panelVisual.actualizarEstado();
                            
                            ingredientesDisponibles[ingrediente1] = true;
                            ingredientesDisponibles[ingrediente2] = true;
                            agentePuedeTrabajar = false;
                            monitor.notifyAll();
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
                                estadosFumadores[id] = "ESPERANDO";
                                notificarEventoGrafo("Fumador" + id, "Esperando");
                                panelVisual.actualizarEstado();
                                
                                while (!(ingredientesDisponibles[0] && ingredientesDisponibles[1] && ingredientesDisponibles[2] && 
                                       !ingredientesDisponibles[id])) {
                                    monitor.wait();
                                }
                                
                                estadosFumadores[id] = "FUMANDO";
                                notificarEventoGrafo("Fumador" + id, "Fumando");
                                cigarroEncendido = true;
                                panelVisual.actualizarEstado();
                            }
                            
                            Thread.sleep(ThreadLocalRandom.current().nextInt(2000, 3000));
                            
                            synchronized (monitor) {
                                estadosFumadores[id] = "TERMINÓ";
                                notificarEventoGrafo("Fumador" + id, "Terminó");
                                cigarroEncendido = false;
                                ingredientesDisponibles[0] = false;
                                ingredientesDisponibles[1] = false;
                                ingredientesDisponibles[2] = false;
                                agentePuedeTrabajar = true;
                                monitor.notifyAll();
                                panelVisual.actualizarEstado();
                            }
                            
                            Thread.sleep(500);
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

    static class FumadoresBarrera extends FumadoresBase {
        private final CyclicBarrier barrera;
        private final Lock lock = new ReentrantLock();
        
        public FumadoresBarrera(ProyectoPCyPoto2025 parent) {
            super(parent);
            barrera = new CyclicBarrier(4);
        }
        
        @Override
        public void iniciarSimulacion() {
            ejecutando = true;
            hiloAgente = new Thread(() -> {
                while (ejecutando && !Thread.currentThread().isInterrupted()) {
                    try {
                        estadoAgente = "PREPARANDO";
                        notificarEventoGrafo("Agente", "Preparando");
                        panelVisual.actualizarEstado();
                        Thread.sleep(ThreadLocalRandom.current().nextInt(1000, 2000));
                        
                        int ingrediente1 = ThreadLocalRandom.current().nextInt(3);
                        int ingrediente2;
                        do {
                            ingrediente2 = ThreadLocalRandom.current().nextInt(3);
                        } while (ingrediente2 == ingrediente1);
                        
                        lock.lock();
                        try {
                            estadoAgente = "PONIENDO: " + getNombreIngrediente(ingrediente1) + " y " + getNombreIngrediente(ingrediente2);
                            notificarEventoGrafo("Agente", "Poniendo ingredientes");
                            ingredientesDisponibles[ingrediente1] = true;
                            ingredientesDisponibles[ingrediente2] = true;
                            panelVisual.actualizarEstado();
                        } finally {
                            lock.unlock();
                        }
                        
                        barrera.await();
                        
                        lock.lock();
                        try {
                            ingredientesDisponibles[0] = false;
                            ingredientesDisponibles[1] = false;
                            ingredientesDisponibles[2] = false;
                        } finally {
                            lock.unlock();
                        }
                        
                    } catch (InterruptedException | BrokenBarrierException e) {
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
                            estadosFumadores[id] = "ESPERANDO";
                            notificarEventoGrafo("Fumador" + id, "Esperando");
                            panelVisual.actualizarEstado();
                            
                            barrera.await();
                            
                            lock.lock();
                            boolean puedeFumar = false;
                            try {
                                if (ingredientesDisponibles[0] && ingredientesDisponibles[1] && ingredientesDisponibles[2] && 
                                    !ingredientesDisponibles[id]) {
                                    puedeFumar = true;
                                    estadosFumadores[id] = "FUMANDO";
                                    notificarEventoGrafo("Fumador" + id, "Fumando");
                                    cigarroEncendido = true;
                                    panelVisual.actualizarEstado();
                                }
                            } finally {
                                lock.unlock();
                            }
                            
                            if (puedeFumar) {
                                Thread.sleep(ThreadLocalRandom.current().nextInt(2000, 3000));
                                
                                lock.lock();
                                try {
                                    estadosFumadores[id] = "TERMINÓ";
                                    notificarEventoGrafo("Fumador" + id, "Terminó");
                                    cigarroEncendido = false;
                                    panelVisual.actualizarEstado();
                                } finally {
                                    lock.unlock();
                                }
                            }
                            
                            Thread.sleep(500);
                        } catch (InterruptedException | BrokenBarrierException e) {
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

    static class FumadoresMutex extends FumadoresBase {
        private final Lock mutexAgente = new ReentrantLock();
        private final Lock[] mutexFumadores;
        
        public FumadoresMutex(ProyectoPCyPoto2025 parent) {
            super(parent);
            mutexFumadores = new Lock[3];
            for (int i = 0; i < 3; i++) {
                mutexFumadores[i] = new ReentrantLock();
            }
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
                        panelVisual.setProductorStatus("Añadido " + i);
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
                            panelVisual.setProductorStatus("Añadido " + i);
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
                            panelVisual.setProductorStatus("Añadido " + i);
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

    static class ProductorConsumidorBarrera extends ProductorConsumidorBase {
        private final CyclicBarrier barrera;
        private final Semaphore mutex = new Semaphore(1);
        
        public ProductorConsumidorBarrera(ProyectoPCyPoto2025 parent) {
            super(parent);
            barrera = new CyclicBarrier(2);
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
                        
                        mutex.acquire();
                        if (almacen.size() < capacidad) {
                            almacen.add("Item-" + (++i));
                            panelVisual.setProductorStatus("Añadido " + i);
                            notificarEventoGrafo("Buffer", "Agregado item");
                            panelVisual.animarProduccion();
                            SwingUtilities.invokeLater(panelVisual::repaint);
                        }
                        mutex.release();
                        
                        barrera.await();
                    } catch (InterruptedException | BrokenBarrierException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            
            consumidor = new Thread(() -> {
                while (ejecutando && !Thread.currentThread().isInterrupted()) {
                    try {
                        barrera.await();
                        
                        panelVisual.setConsumidorStatus("Esperando...");
                        notificarEventoGrafo("Cons", "Esperando");
                        
                        mutex.acquire();
                        if (!almacen.isEmpty()) {
                            String item = almacen.poll();
                            panelVisual.setConsumidorStatus("Consumido: " + item);
                            notificarEventoGrafo("Buffer", "Consumido item");
                            panelVisual.animarConsumo();
                            SwingUtilities.invokeLater(panelVisual::repaint);
                        }
                        mutex.release();
                        
                        Thread.sleep(ThreadLocalRandom.current().nextInt(700, 2000));
                    } catch (InterruptedException | BrokenBarrierException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            
            productor.start();
            consumidor.start();
        }
    }

    static class ProductorConsumidorMutex extends ProductorConsumidorBase {
        private final Lock mutexProd = new ReentrantLock();
        private final Lock mutexCons = new ReentrantLock();
        
        public ProductorConsumidorMutex(ProyectoPCyPoto2025 parent) {
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
                        
                        mutexProd.lock();
                        try {
                            if (almacen.size() < capacidad) {
                                almacen.add("Item-" + (++i));
                                panelVisual.setProductorStatus("Añadido " + i);
                                notificarEventoGrafo("Buffer", "Agregado item");
                                panelVisual.animarProduccion();
                                SwingUtilities.invokeLater(panelVisual::repaint);
                            }
                        } finally {
                            mutexProd.unlock();
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
                        Thread.sleep(ThreadLocalRandom.current().nextInt(700, 2000));
                        
                        mutexCons.lock();
                        try {
                            if (!almacen.isEmpty()) {
                                String item = almacen.poll();
                                panelVisual.setConsumidorStatus("Consumido: " + item);
                                notificarEventoGrafo("Buffer", "Consumido item");
                                panelVisual.animarConsumo();
                                SwingUtilities.invokeLater(panelVisual::repaint);
                            }
                        } finally {
                            mutexCons.unlock();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            
            productor.start();
            consumidor.start();
        }
    }

    // ============================================================================
    // FILÓSOFOS - TODAS LAS TÉCNICAS
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
                int comiendo = 0;
                for (String estado : estados) {
                    if ("COMIENDO".equals(estado)) comiendo++;
                }
                parent.actualizarGraficaTiempoReal("Comiendo", comiendo);
            }
        }
        
        public String[] getEstados() { return estados; }
        public boolean[] getTenedoresOcupados() { return tenedoresOcupados; }
        public int getNumFilosofos() { return numFilosofos; }
        
        @Override
        public void iniciarSimulacion() {}
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

    static class FilosofosBarrera extends FilosofosBase {
        private final CyclicBarrier barrera;
        private final Lock lock = new ReentrantLock();
        
        public FilosofosBarrera(ProyectoPCyPoto2025 parent) {
            super(parent);
            barrera = new CyclicBarrier(numFilosofos);
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
                            
                            barrera.await();
                            
                            lock.lock();
                            try {
                                estados[id] = "HAMBRIENTO";
                                notificarEventoGrafo("F" + id, "Hambriento");
                                panelVisual.actualizarEstado(id, estados[id]);
                                
                                if (puedeComer(id)) {
                                    tenedoresOcupados[id] = true;
                                    notificarEventoGrafo("T" + id, "Ocupado");
                                    tenedoresOcupados[(id + 1) % numFilosofos] = true;
                                    notificarEventoGrafo("T" + ((id + 1) % numFilosofos), "Ocupado");
                                    estados[id] = "COMIENDO";
                                    notificarEventoGrafo("F" + id, "Comiendo");
                                    panelVisual.actualizarEstado(id, estados[id]);
                                }
                            } finally {
                                lock.unlock();
                            }
                            
                            if (estados[id].equals("COMIENDO")) {
                                Thread.sleep(ThreadLocalRandom.current().nextInt(1000, 2000));
                                
                                lock.lock();
                                try {
                                    tenedoresOcupados[id] = false;
                                    notificarEventoGrafo("T" + id, "Libre");
                                    tenedoresOcupados[(id + 1) % numFilosofos] = false;
                                    notificarEventoGrafo("T" + ((id + 1) % numFilosofos), "Libre");
                                } finally {
                                    lock.unlock();
                                }
                            }
                            
                        } catch (InterruptedException | BrokenBarrierException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                });
                filosofos[i].start();
            }
        }
    }

    static class FilosofosMutex extends FilosofosBase {
        private final Lock[] mutexTenedores;
        
        public FilosofosMutex(ProyectoPCyPoto2025 parent) {
            super(parent);
            mutexTenedores = new Lock[numFilosofos];
            for (int i = 0; i < numFilosofos; i++) {
                mutexTenedores[i] = new ReentrantLock();
            }
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
                            
                            int left = id;
                            int right = (id + 1) % numFilosofos;
                            if (id % 2 == 0) {
                                mutexTenedores[left].lock();
                                mutexTenedores[right].lock();
                            } else {
                                mutexTenedores[right].lock();
                                mutexTenedores[left].lock();
                            }
                            
                            try {
                                tenedoresOcupados[left] = true;
                                notificarEventoGrafo("T" + left, "Ocupado");
                                tenedoresOcupados[right] = true;
                                notificarEventoGrafo("T" + right, "Ocupado");
                                estados[id] = "COMIENDO";
                                notificarEventoGrafo("F" + id, "Comiendo");
                                panelVisual.actualizarEstado(id, estados[id]);
                                
                                Thread.sleep(ThreadLocalRandom.current().nextInt(1000, 2000));
                                
                                tenedoresOcupados[left] = false;
                                notificarEventoGrafo("T" + left, "Libre");
                                tenedoresOcupados[right] = false;
                                notificarEventoGrafo("T" + right, "Libre");
                            } finally {
                                mutexTenedores[left].unlock();
                                mutexTenedores[right].unlock();
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

    // ============================================================================
    // BARBERO - BARRERA Y MUTEX
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
                parent.actualizarGraficaTiempoReal("Clientes", sillasOcupadas);
            }
        }
        
        public String getEstadoBarbero() { return estadoBarbero; }
        public int[] getEstadoSillas() { return estadoSillas; }
        public int getNumSillas() { return numSillas; }
        
        @Override
        public void iniciarSimulacion() {}
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

    static class BarberoBarrera extends BarberoBase {
        private final CyclicBarrier barrera;
        private final Lock lock = new ReentrantLock();
        
        public BarberoBarrera(ProyectoPCyPoto2025 parent) {
            super(parent);
            barrera = new CyclicBarrier(2);
        }
        
        @Override
        public void iniciarSimulacion() {
            ejecutando = true;
            hiloBarbero = new Thread(() -> {
                while (ejecutando && !Thread.currentThread().isInterrupted()) {
                    try {
                        lock.lock();
                        try {
                            if (sillasOcupadas == 0) {
                                estadoBarbero = "DURMIENDO (ZZZ)";
                                notificarEventoGrafo("Barbero", "Durmiendo");
                                panelVisual.actualizarEstado();
                            }
                        } finally {
                            lock.unlock();
                        }
                        
                        Thread.sleep(1000);
                        
                        lock.lock();
                        try {
                            if (sillasOcupadas > 0) {
                                sillasOcupadas--;
                                estadoBarbero = "CORTANDO CABELLO";
                                notificarEventoGrafo("Barbero", "Cortando");
                                panelVisual.actualizarEstado();
                                
                                barrera.await();
                                Thread.sleep(ThreadLocalRandom.current().nextInt(2000, 4000));
                                barrera.await();
                            }
                        } finally {
                            lock.unlock();
                        }
                    } catch (InterruptedException | BrokenBarrierException e) {
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
                        boolean aceptado = false;
                        try {
                            if (sillasOcupadas < numSillas) {
                                sillasOcupadas++;
                                aceptado = true;
                                notificarEventoGrafo("Sillas", "Cliente esperando");
                                for (int j = 0; j < numSillas; j++) {
                                    if (estadoSillas[j] == 0) {
                                        estadoSillas[j] = 1;
                                        break;
                                    }
                                }
                            } else {
                                notificarEventoGrafo("Cliente", "Se fue (lleno)");
                            }
                        } finally {
                            lock.unlock();
                        }
                        
                        if (aceptado) {
                            barrera.await();
                            
                            lock.lock();
                            try {
                                for (int j = 0; j < numSillas; j++) {
                                    if (estadoSillas[j] == 1) {
                                        estadoSillas[j] = 2;
                                        break;
                                    }
                                }
                                notificarEventoGrafo("Cliente", "Siendo atendido");
                                panelVisual.actualizarEstado();
                            } finally {
                                lock.unlock();
                            }
                            
                            barrera.await();
                            
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
                        }
                    } catch (InterruptedException | BrokenBarrierException e) {
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

    static class BarberoMutex extends BarberoBase {
        private final Lock mutexBarbero = new ReentrantLock();
        private final Lock mutexSillas = new ReentrantLock();
        
        public BarberoMutex(ProyectoPCyPoto2025 parent) {
            super(parent);
        }
        
        @Override
        public void iniciarSimulacion() {
            ejecutando = true;