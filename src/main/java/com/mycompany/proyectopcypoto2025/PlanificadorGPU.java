package com.mycompany.proyectopcypoto2025;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

public class PlanificadorGPU {
    private final Map<Integer, Integer> gpuLibres;
    private int tokensGlobal;
    private final Map<Integer, Integer> tokensIsla;
    private int ventanasLibres;

    private final Queue<Job> colaAlta = new ConcurrentLinkedQueue<>();
    private final Queue<Job> colaNormal = new ConcurrentLinkedQueue<>();
    
    private final Object cvAsignacion = new Object();
    private final Object cvBarrera = new Object();
    private final Object cvTokens = new Object();
    private final Object cvVentanas = new Object();
    
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
    
    private Job topeCola() { 
        if (!colaAlta.isEmpty()) return colaAlta.peek();
        return colaNormal.peek();
    }
    
    public void solicitarGang(Job J) throws InterruptedException {
        (J.prioridadAlta ? colaAlta : colaNormal).offer(J);
        
        synchronized (cvAsignacion) {
            while (true) {
                Job currentTop = topeCola();
                boolean esTope = (currentTop == J);
                
                if (esTope && puedeAsignar(J)) {
                    int i = islaElegida(J);
                    if (i != -1) {
                        gpuLibres.put(i, gpuLibres.get(i) - J.g);
                        tokensIsla.put(i, tokensIsla.get(i) - J.b);
                        tokensGlobal -= J.b;
                        
                        (J.prioridadAlta ? colaAlta : colaNormal).remove(J);
                        J.islaAsignada = i;
                        J.estado = Job.Estado.RUN;

                        System.out.printf("[%s] Asignado en Isla %d.\n", J.id, i);
                        
                        cvAsignacion.notifyAll();
                        return;
                    }
                }
                cvAsignacion.wait();
            }
        }
    }
    
    public void entrarBarrera(Job J) throws InterruptedException {
        synchronized (cvBarrera) {
            J.estado = Job.Estado.BARRIER;
            int replicasEnBarrera = J.replicasEnBarrera.incrementAndGet();
            
            if (replicasEnBarrera == J.g) {
                boolean debePreemptarse = preemptTargets.contains(J);
                
                if (debePreemptarse || J.fallosReportados.get() > J.maxFallos) { 
                    preemptTargets.remove(J);
                    liberarRecursos(J);
                    J.replicasEnBarrera.set(0); 
                    J.estado = Job.Estado.PREEMPTED;
                    cvBarrera.notifyAll();
                    throw new InterruptedException("Preemptado/Abortado.");
                } else if (J.fallosReportados.get() > 0) {
                    J.replicasEnBarrera.set(0); 
                    J.fallosReportados.set(0); 
                }
                
                J.replicasEnBarrera.set(0); 
                J.estado = Job.Estado.RUN; 
                cvBarrera.notifyAll();
            } else {
                cvBarrera.wait();
            }
        }
    }

    public void solicitarVentanaColectiva(Job J) throws InterruptedException {
        synchronized (cvVentanas) {
            while (ventanasLibres <= 0) {
                cvVentanas.wait();
            }
            
            synchronized (cvTokens) {
                while (!(tokensGlobal >= J.b && tokensIsla.get(J.islaAsignada) >= J.b)) { 
                    cvTokens.wait();
                }
                
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
                ventanasLibres++;
                tokensGlobal += J.b;
                tokensIsla.put(J.islaAsignada, tokensIsla.get(J.islaAsignada) + J.b);
                J.estado = Job.Estado.RUN;
                
                cvVentanas.notifyAll();
                cvTokens.notifyAll();
            }
        }
    }
    
    public void solicitarPreempcion(Job K) { 
        synchronized (cvBarrera) {
            Job target = null;
            if (!colaNormal.isEmpty()) {
                target = colaNormal.peek();
            }
            
            if (target != null && target.estado != Job.Estado.WAIT) {
                preemptTargets.add(target);
                System.out.printf("[%s] Marcado para Preempción por %s.\n", target.id, K.id);
            }
        }
    }
    
    public void liberarRecursos(Job J) {
        if (J.islaAsignada == -1) return; 
        
        synchronized (cvAsignacion) {
            gpuLibres.put(J.islaAsignada, gpuLibres.get(J.islaAsignada) + J.g);
            tokensIsla.put(J.islaAsignada, tokensIsla.get(J.islaAsignada) + J.b);
            tokensGlobal += J.b;
            
            colaAlta.remove(J);
            colaNormal.remove(J);

            J.islaAsignada = -1;
            
            cvAsignacion.notifyAll();
        }
        
        synchronized (cvTokens) {
            cvTokens.notifyAll(); 
        }
    }

    public Map<Integer, Integer> getGpuLibres() { return gpuLibres; }
    public int getTokensGlobal() { return tokensGlobal; }
    public Map<Integer, Integer> getTokensIsla() { return tokensIsla; }
    public int getVentanasLibres() { return ventanasLibres; }
    public Queue<Job> getColaAlta() { return colaAlta; }
    public Queue<Job> getColaNormal() { return colaNormal; }
}