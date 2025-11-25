package com.mycompany.proyectopcypoto2025;

/**
 * Simulaciones para cada método de sincronización.
 * Cada método devuelve un "tiempo" en ms.
 * A propósito hacemos trabajo pesado para que los tiempos
 * estén en un rango visible (decenas de ms).
 */
public class SimuladorSincronizacion {

    private static double trabajoBase(int repeticiones) {
        long ini = System.nanoTime();
        long x = 1;
        for (int i = 0; i < repeticiones; i++) {
            // bucle tonto solo para consumir CPU
            x = x * 1103515245L + 12345L;
        }
        long fin = System.nanoTime();
        return (fin - ini) / 1_000_000.0; // ms
    }

    // Ajusta las repeticiones para que cada método dé un rango distinto
    public static double simularMutex() {
        return trabajoBase(3_000_000);   // ~30-60 ms (depende de tu máquina)
    }

    public static double simularSemaforos() {
        return trabajoBase(3_500_000);   // un poco más pesado
    }

    public static double simularMonitores() {
        return trabajoBase(2_800_000);   // un poco más ligero
    }

    public static double simularBarreras() {
        return trabajoBase(3_200_000);
    }

    public static double simularVariablesCondicion() {
        return trabajoBase(3_800_000);   // el más pesado
    }
}
