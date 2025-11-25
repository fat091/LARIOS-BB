package com.mycompany.proyectopcypoto2025;

import mpi.MPI;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class SyncMetricsMPJ {

    private static final int NUM_METODOS = 5;
    private static final int N = 50; // iteraciones

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

        System.out.println("[Rank " + rank + "] iniciado.");

        // ---------- 1. Cada proceso genera sus tiempos ----------
        double[] tiempos = new double[N];

        switch (rank) {
            case 0 -> llenar(tiempos, SimuladorSincronizacion::simularMutex);
            case 1 -> llenar(tiempos, SimuladorSincronizacion::simularSemaforos);
            case 2 -> llenar(tiempos, SimuladorSincronizacion::simularMonitores);
            case 3 -> llenar(tiempos, SimuladorSincronizacion::simularBarreras);
            case 4 -> llenar(tiempos, SimuladorSincronizacion::simularVariablesCondicion);
            default -> llenar(tiempos, () -> 0);
        }

        // ---------- 2. Enviar / Recibir manual (SIN Gather) ----------
        if (rank == 0) {
            // rank 0 recibe todo en matriz[metodo][iter]
            double[][] matriz = new double[NUM_METODOS][N];
            matriz[0] = tiempos; // ya tiene su arreglo local

            for (int r = 1; r < NUM_METODOS; r++) {
                double[] buffer = new double[N];
                MPI.COMM_WORLD.Recv(buffer, 0, N, MPI.DOUBLE, r, 99);
                matriz[r] = buffer;
                System.out.println("[Rank 0] Recibí datos del core " + r);
            }

            escribirCSV(matriz);

        } else if (rank < NUM_METODOS) {
            MPI.COMM_WORLD.Send(tiempos, 0, N, MPI.DOUBLE, 0, 99);
            System.out.println("[Rank " + rank + "] datos enviados.");
        }

        MPI.Finalize();
    }

    // --------- Helpers ---------

    @FunctionalInterface
    interface MetodoSim {
        double run();
    }

    private static void llenar(double[] arr, MetodoSim sim) {
        for (int i = 0; i < arr.length; i++) {
            arr[i] = sim.run();
        }
    }

    private static void escribirCSV(double[][] matriz) {

        // Carpeta del proyecto (donde estás parado al correr mpjrun)
        String dir = System.getProperty("user.dir");

        File f1 = new File(dir, "mpj_metrics.csv");

        System.out.println("Escribiendo métricas en: " + f1.getAbsolutePath());

        try (PrintWriter pw = new PrintWriter(new FileWriter(f1))) {

            pw.println("iter,mutex,semaforos,monitores,barreras,variables_condicion");

            for (int i = 0; i < N; i++) {
                pw.printf(
                        "%d,%.5f,%.5f,%.5f,%.5f,%.5f%n",
                        i,
                        matriz[0][i], matriz[1][i], matriz[2][i], matriz[3][i], matriz[4][i]
                );
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("CSV generado correctamente en: " + dir);
    }
}
