package com.mycompany.proyectopcypoto2025;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class GpuClusterPanel extends JPanel implements Reseteable, Demoable, SyncAware {
    
    private PlanificadorGPU planificador;
    private final List<Thread> replicaThreads = new ArrayList<>();
    private final List<Job> trabajos = new ArrayList<>();
    private volatile boolean running = false;
    private Timer animTimer;
    private int jobCounter = 0;
    private int frameCount = 0;
    
    private SyncMode currentMode = SyncMode.MONITORES;

    // Parámetros del clúster
    private final int NUM_ISLAS = 3;
    private final int GPUS_POR_ISLA = 8;
    private final int TOKENS_GLOBAL = 30;
    private final int TOKENS_ISLA = 12;
    private final int VENTANAS_K = 3;

    public GpuClusterPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(20, 20, 40));
        
        planificador = new PlanificadorGPU(NUM_ISLAS, GPUS_POR_ISLA, TOKENS_GLOBAL, TOKENS_ISLA, VENTANAS_K);
        
        lanzarTrabajosDemo();
        
        animTimer = new Timer(100, e -> {
            frameCount++;
            repaint();
        });
        animTimer.start();
    }
    
    private void lanzarTrabajosDemo() {
        if (running) return;
        running = true;
        
        for (int i = 0; i < 5; i++) {
            final int jobIndex = i;
            
            jobCounter++;
            int g = ThreadLocalRandom.current().nextInt(2, 6);
            int b = ThreadLocalRandom.current().nextInt(1, 4);
            boolean alta = jobIndex % 3 == 0;
            Job newJob = new Job(jobCounter, g, b, 1, alta);
            trabajos.add(newJob);
            
            for (int r = 0; r < g; r++) {
                final int replicaId = r;
                
                Thread t = new Thread(() -> replicaCiclo(newJob, replicaId), newJob.id + "-R" + replicaId);
                replicaThreads.add(t);
                t.start();
            }
        }
    }
    
    private void pausaCondicional(Job J) throws InterruptedException {
        switch (currentMode) {
            case SEMAFOROS:
                if (J.estado == Job.Estado.WAIT) {
                    dormir(1000);
                }
                break;
            case VAR_CONDICION:
                if (J.islaAsignada != -1 && J.estado == Job.Estado.RUN) {
                    if (ThreadLocalRandom.current().nextDouble() < 0.05) {
                        J.reportarFallo();
                        J.estado = Job.Estado.PREEMPTED;
                    }
                }
                break;
            case MUTEX:
                if (J.estado == Job.Estado.COMM) {
                    if (ThreadLocalRandom.current().nextDouble() < 0.2) {
                        throw new InterruptedException("Livelock simulado.");
                    }
                }
                break;
            case BARRERAS:
            case MONITORES:
            default:
                break;
        }
    }
    
    private void replicaCiclo(Job J, int r) {
        try {
            if (r == 0) {
                pausaCondicional(J);
                planificador.solicitarGang(J);
            }
            
            while (J.islaAsignada == -1 && J.estado == Job.Estado.WAIT) {
                pausaCondicional(J);
                dormir(20);
            }

            if (J.islaAsignada == -1 || J.estado == Job.Estado.PREEMPTED) return;
            
            planificador.entrarBarrera(J);

            while (J.estaEjecutando()) {
                pausaCondicional(J);
                dormir(ThreadLocalRandom.current().nextInt(400, 800));
                
                pausaCondicional(J);
                planificador.entrarBarrera(J);
                
                if (J.estado == Job.Estado.PREEMPTED) break;
                
                pausaCondicional(J);
                planificador.solicitarVentanaColectiva(J);
                dormir(ThreadLocalRandom.current().nextInt(200, 400));
                planificador.liberarVentanaColectiva(J);
            }
            
            if (r == 0 && J.estado != Job.Estado.FINISHED && J.estado != Job.Estado.PREEMPTED) {
                planificador.entrarBarrera(J);
            }

        } catch (InterruptedException e) {
            J.estado = Job.Estado.PREEMPTED;
        } finally {
            if (r == 0 && J.estado != Job.Estado.ABORTED) {
                planificador.liberarRecursos(J);
                J.estado = Job.Estado.FINISHED;
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int W = getWidth(), H = getHeight();
        int islandHeight = (H - 60) / NUM_ISLAS;
        
        // Dibujar Islas GPU
        for (int i = 0; i < NUM_ISLAS; i++) {
            int y = 30 + i * islandHeight;
            dibujarIsla(g2, i, 20, y, W - 40, islandHeight - 10);
        }
        
        // Dibujar Estado Global
        dibujarEstadoGlobal(g2, W / 2, H - 20);

        // Dibujar Trabajos
        dibujarTrabajos(g2, W, H);
        
        // Info del modo
        g2.setColor(Color.WHITE);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14f));
        g2.drawString("MODO: " + currentMode.toString(), W - 150, 30);
        
        g2.dispose();
    }
    
    private void dibujarIsla(Graphics2D g2, int id, int x, int y, int w, int h) {
        g2.setColor(new Color(30, 30, 80));
        g2.fillRoundRect(x, y, w, h, 15, 15);
        g2.setColor(new Color(100, 100, 255));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(x, y, w, h, 15, 15);
        
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 16f));
        g2.setColor(Color.WHITE);
        g2.drawString("ISLA GPU " + id, x + 10, y + 25);
        
        int gpusLibres = planificador.getGpuLibres().getOrDefault(id, 0);
        int tokensLibres = planificador.getTokensIsla().getOrDefault(id, 0);
        
        g2.setFont(g2.getFont().deriveFont(10f));
        g2.setColor(new Color(180, 180, 255));
        g2.drawString("GPUs: " + gpusLibres + "/" + GPUS_POR_ISLA, x + 10, y + h - 18);
        g2.drawString("Tokens: " + tokensLibres + "/" + TOKENS_ISLA, x + 100, y + h - 18);
        
        int gx = x + 10;
        int gy = y + 40;
        int gpuGap = 45;
        
        for (int i = 0; i < GPUS_POR_ISLA; i++) {
            int currentGx = gx + i * gpuGap;
            boolean libre = i < gpusLibres;
            
            Color coreColor = libre ? new Color(0, 255, 100) : new Color(255, 50, 50);
            dibujarGPU(g2, currentGx, gy, coreColor);
        }
    }
    
    private void dibujarGPU(Graphics2D g2, int x, int y, Color coreColor) {
        int w = 40, h = 18;
        
        g2.setColor(new Color(50, 50, 60));
        g2.fillRoundRect(x, y, w, h, 6, 6);
        
        g2.setColor(coreColor);
        int coreSize = 10;
        int coreX = x + (w - coreSize) / 2;
        int coreY = y + (h - coreSize) / 2;
        g2.fillOval(coreX, coreY, coreSize, coreSize);
        
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(x, y, w, h, 6, 6);
    }
    
    private void dibujarTrabajos(Graphics2D g2, int W, int H) {
        for (Job J : trabajos) {
            int isla = J.islaAsignada;
            int statusX, statusY;

            if (J.estado == Job.Estado.WAIT) {
                int colaOffset = J.prioridadAlta ? 0 : 100;
                int index = new ArrayList<>(J.prioridadAlta ? planificador.getColaAlta() : planificador.getColaNormal()).indexOf(J);
                statusX = 50 + colaOffset;
                statusY = H - 80 + index * 20;
            } else if (isla != -1) {
                int islandY = 30 + isla * ((H - 60) / NUM_ISLAS);
                statusX = W - 100;
                statusY = islandY + 50;
            } else {
                statusX = W - 50;
                statusY = H - 50;
            }
            
            Color jobCoreColor = switch (J.estado) {
                case WAIT -> Color.GRAY;
                case RUN -> Color.GREEN;
                case BARRIER -> Color.YELLOW;
                case COMM -> Color.MAGENTA;
                case PREEMPTED, ABORTED -> Color.RED;
                case FINISHED -> Color.DARK_GRAY;
            };
            
            dibujarGPU(g2, statusX, statusY, jobCoreColor);
            
            g2.setColor(Color.WHITE);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 10f));
            String info = J.id + " " + J.estado.name().substring(0, 3);
            g2.drawString(info, statusX + 45, statusY + 12);
        }
    }
    
    private void dibujarEstadoGlobal(Graphics2D g2, int cx, int y) {
        int B_USED = TOKENS_GLOBAL - planificador.getTokensGlobal();
        int K_USED = VENTANAS_K - planificador.getVentanasLibres();
        
        g2.setColor(new Color(10, 10, 10));
        g2.fillRect(cx - 160, y - 18, 320, 36);
        g2.setColor(new Color(0, 200, 255));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRect(cx - 160, y - 18, 320, 36);

        double tokenRatio = (double) B_USED / TOKENS_GLOBAL;
        int barW = (int) (300 * tokenRatio);
        
        g2.setColor(Color.BLUE.darker());
        g2.fillRect(cx - 150, y - 15, 300, 10);
        g2.setColor(new Color(0, 200, 255));
        g2.fillRect(cx - 150, y - 15, barW, 10);
        
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 9f));
        g2.setColor(Color.WHITE);
        g2.drawString("TOKENS GLOBALES: " + B_USED + "/" + TOKENS_GLOBAL, cx - 145, y - 6);

        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 12f));
        g2.setColor(Color.WHITE);
        String status = String.format("VENTANAS: %d/%d | COLA A:%d N:%d",
                K_USED, VENTANAS_K, planificador.getColaAlta().size(), planificador.getColaNormal().size());
        g2.drawString(status, cx - 145, y + 15);
    }

    @Override
    public void setSyncMode(SyncMode m) {
        this.currentMode = m;
        reset();
    }
    
    @Override
    public void reset() {
        running = false;
        replicaThreads.forEach(Thread::interrupt);
        replicaThreads.clear();
        trabajos.clear();
        
        planificador = new PlanificadorGPU(NUM_ISLAS, GPUS_POR_ISLA, TOKENS_GLOBAL, TOKENS_ISLA, VENTANAS_K);
        jobCounter = 0;
        lanzarTrabajosDemo();
        
        repaint();
    }
    
    @Override
    public void demo() {
    }
    
    private void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}