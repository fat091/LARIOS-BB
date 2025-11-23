package com.mycompany.proyectopcypoto2025;

import java.util.concurrent.atomic.AtomicInteger;

public class Job {
    public enum Estado {
        WAIT, RUN, BARRIER, COMM, PREEMPTED, ABORTED, FINISHED
    }

    public final String id;
    public final int g;
    public final int b;
    public final int maxFallos;
    public final boolean prioridadAlta;
    
    public volatile Estado estado = Estado.WAIT;
    public volatile int islaAsignada = -1;
    public final AtomicInteger replicasEnBarrera = new AtomicInteger(0);
    public final AtomicInteger fallosReportados = new AtomicInteger(0);

    public Job(int id, int g, int b, int maxFallos, boolean prioridadAlta) {
        this.id = "J" + id;
        this.g = g;
        this.b = b;
        this.maxFallos = maxFallos;
        this.prioridadAlta = prioridadAlta;
    }

    public boolean estaEjecutando() {
        return estado == Estado.RUN || estado == Estado.BARRIER || estado == Estado.COMM;
    }

    public void reportarFallo() {
        fallosReportados.incrementAndGet();
    }
}