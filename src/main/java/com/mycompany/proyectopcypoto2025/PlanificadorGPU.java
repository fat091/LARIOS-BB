<<<<<<< HEAD
=======
// Contenido de PlanificadorGPU.java (NUEVO - EL MONITOR)
>>>>>>> 23e934d68df5d3d253bd9fa0253db12338618c8b
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
    
<<<<<<< HEAD
    // Colas de espera
=======
    // Colas de espera (Objetivo 5: Evitar inanición/Aging)
>>>>>>> 23e934d68df5d3d253bd9fa0253db12338618c8b
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
<<<<<<< HEAD
    private boolean puedeAsignar(Job J) { 
=======
    private boolean puedeAsignar(Job J) { // (Objetivo 1: Asignación atómica)
>>>>>>> 23e934d68df5d3d253bd9fa0253db12338618c8b
        for (Map.Entry<Integer, Integer> entry : gpuLibres.entrySet()) {
            int i = entry.getKey();
            if (entry.getValue() >= J.g && tokensIsla.get(i) >= J.b && tokensGlobal >= J.b) {
                return true;
            }
        }
        return false;
    }
    
    private int islaElegida(Job J) {
<<<<<<< HEAD
=======
        // Política de ejemplo: elige la isla al azar entre las válidas
>>>>>>> 23e934d68df5d3d253bd9fa0253db12338618c8b
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
    
<<<<<<< HEAD
    private Job topeCola() { 
=======
    private Job topeCola() { // (Objetivo 5: Fairness/Aging)
>>>>>>> 23e934d68df5d3d253bd9fa0253db12338618c8b
        if (!colaAlta.isEmpty()) return colaAlta.peek();
        return colaNormal.peek();
    }
    
    // --- Lógica del Monitor ---
    
<<<<<<< HEAD
    public void solicitarGang(Job J) throws InterruptedException {
=======
    // 1. Asignación Gang (Objetivo 1 y 5)
    public void solicitarGang(Job J) throws InterruptedException {
        // Añadir a la cola y esperar a ser el tope Y que haya recursos
>>>>>>> 23e934d68df5d3d253bd9fa0253db12338618c8b
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
<<<<<<< HEAD
                        
=======
                        System.out.printf("[%s] Asignado en Isla %d.\n", J.id, i);
                        
                        // Notificar a otros que podrían ser el nuevo tope
>>>>>>> 23e934d68df5d3d253bd9fa0253db12338618c8b
                        cvAsignacion.notifyAll();
                        return;
                    }
                }
                cvAsignacion.wait();
            }
        }
    }
    
<<<<<<< HEAD
=======
    // 2. Barrera por trabajo (Objetivo 2, 4 y 6)
>>>>>>> 23e934d68df5d3d253bd9fa0253db12338618c8b
    public void entrarBarrera(Job J) throws InterruptedException {
        synchronized (cvBarrera) {
            J.estado = Job.Estado.BARRIER;
            int replicasEnBarrera = J.replicasEnBarrera.incrementAndGet();
            
            if (replicasEnBarrera == J.g) { // Ultima réplica llegó (Punto Seguro)
<<<<<<< HEAD
                boolean debePreemptarse = preemptTargets.contains(J);
                
                if (debePreemptarse || J.fallosReportados.get() > J.maxFallos) { 
                    preemptTargets.remove(J);
                    liberarRecursos(J);
                    J.replicasEnBarrera.set(0); 
                    J.estado = Job.Estado.PREEMPTED;
                    cvBarrera.notifyAll(); 
                    throw new InterruptedException("Preemptado/Abortado.");
                } else if (J.fallosReportados.get() > 0) {
                    // Re-shard
                    J.replicasEnBarrera.set(0); 
                    J.fallosReportados.set(0); 
                }
                
                J.replicasEnBarrera.set(0); 
                J.estado = Job.Estado.RUN; 
                cvBarrera.notifyAll(); 
            } else {
                cvBarrera.wait(); 
=======
                // Objetivo 4: Preempción segura y Objetivo 6: Tolerancia a fallos
                boolean debePreemptarse = preemptTargets.contains(J);
                
                if (debePreemptarse || J.fallosReportados.get() > J.maxFallos) { 
                    // Preemptado o Fallo catastrófico
                    preemptTargets.remove(J);
                    liberarRecursos(J);
                    J.replicasEnBarrera.set(0); 
                    J.estado = Job.Estado.PREEMPTED; // O ABORTED
                    System.out.printf("[%s] PREEMPTED/ABORTED en barrera.\n", J.id);
                    cvBarrera.notifyAll(); // Despertar a sus pares para que lancen InterruptedException
                    throw new InterruptedException("Preemptado/Abortado.");
                } else if (J.fallosReportados.get() > 0) {
                    // Objetivo 6: Re-shard (reconfiguración)
                    int g_viejo = J.g;
                    // En un caso real: aquí se recalcula g(J) y se reasignan recursos
                    // Simulamos que el trabajo sigue con menos réplicas.
                    J.replicasEnBarrera.set(0); // resetear contador
                    J.fallosReportados.set(0); // resetear fallos
                    System.out.printf("[%s] RE-SHARD exitoso. Continúa.\n", J.id);
                }
                
                J.replicasEnBarrera.set(0); // resetear para el próximo ciclo
                J.estado = Job.Estado.RUN; 
                cvBarrera.notifyAll(); // Despertar a todas las réplicas para que continúen
            } else {
                cvBarrera.wait(); // Esperar a sus pares
>>>>>>> 23e934d68df5d3d253bd9fa0253db12338618c8b
            }
        }
    }

