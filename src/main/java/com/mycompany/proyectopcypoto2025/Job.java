package com.mycompany.proyectopcypoto2025;

import java.util.concurrent.atomic.AtomicInteger;

public class Job {
    public enum Estado { WAIT, RUN, BARRIER, COMM, PREEMPTED, ABORTED, FINISHED }
    public final String id;
    public final int g; // GPUs requeridas (g(J))
    public final int b; // Tokens de red requeridos (b(J))
    public final int maxFallos; // f(J)
    public final boolean prioridadAlta;
    public int islaAsignada = -1;
    public Estado estado = Estado.WAIT;
    
    public AtomicInteger replicasEnBarrera = new AtomicInteger(0);
    public AtomicInteger fallosReportados = new AtomicInteger(0);
    
    public Job(int id, int g, int b, int maxFallos, boolean alta) {
        this.id = "J" + id;
        this.g = g;
        this.b = b;
        this.maxFallos = maxFallos;
        this.prioridadAlta = alta;
    }
    
    public void reportarFallo() {
        fallosReportados.incrementAndGet();
    }
    
    public boolean todosReportadosEnBarrera() {
        return replicasEnBarrera.get() == g; 
    }
    
    public boolean estaEjecutando() {
        return estado == Estado.RUN || estado == Estado.COMM || estado == Estado.BARRIER;
    }
    
    @Override
    public String toString() {
        return String.format("%s (g=%d, b=%d) [%s] @Isla%d", id, g, b, estado, islaAsignada);
    }
}
