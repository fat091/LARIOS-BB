package com.mycompany.proyectopcypoto2025;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

public class PlanificadorGPU {
    // --- Recursos (Variables del Monitor) ---
    private final Map<Integer, Integer> gpuLibres; // por isla
    private int tokensGlobal;
    private final Map<Integer, Integer> tokensIsla; // por isla
    private int ventanasLibres;

    private final Queue<Job> colaAlta = new ConcurrentLinkedQueue<>();
    private final Queue<Job> colaNormal = new ConcurrentLinkedQueue<>();
    
    // Variables de condición simuladas
    private final Object cvAsignacion = new Object(); // para solicitarGang
    private final Object cvBarrera = new Object(); // para entrarBarrera
    private final Object cvTokens = new Object(); // para solicitarVentanaColectiva (tokens)
    private final Object cvVentanas = new Object(); // para solicitarVentanaColectiva (slots K)
    
    // Marcador de trabajos a preemptar
    private final Set<Job> preemptTargets = new HashSet<>();
    
    public PlanificadorGPU(int numIslas, int gpusPorIsla, int tokensG, int tokensI, int K) {
        gpuLibres = new HashMap<>();
        tokensIsla = new HashMap<>();
        for (int i = 0; i < numIslas; i++) {
            gpuLibres.put(i, gpusPorIsla);
            tokensIsla.put(i, tokensI);
        }
        this.tokensGlobal = tokensG;
        this.ventanasLibres = K;
    }

    // --- Funciones de Utilidad y Auxiliares ---

    /**
     * Verifica si hay suficientes recursos (GPUs, Tokens de Isla, Tokens Globales)
     * disponibles en alguna isla para asignar el trabajo J.
     */
    private boolean puedeAsignar(Job J) { 
        for (Map.Entry<Integer, Integer> entry : gpuLibres.entrySet()) {
            int i = entry.getKey();
            if (entry.getValue() >= J.g && tokensIsla.get(i) >= J.b && tokensGlobal >= J.b) {
                return true;
            }
        }
        return false;
    }
    