<<<<<<< HEAD
    public void solicitarVentanaColectiva(Job J) throws InterruptedException {
        synchronized (cvVentanas) {
            while (ventanasLibres <= 0) { 
=======
    // 3. Control de Ventana y Tokens (Objetivo 3)
    public void solicitarVentanaColectiva(Job J) throws InterruptedException {
        synchronized (cvVentanas) {
            while (ventanasLibres <= 0) { // Esperar por slot de ventana (K slots)
>>>>>>> 23e934d68df5d3d253bd9fa0253db12338618c8b
                cvVentanas.wait();
            }
            
            synchronized (cvTokens) {
<<<<<<< HEAD
=======
                // Verificar tokens globales y de isla
>>>>>>> 23e934d68df5d3d253bd9fa0253db12338618c8b
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
                
<<<<<<< HEAD
=======
                // Despertar a otros en espera (Objetivo 3)
>>>>>>> 23e934d68df5d3d253bd9fa0253db12338618c8b
                cvVentanas.notifyAll();
                cvTokens.notifyAll();
            }
        }
    }
    
<<<<<<< HEAD
=======
    // 4. Preempción y Liberación (Objetivo 4)
    public void solicitarPreempcion(Job K) { // K es el trabajo de alta prioridad que quiere entrar
        // Objetivo 4: Marcar el objetivo y esperar a que llegue a la barrera.
        synchronized (cvBarrera) {
            // Busca un trabajo RUNNING/COMM (menor prioridad) para marcar.
            // Para simplificar, buscamos el primer trabajo no-WAIT en la cola.
            Job target = null;
            if (!colaNormal.isEmpty()) {
                target = colaNormal.peek();
            } else if (!colaAlta.isEmpty()) {
                // Preemptar solo si la nueva alta prioridad es mayor que la antigua
                // (Lógica omitida, solo marcamos si encontramos un objetivo simple)
            }
            
            if (target != null && target.estado != Job.Estado.WAIT) {
                preemptTargets.add(target);
                System.out.printf("[%s] Marcado para Preempción por %s.\n", target.id, K.id);
            }
        }
    }
    
>>>>>>> 23e934d68df5d3d253bd9fa0253db12338618c8b
    public void liberarRecursos(Job J) {
        if (J.islaAsignada == -1) return; 
        
        synchronized (cvAsignacion) {
<<<<<<< HEAD
=======
            // Objetivo: liberar la asignación gang y los tokens.
>>>>>>> 23e934d68df5d3d253bd9fa0253db12338618c8b
            gpuLibres.put(J.islaAsignada, gpuLibres.get(J.islaAsignada) + J.g);
            tokensIsla.put(J.islaAsignada, tokensIsla.get(J.islaAsignada) + J.b);
            tokensGlobal += J.b;
            
<<<<<<< HEAD
=======
            // Si el trabajo estaba esperando en cola, removerlo.
>>>>>>> 23e934d68df5d3d253bd9fa0253db12338618c8b
            colaAlta.remove(J);
            colaNormal.remove(J);

            J.islaAsignada = -1;
            
<<<<<<< HEAD
=======
            // Despertar a trabajos esperando por Gang (Objetivo 1 y 5)
>>>>>>> 23e934d68df5d3d253bd9fa0253db12338618c8b
            cvAsignacion.notifyAll();
        }
        
        synchronized (cvTokens) {
            cvTokens.notifyAll(); 
        }
    }

<<<<<<< HEAD
    // Getters para la GUI
=======
    // Getters para la GUI (métricas)
>>>>>>> 23e934d68df5d3d253bd9fa0253db12338618c8b
    public Map<Integer, Integer> getGpuLibres() { return gpuLibres; }
    public int getTokensGlobal() { return tokensGlobal; }
    public Map<Integer, Integer> getTokensIsla() { return tokensIsla; }
    public int getVentanasLibres() { return ventanasLibres; }
    public Queue<Job> getColaAlta() { return colaAlta; }
    public Queue<Job> getColaNormal() { return colaNormal; }
}