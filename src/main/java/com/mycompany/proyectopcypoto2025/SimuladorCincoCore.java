package com.mycompany.proyectopcypoto2025;

import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.io.*;

/**
 * Simula 5 cores ejecutando diferentes mecanismos de sincronización
 * NO requiere MPJ Express - usa threads de Java
 */
public class SimuladorCincoCore {
    
    private final ExecutorService executor;
    private final MetricasGpuCollector collector;
    private volatile boolean running = false;
    
    public SimuladorCincoCore(MetricasGpuCollector collector) {
        this.collector = collector;
        this.executor = Executors.newFixedThreadPool(5);
    }
    
    public void iniciar() {
        if (running) return;
        running = true;
        
        System.out.println("=== Iniciando simulación de 5 cores ===");
        
        // Core 0: Semáforos
        executor.submit(() -> ejecutarCore(0, SyncMode.SEMAFOROS));
        
        // Core 1: Variables de Condición
        executor.submit(() -> ejecutarCore(1, SyncMode.VAR_CONDICION));
        
        // Core 2: Monitores
        executor.submit(() -> ejecutarCore(2, SyncMode.MONITORES));
        
        // Core 3: Mutex
        executor.submit(() -> ejecutarCore(3, SyncMode.MUTEX));
        
        // Core 4: Barreras
        executor.submit(() -> ejecutarCore(4, SyncMode.BARRERAS));
    }
    
    private void ejecutarCore(int coreId, SyncMode modo) {
        Thread.currentThread().setName("Core-" + coreId + "-" + modo);
        System.out.printf("[Core %d] Ejecutando con %s\n", coreId, modo);
        
        switch(modo) {
            case SEMAFOROS -> ejecutarConSemaforos(coreId);
            case VAR_CONDICION -> ejecutarConVariablesCondicion(coreId);
            case MONITORES -> ejecutarConMonitores(coreId);
            case MUTEX -> ejecutarConMutex(coreId);
            case BARRERAS -> ejecutarConBarreras(coreId);
        }
    }
    
    // ============= SEMÁFOROS =============
    private void ejecutarConSemaforos(int coreId) {
        Semaphore sem = new Semaphore(1);
        int recursoCompartido = 0;
        int exitosas = 0, conflictos = 0;
        
        for (int i = 0; i < 200 && running; i++) {
            try {
                if (sem.tryAcquire(50, TimeUnit.MILLISECONDS)) {
                    recursoCompartido++;
                    Thread.sleep(ThreadLocalRandom.current().nextInt(5, 15));
                    exitosas++;
                    sem.release();
                } else {
                    conflictos++;
                }
                
                // Actualizar métricas cada 10 ops
                if (i % 10 == 0) {
                    collector.actualizarMetricaCore(SyncMode.SEMAFOROS, exitosas, conflictos);
                }
                
                Thread.sleep(ThreadLocalRandom.current().nextInt(20, 50));
                
            } catch (InterruptedException e) {
                conflictos++;
            }
        }
        
        System.out.printf("[Core %d - SEMAFOROS] Finalizado. Exitosas: %d, Conflictos: %d\n", 
                         coreId, exitosas, conflictos);
    }
    
    // ============= VARIABLES DE CONDICIÓN =============
    private void ejecutarConVariablesCondicion(int coreId) {
        Lock lock = new ReentrantLock();
        Condition condition = lock.newCondition();
        int exitosas = 0, conflictos = 0;
        
        for (int i = 0; i < 200 && running; i++) {
            lock.lock();
            try {
                // Simular espera condicional
                if (ThreadLocalRandom.current().nextBoolean()) {
                    condition.await(ThreadLocalRandom.current().nextInt(10, 30), TimeUnit.MILLISECONDS);
                    exitosas++;
                } else {
                    conflictos++;
                }
                condition.signal();
                
                if (i % 10 == 0) {
                    collector.actualizarMetricaCore(SyncMode.VAR_CONDICION, exitosas, conflictos);
                }
                
            } catch (InterruptedException e) {
                conflictos++;
            } finally {
                lock.unlock();
            }
            
            try {
                Thread.sleep(ThreadLocalRandom.current().nextInt(20, 50));
            } catch (InterruptedException e) {}
        }
        
        System.out.printf("[Core %d - VAR_CONDICION] Finalizado. Exitosas: %d, Conflictos: %d\n", 
                         coreId, exitosas, conflictos);
    }
    
