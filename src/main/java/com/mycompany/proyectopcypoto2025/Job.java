// Contenido de Job.java (NUEVO)
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
    
    // Conteo para la barrera (Objetivo 2) y Fallos (Objetivo 6)
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
    
    // Utilizado por el Monitor
    public boolean todosReportadosEnBarrera() {
        // En un caso real, esto usaría el tamaño actual después de un re-shard
        return replicasEnBarrera.get() == g; 
    }
    
    // El hilo líder del trabajo usa este método para verificar si debe continuar
    public boolean estaEjecutando() {
        return estado == Estado.RUN || estado == Estado.COMM || estado == Estado.BARRIER;
    }
    
    @Override
    public String toString() {
        return String.format("%s (g=%d, b=%d) [%s] @Isla%d", id, g, b, estado, islaAsignada);
    }
}