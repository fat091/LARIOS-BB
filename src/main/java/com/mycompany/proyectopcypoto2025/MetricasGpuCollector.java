package com.mycompany.proyectopcypoto2025;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Recolector de métricas para el GPU Cluster
 */
public class MetricasGpuCollector {
    
    private final ConcurrentHashMap<SyncMode, AtomicInteger> conflictos = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<SyncMode, AtomicInteger> asignaciones = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<SyncMode, AtomicInteger> preempciones = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<SyncMode, AtomicInteger> tiemposEspera = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<SyncMode, AtomicInteger> barrerasCompletadas = new ConcurrentHashMap<>();
    
    private final AtomicInteger jobsEnCola = new AtomicInteger(0);
    private final AtomicInteger jobsEjecutando = new AtomicInteger(0);
    private final AtomicInteger recursosLibres = new AtomicInteger(0);
    
    private SyncMode modoActual = SyncMode.MONITORES;
    
    public MetricasGpuCollector() {
        for (SyncMode modo : SyncMode.values()) {
            conflictos.put(modo, new AtomicInteger(0));
            asignaciones.put(modo, new AtomicInteger(0));
            preempciones.put(modo, new AtomicInteger(0));
            tiemposEspera.put(modo, new AtomicInteger(0));
            barrerasCompletadas.put(modo, new AtomicInteger(0));
        }
    }
    
    public void setModoActual(SyncMode modo) {
        this.modoActual = modo;
    }
    
    public void registrarConflicto() {
        conflictos.get(modoActual).incrementAndGet();
    }
    
    public void registrarAsignacion() {
        asignaciones.get(modoActual).incrementAndGet();
    }
    
    public void registrarPreempcion() {
        preempciones.get(modoActual).incrementAndGet();
    }
    
    public void registrarTiempoEspera(int ms) {
        tiemposEspera.get(modoActual).addAndGet(ms);
    }
    
    public void registrarBarreraCompletada() {
        barrerasCompletadas.get(modoActual).incrementAndGet();
    }
    
    public void actualizarJobsEnCola(int cantidad) {
        jobsEnCola.set(cantidad);
    }
    
    public void actualizarJobsEjecutando(int cantidad) {
        jobsEjecutando.set(cantidad);
    }
    
    public void actualizarRecursosLibres(int cantidad) {
        recursosLibres.set(cantidad);
    }
    
    public int getConflictos(SyncMode modo) {
        return conflictos.get(modo).get();
    }
    
    public int getAsignaciones(SyncMode modo) {
        return asignaciones.get(modo).get();
    }
    
    public int getPreempciones(SyncMode modo) {
        return preempciones.get(modo).get();
    }
    
    public int getTiemposEspera(SyncMode modo) {
        return tiemposEspera.get(modo).get();
    }
    
    public int getBarrerasCompletadas(SyncMode modo) {
        return barrerasCompletadas.get(modo).get();
    }
    
    public int getJobsEnCola() {
        return jobsEnCola.get();
    }
    
    public int getJobsEjecutando() {
        return jobsEjecutando.get();
    }
    
    public int getRecursosLibres() {
        return recursosLibres.get();
    }
    
    public double calcularEficiencia(SyncMode modo) {
        int a = asignaciones.get(modo).get();
        int c = conflictos.get(modo).get();
        int p = preempciones.get(modo).get();
        int total = a + c + p;
        return total > 0 ? (100.0 * a / total) : 0.0;
    }
    
    public double calcularTiempoEsperaPromedio(SyncMode modo) {
        int total = tiemposEspera.get(modo).get();
        int asig = asignaciones.get(modo).get();
        return asig > 0 ? (double) total / asig : 0.0;
    }
    
    public void reset() {
        for (SyncMode modo : SyncMode.values()) {
            conflictos.get(modo).set(0);
            asignaciones.get(modo).set(0);
            preempciones.get(modo).set(0);
            tiemposEspera.get(modo).set(0);
            barrerasCompletadas.get(modo).set(0);
        }
        jobsEnCola.set(0);
        jobsEjecutando.set(0);
        recursosLibres.set(0);
    }
}