    // ============= MONITORES =============
    private void ejecutarConMonitores(int coreId) {
        Monitor monitor = new Monitor();
        int exitosas = 0, conflictos = 0;
        
        for (int i = 0; i < 200 && running; i++) {
            try {
                monitor.entrar();
                Thread.sleep(ThreadLocalRandom.current().nextInt(5, 15));
                monitor.salir();
                exitosas++;
                
                if (i % 10 == 0) {
                    collector.actualizarMetricaCore(SyncMode.MONITORES, exitosas, conflictos);
                }
                
                Thread.sleep(ThreadLocalRandom.current().nextInt(20, 50));
                
            } catch (InterruptedException e) {
                conflictos++;
            }
        }
        
        System.out.printf("[Core %d - MONITORES] Finalizado. Exitosas: %d, Conflictos: %d\n", 
                         coreId, exitosas, conflictos);
    }
    
    // ============= MUTEX =============
    private void ejecutarConMutex(int coreId) {
        Lock mutex = new ReentrantLock();
        int exitosas = 0, conflictos = 0;
        
        for (int i = 0; i < 200 && running; i++) {
            try {
                if (mutex.tryLock(50, TimeUnit.MILLISECONDS)) {
                    try {
                        Thread.sleep(ThreadLocalRandom.current().nextInt(5, 15));
                        exitosas++;
                    } finally {
                        mutex.unlock();
                    }
                } else {
                    conflictos++;
                }
                
                if (i % 10 == 0) {
                    collector.actualizarMetricaCore(SyncMode.MUTEX, exitosas, conflictos);
                }
                
                Thread.sleep(ThreadLocalRandom.current().nextInt(20, 50));
                
            } catch (InterruptedException e) {
                conflictos++;
            }
        }
        
        System.out.printf("[Core %d - MUTEX] Finalizado. Exitosas: %d, Conflictos: %d\n", 
                         coreId, exitosas, conflictos);
    }
    
    // ============= BARRERAS =============
    private final CyclicBarrier barrera = new CyclicBarrier(5);
    
    private void ejecutarConBarreras(int coreId) {
        int exitosas = 0, conflictos = 0;
        
        for (int i = 0; i < 200 && running; i++) {
            try {
                // Esperar en la barrera
                barrera.await(100, TimeUnit.MILLISECONDS);
                Thread.sleep(ThreadLocalRandom.current().nextInt(5, 15));
                exitosas++;
                
                if (i % 10 == 0) {
                    collector.actualizarMetricaCore(SyncMode.BARRERAS, exitosas, conflictos);
                }
                
                Thread.sleep(ThreadLocalRandom.current().nextInt(20, 50));
                
            } catch (InterruptedException | BrokenBarrierException | TimeoutException e) {
                conflictos++;
            }
        }
        
        System.out.printf("[Core %d - BARRERAS] Finalizado. Exitosas: %d, Conflictos: %d\n", 
                         coreId, exitosas, conflictos);
    }
    
    public void detener() {
        running = false;
        executor.shutdownNow();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.err.println("Error al detener cores: " + e.getMessage());
        }
        System.out.println("=== Simulación de 5 cores detenida ===");
    }
    
    public boolean estaEjecutando() {
        return running;
    }
    
    // Clase Monitor auxiliar
    static class Monitor {
        private boolean ocupado = false;
        
        public synchronized void entrar() throws InterruptedException {
            while (ocupado) {
                wait();
            }
            ocupado = true;
        }
        
        public synchronized void salir() {
            ocupado = false;
            notify();
        }
    }
}