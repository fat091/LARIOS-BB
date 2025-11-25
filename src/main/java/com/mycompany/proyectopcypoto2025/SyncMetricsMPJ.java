package com.mycompany.proyectopcypoto2025;

import mpi.MPI;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicInteger;

public class SyncMetricsMPJ {

    private static final int NUM_METODOS = 5;
    private static final int N = 200; // iteraciones aumentadas para mejor visualización
    private static final int OPERACIONES_POR_ITER = 10;

    // Contadores para métricas en tiempo real
    private static final AtomicInteger[] exitosasPorCore = new AtomicInteger[NUM_METODOS];
    private static final AtomicInteger[] conflictosPorCore = new AtomicInteger[NUM_METODOS];
    
    static {
        for (int i = 0; i < NUM_METODOS; i++) {
            exitosasPorCore[i] = new AtomicInteger(0);
            conflictosPorCore[i] = new AtomicInteger(0);
        }
    }

    public static void main(String[] args) throws Exception {
        MPI.Init(args);

        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        if (size < NUM_METODOS) {
            if (rank == 0) {
                System.err.println(
                        "ERROR: Ejecuta con: mpjrun -np 5 -cp target/classes com.mycompany.proyectopcypoto2025.SyncMetricsMPJ");
            }
            MPI.Finalize();
            return;
        }

        System.out.println("[Rank " + rank + "] iniciado - " + getNombreMetodo(rank));

        // ---------- 1. Cada proceso ejecuta su mecanismo de sincronización ----------
        double[] metricasTiempo = new double[N];
        int[] exitosas = new int[N];
        int[] conflictos = new int[N];

        // Simulación del mecanismo de sincronización asignado
        for (int iter = 0; iter < N; iter++) {
            long startTime = System.nanoTime();
            
            int[] resultados = simularOperacionesSincronizacion(rank, iter);
            exitosas[iter] = resultados[0];
            conflictos[iter] = resultados[1];
            
            long endTime = System.nanoTime();
            metricasTiempo[iter] = (endTime - startTime) / 1_000_000.0; // ms

            // Actualizar contadores globales para métricas en tiempo real
            exitosasPorCore[rank].addAndGet(exitosas[iter]);
            conflictosPorCore[rank].addAndGet(conflictos[iter]);
            
            // Pequeña pausa entre iteraciones
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // ---------- 2. Recopilación de datos en Rank 0 ----------
        if (rank == 0) {
            // Recibir datos de todos los cores
            double[][] todasMetricasTiempo = new double[NUM_METODOS][N];
            int[][] todasExitosas = new int[NUM_METODOS][N];
            int[][] todasConflictos = new int[NUM_METODOS][N];
            
            todasMetricasTiempo[0] = metricasTiempo;
            todasExitosas[0] = exitosas;
            todasConflictos[0] = conflictos;

            // Recibir de los otros cores
            for (int r = 1; r < NUM_METODOS; r++) {
                double[] bufferTiempo = new double[N];
                int[] bufferExitosas = new int[N];
                int[] bufferConflictos = new int[N];
                
                MPI.COMM_WORLD.Recv(bufferTiempo, 0, N, MPI.DOUBLE, r, 100);
                MPI.COMM_WORLD.Recv(bufferExitosas, 0, N, MPI.INT, r, 101);
                MPI.COMM_WORLD.Recv(bufferConflictos, 0, N, MPI.INT, r, 102);
                
                todasMetricasTiempo[r] = bufferTiempo;
                todasExitosas[r] = bufferExitosas;
                todasConflictos[r] = bufferConflictos;
                
                System.out.println("[Rank 0] Recibió datos del core " + r + " - " + getNombreMetodo(r));
            }

            // Generar archivos CSV
            escribirCSVTiempos(todasMetricasTiempo);
            escribirCSVOperaciones(todasExitosas, todasConflictos);
            generarReporteFinal(todasExitosas, todasConflictos);

        } else {
            // Enviar datos al rank 0
            MPI.COMM_WORLD.Send(metricasTiempo, 0, N, MPI.DOUBLE, 0, 100);
            MPI.COMM_WORLD.Send(exitosas, 0, N, MPI.INT, 0, 101);
            MPI.COMM_WORLD.Send(conflictos, 0, N, MPI.INT, 0, 102);
            System.out.println("[Rank " + rank + "] datos enviados.");
        }

        MPI.Finalize();
    }

    // ---------- Simulaciones Específicas por Core ----------

    private static int[] simularOperacionesSincronizacion(int coreId, int iteracion) {
        int exitosas = 0;
        int conflictos = 0;
        
        for (int op = 0; op < OPERACIONES_POR_ITER; op++) {
            boolean exito = switch (coreId) {
                case 0 -> simularSemáforos(iteracion, op);
                case 1 -> simularVariablesCondicion(iteracion, op);
                case 2 -> simularMonitores(iteracion, op);
                case 3 -> simularMutex(iteracion, op);
                case 4 -> simularBarreras(iteracion, op);
                default -> true;
            };
            
            if (exito) {
                exitosas++;
            } else {
                conflictos++;
            }
        }
        
        return new int[]{exitosas, conflictos};
    }

    private static boolean simularSemáforos(int iter, int op) {
        try {
            // Simulación de adquisición y liberación de semáforos
            double probabilidadExito = 0.85 - (iter % 20) * 0.01; // Variación dinámica
            Thread.sleep(2 + (iter % 3));
            return Math.random() < probabilidadExito;
        } catch (InterruptedException e) {
            return false;
        }
    }

    private static boolean simularVariablesCondicion(int iter, int op) {
        try {
            // Simulación más compleja con notificaciones
            double probabilidadExito = 0.80 - (iter % 25) * 0.008;
            Thread.sleep(3 + (iter % 4));
            
            // Simular condiciones de carrera ocasionales
            if (iter % 15 == 0 && op % 3 == 0) {
                probabilidadExito *= 0.7;
            }
            
            return Math.random() < probabilidadExito;
        } catch (InterruptedException e) {
            return false;
        }
    }

    private static boolean simularMonitores(int iter, int op) {
        try {
            // Monitores suelen ser más eficientes
            double probabilidadExito = 0.90 - (iter % 30) * 0.005;
            Thread.sleep(1 + (iter % 2));
            return Math.random() < probabilidadExito;
        } catch (InterruptedException e) {
            return false;
        }
    }

    private static boolean simularMutex(int iter, int op) {
        try {
            // Mutex puede tener más contención
            double probabilidadExito = 0.75 - (iter % 15) * 0.012;
            Thread.sleep(4 + (iter % 5));
            
            // Simular bloqueos ocasionales
            if (iter % 10 == 0) {
                probabilidadExito *= 0.6;
            }
            
            return Math.random() < probabilidadExito;
        } catch (InterruptedException e) {
            return false;
        }
    }

    private static boolean simularBarreras(int iter, int op) {
        try {
            // Barreras para sincronización grupal
            double probabilidadExito = 0.88 - (iter % 18) * 0.009;
            Thread.sleep(2 + (iter % 3));
            
            // Simular espera en barrera
            if (op % 4 == 0) {
                Thread.sleep(5);
            }
            
            return Math.random() < probabilidadExito;
        } catch (InterruptedException e) {
            return false;
        }
    }

    // ---------- Utilidades ----------

    private static String getNombreMetodo(int coreId) {
        return switch (coreId) {
            case 0 -> "SEMÁFOROS";
            case 1 -> "VARIABLES_CONDICION";
            case 2 -> "MONITORES";
            case 3 -> "MUTEX";
            case 4 -> "BARRERAS";
            default -> "DESCONOCIDO";
        };
    }

    private static void escribirCSVTiempos(double[][] matriz) {
        String dir = System.getProperty("user.dir");
        File archivo = new File(dir, "mpj_tiempos.csv");

        System.out.println("Escribiendo métricas de tiempo en: " + archivo.getAbsolutePath());

        try (PrintWriter pw = new PrintWriter(new FileWriter(archivo))) {
            pw.println("iter,semaforos,var_condicion,monitores,mutex,barreras");

            for (int i = 0; i < N; i++) {
                pw.printf("%d,%.5f,%.5f,%.5f,%.5f,%.5f%n",
                        i, matriz[0][i], matriz[1][i], matriz[2][i], matriz[3][i], matriz[4][i]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void escribirCSVOperaciones(int[][] exitosas, int[][] conflictos) {
        String dir = System.getProperty("user.dir");
        File archivo = new File(dir, "mpj_operaciones.csv");

        try (PrintWriter pw = new PrintWriter(new FileWriter(archivo))) {
            pw.println("iter,core,metodo,exitosas,conflictos,eficiencia");

            for (int i = 0; i < N; i++) {
                for (int core = 0; core < NUM_METODOS; core++) {
                    int exit = exitosas[core][i];
                    int conf = conflictos[core][i];
                    double eficiencia = (exit + conf) > 0 ? (100.0 * exit / (exit + conf)) : 0.0;
                    
                    pw.printf("%d,%d,%s,%d,%d,%.2f%n",
                            i, core, getNombreMetodo(core), exit, conf, eficiencia);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void generarReporteFinal(int[][] exitosas, int[][] conflictos) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("REPORTE FINAL - COMPARACIÓN MECANISMOS DE SINCRONIZACIÓN");
        System.out.println("=".repeat(80));
        
        for (int core = 0; core < NUM_METODOS; core++) {
            int totalExitosas = 0;
            int totalConflictos = 0;
            
            for (int i = 0; i < N; i++) {
                totalExitosas += exitosas[core][i];
                totalConflictos += conflictos[core][i];
            }
            
            double eficiencia = (totalExitosas + totalConflictos) > 0 ? 
                (100.0 * totalExitosas / (totalExitosas + totalConflictos)) : 0.0;
            
            System.out.printf("Core %d (%s): %d exitosas, %d conflictos, Eficiencia: %.2f%%%n",
                    core, getNombreMetodo(core), totalExitosas, totalConflictos, eficiencia);
        }
        System.out.println("=".repeat(80));
    }

    // Método para obtener métricas en tiempo real (puede ser usado por la GUI)
    public static int[] getMetricasTiempoReal(int coreId) {
        if (coreId >= 0 && coreId < NUM_METODOS) {
            return new int[]{
                exitosasPorCore[coreId].get(),
                conflictosPorCore[coreId].get()
            };
        }
        return new int[]{0, 0};
    }
}