package com.mycompany.proyectopcypoto2025.utils;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.BrokenBarrierException;

public class SyncHelpers {
    private CyclicBarrier barrier;
    public void createBarrier(int parties, Runnable action) { barrier = new CyclicBarrier(parties, action); }
    public void awaitBarrier() {
        if (barrier == null) return;
        try { barrier.await(); } catch (InterruptedException | BrokenBarrierException e) { Thread.currentThread().interrupt(); }
    }
    public void clearBarrier() { barrier = null; }
}