    private int islaElegida(Job J) {
        List<Integer> islasAdmisibles = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : gpuLibres.entrySet()) {
            int i = entry.getKey();
            if (entry.getValue() >= J.g && tokensIsla.get(i) >= J.b && tokensGlobal >= J.b) {
                islasAdmisibles.add(i);
            }
        }
        if (islasAdmisibles.isEmpty()) return -1;
        return islasAdmisibles.get(ThreadLocalRandom.current().nextInt(islasAdmisibles.size()));
    }
    
    /**
     * Devuelve el trabajo con mayor prioridad y más tiempo esperando.
     */
    private Job topeCola() { 
        if (!colaAlta.isEmpty()) return colaAlta.peek();
        return colaNormal.peek();
    }
    
    // --- Lógica del Monitor ---
    
    /**
     * 1. Asignación Gang (Objetivo 1 y 5)
     * Espera hasta que el trabajo sea el tope de la cola y los recursos estén disponibles.
     */
    public void solicitarGang(Job J) throws InterruptedException {
        // Añadir a la cola y esperar a ser el tope Y que haya recursos
        (J.prioridadAlta ? colaAlta : colaNormal).offer(J);
        
        synchronized (cvAsignacion) {
            while (true) {
                Job currentTop = topeCola();
                boolean esTope = (currentTop == J);
                
                if (esTope && puedeAsignar(J)) {
                    int i = islaElegida(J);
                    if (i != -1) {
                        // Asignación atómica de recursos
                        gpuLibres.put(i, gpuLibres.get(i) - J.g);
                        tokensIsla.put(i, tokensIsla.get(i) - J.b);
                        tokensGlobal -= J.b;
                        
                        // Eliminar de la cola y actualizar estado
                        (J.prioridadAlta ? colaAlta : colaNormal).remove(J);
                        J.islaAsignada = i;
                        J.estado = Job.Estado.RUN;

                        System.out.printf("[%s] Asignado en Isla %d.\n", J.id, i);
                        
                        // Notificar a otros que podrían ser el nuevo tope
                        cvAsignacion.notifyAll();
                        return;
                    }
                }
                cvAsignacion.wait();
            }
        }
    }
    
    /**
     * 2. Barrera por trabajo (Objetivo 2, 4 y 6)
     * Sincroniza todas las réplicas y verifica los puntos seguros (preempción/fallos).
     */
    public void entrarBarrera(Job J) throws InterruptedException {
        synchronized (cvBarrera) {
            J.estado = Job.Estado.BARRIER;
            int replicasEnBarrera = J.replicasEnBarrera.incrementAndGet();
            
            if (replicasEnBarrera == J.g) { // Ultima réplica llegó (Punto Seguro)
                boolean debePreemptarse = preemptTargets.contains(J);
                
                if (debePreemptarse || J.fallosReportados.get() > J.maxFallos) { 
                    // Preemptado o Fallo catastrófico
                    preemptTargets.remove(J);
                    liberarRecursos(J);
                    J.replicasEnBarrera.set(0); 
                    J.estado = Job.Estado.PREEMPTED;
                    cvBarrera.notifyAll(); // Despertar a sus pares para que lancen InterruptedException
                    throw new InterruptedException("Preemptado/Abortado.");
                } else if (J.fallosReportados.get() > 0) {
                    // Re-shard (reconfiguración)
                    // (Lógica de re-shard simulada)
                    J.replicasEnBarrera.set(0); 
                    J.fallosReportados.set(0); 
                }
                
                J.replicasEnBarrera.set(0); 
                J.estado = Job.Estado.RUN; 
                cvBarrera.notifyAll(); // Despertar a todas las réplicas para que continúen
            } else {
                cvBarrera.wait(); // Esperar a sus pares
            }
        }
    }

    /**
     * 3. Control de Ventana y Tokens (Objetivo 3)
     * Espera por un slot de ventana (K) Y tokens (Global e Isla).
     */
    public void solicitarVentanaColectiva(Job J) throws InterruptedException {
        synchronized (cvVentanas) {
            while (ventanasLibres <= 0) { // Esperar por slot de ventana (K slots)
                cvVentanas.wait();
            }
            
            synchronized (cvTokens) {
                while (!(tokensGlobal >= J.b && tokensIsla.get(J.islaAsignada) >= J.b)) { 
                    cvTokens.wait();
                }
                
                // Consumir recursos (Asignación atómica de ventana y tokens)
                ventanasLibres--;
                tokensGlobal -= J.b;
                tokensIsla.put(J.islaAsignada, tokensIsla.get(J.islaAsignada) - J.b);
                
                J.estado = Job.Estado.COMM;
            }
        }
    }
    
    public void liberarVentanaColectiva(Job J) {
        synchronized (cvVentanas) {
            synchronized (cvTokens) {
                // Liberar recursos
                ventanasLibres++;
                tokensGlobal += J.b;
                tokensIsla.put(J.islaAsignada, tokensIsla.get(J.islaAsignada) + J.b);
                J.estado = Job.Estado.RUN;
                
                // Despertar a otros en espera
                cvVentanas.notifyAll();
                cvTokens.notifyAll();
            }
        }
    }
    
    /**
     * 4. Preempción (Objetivo 4)
     * Marca un trabajo de menor prioridad para ser interrumpido en la próxima barrera.
     */
    public void solicitarPreempcion(Job K) { 
        synchronized (cvBarrera) {
            // Lógica simplificada: encontrar un objetivo no-WAIT para marcar
            Job target = null;
            if (!colaNormal.isEmpty()) {
                target = colaNormal.peek();
            } else if (!colaAlta.isEmpty()) {
                // ... (lógica compleja de prioridad)
            }
            
            if (target != null && target.estado != Job.Estado.WAIT) {
                preemptTargets.add(target);
                System.out.printf("[%s] Marcado para Preempción por %s.\n", target.id, K.id);
            }
        }
    }
    
    /**
     * Libera todos los recursos asignados por el Gang Scheduling.
     */
    public void liberarRecursos(Job J) {
        if (J.islaAsignada == -1) return; 
        
        synchronized (cvAsignacion) {
            // Liberar la asignación gang y los tokens retenidos.
            gpuLibres.put(J.islaAsignada, gpuLibres.get(J.islaAsignada) + J.g);
            tokensIsla.put(J.islaAsignada, tokensIsla.get(J.islaAsignada) + J.b);
            tokensGlobal += J.b;
            
            colaAlta.remove(J);
            colaNormal.remove(J);

            J.islaAsignada = -1;
            
            // Despertar a trabajos esperando por Gang
            cvAsignacion.notifyAll();
        }
        
        synchronized (cvTokens) {
            cvTokens.notifyAll(); 
        }
    }

    // Getters para la GUI
    public Map<Integer, Integer> getGpuLibres() { return gpuLibres; }
    public int getTokensGlobal() { return tokensGlobal; }
    public Map<Integer, Integer> getTokensIsla() { return tokensIsla; }
    public int getVentanasLibres() { return ventanasLibres; }
    public Queue<Job> getColaAlta() { return colaAlta; }
    public Queue<Job> getColaNormal() { return colaNormal; }
}