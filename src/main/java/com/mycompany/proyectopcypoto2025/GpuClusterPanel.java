package com.mycompany.proyectopcypoto2025;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
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
    
    // Animación
    private float[] particleOffsets = new float[50];

    public GpuClusterPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(20, 20, 40));
        
        // Inicializar partículas
        for (int i = 0; i < particleOffsets.length; i++) {
            particleOffsets[i] = (float) (Math.random() * Math.PI * 2);
        }
        
        planificador = new PlanificadorGPU(NUM_ISLAS, GPUS_POR_ISLA, TOKENS_GLOBAL, TOKENS_ISLA, VENTANAS_K);
        
        lanzarTrabajosDemo();
        
        animTimer = new Timer(50, e -> {
            frameCount++;
            // Actualizar partículas
            for (int i = 0; i < particleOffsets.length; i++) {
                particleOffsets[i] += 0.05f;
            }
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
        
        // Fondo con gradiente
        GradientPaint bgGrad = new GradientPaint(0, 0, new Color(20, 20, 40),
                0, H, new Color(40, 20, 60));
        g2.setPaint(bgGrad);
        g2.fillRect(0, 0, W, H);
        
        // Dibujar partículas de fondo
        dibujarParticulasFondo(g2, W, H);
        
        int islandHeight = (H - 120) / NUM_ISLAS;
        
        // Dibujar Islas GPU con mejores efectos
        for (int i = 0; i < NUM_ISLAS; i++) {
            int y = 60 + i * islandHeight;
            dibujarIsla(g2, i, 40, y, W - 280, islandHeight - 20);
        }
        
        // Panel lateral para trabajos en cola
        dibujarPanelColas(g2, W - 230, 60, 220, H - 140);
        
        // Dibujar Estado Global en la parte inferior
        dibujarEstadoGlobal(g2, W / 2, H - 30);

        // Info del modo en la esquina
        dibujarInfoModo(g2, W, H);
        
        g2.dispose();
    }
    
    private void dibujarParticulasFondo(Graphics2D g2, int W, int H) {
        g2.setColor(new Color(100, 100, 255, 20));
        for (int i = 0; i < particleOffsets.length; i++) {
            float offset = particleOffsets[i];
            int x = (int) ((Math.sin(offset) * 0.5 + 0.5) * W);
            int y = (int) ((Math.cos(offset * 0.7) * 0.5 + 0.5) * H);
            int size = 2 + (i % 3);
            g2.fillOval(x, y, size, size);
        }
    }
    
    private void dibujarIsla(Graphics2D g2, int id, int x, int y, int w, int h) {
        // Sombra
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillRoundRect(x + 4, y + 4, w, h, 20, 20);
        
        // Fondo de isla con gradiente
        GradientPaint islandGrad = new GradientPaint(x, y, new Color(45, 45, 90),
                x, y + h, new Color(30, 30, 70));
        g2.setPaint(islandGrad);
        g2.fillRoundRect(x, y, w, h, 20, 20);
        
        // Borde brillante
        g2.setColor(new Color(120, 120, 255, 180));
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawRoundRect(x, y, w, h, 20, 20);
        
        // Título de la isla
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 18f));
        g2.setColor(new Color(200, 200, 255));
        g2.drawString("ISLA GPU " + id, x + 15, y + 28);
        
        // Estadísticas
        int gpusLibres = planificador.getGpuLibres().getOrDefault(id, 0);
        int tokensLibres = planificador.getTokensIsla().getOrDefault(id, 0);
        
        // Barra de GPUs
        dibujarBarraRecurso(g2, x + 15, y + 40, w - 30, 18, 
                            gpusLibres, GPUS_POR_ISLA, "GPUs", new Color(0, 255, 150));
        
        // Barra de Tokens
        dibujarBarraRecurso(g2, x + 15, y + 65, w - 30, 18,
                           tokensLibres, TOKENS_ISLA, "Tokens", new Color(255, 200, 0));
        
        // Grid de GPUs
        int gx = x + 15;
        int gy = y + 95;
        int gpuGap = Math.min(50, (w - 30) / GPUS_POR_ISLA);
        
        for (int i = 0; i < GPUS_POR_ISLA; i++) {
            int currentGx = gx + i * gpuGap;
            boolean libre = i < gpusLibres;
            
            Color coreColor = libre ? new Color(0, 255, 100) : new Color(255, 50, 50);
            dibujarGPUMejorado(g2, currentGx, gy, coreColor, libre);
        }
        
        // Dibujar trabajos asignados a esta isla
        dibujarTrabajosEnIsla(g2, id, x, y, w, h);
    }
    
    private void dibujarBarraRecurso(Graphics2D g2, int x, int y, int w, int h,
                                     int valor, int maximo, String label, Color color) {
        // Fondo de barra
        g2.setColor(new Color(20, 20, 30));
        g2.fillRoundRect(x, y, w, h, 8, 8);
        
        // Progreso
        float ratio = (float) valor / maximo;
        int barW = (int) (w * ratio);
        
        GradientPaint barGrad = new GradientPaint(x, y, color.brighter(),
                x + barW, y, color);
        g2.setPaint(barGrad);
        g2.fillRoundRect(x, y, barW, h, 8, 8);
        
        // Borde
        g2.setColor(new Color(100, 100, 150));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(x, y, w, h, 8, 8);
        
        // Texto
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 11f));
        g2.setColor(Color.WHITE);
        String text = label + ": " + valor + "/" + maximo;
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, x + (w - fm.stringWidth(text)) / 2, y + h - 4);
    }
    
    private void dibujarGPUMejorado(Graphics2D g2, int x, int y, Color coreColor, boolean activo) {
        int w = 42, h = 50;
        
        // Sombra
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillRoundRect(x + 2, y + 2, w, h, 8, 8);
        
        // Cuerpo del GPU
        GradientPaint gpuGrad = new GradientPaint(x, y, new Color(60, 60, 80),
                x, y + h, new Color(40, 40, 60));
        g2.setPaint(gpuGrad);
        g2.fillRoundRect(x, y, w, h, 8, 8);
        
        // Core central animado
        int coreSize = 18;
        int coreX = x + (w - coreSize) / 2;
        int coreY = y + 8;
        
        if (activo) {
            // Pulso de luz
            float pulse = (float) (Math.sin(frameCount * 0.1) * 0.3 + 0.7);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, pulse * 0.4f));
            g2.setColor(coreColor);
            g2.fillOval(coreX - 4, coreY - 4, coreSize + 8, coreSize + 8);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }
        
        g2.setColor(coreColor);
        g2.fillOval(coreX, coreY, coreSize, coreSize);
        
        // Detalles del chip
        g2.setColor(new Color(80, 80, 100));
        g2.setStroke(new BasicStroke(1.5f));
        for (int i = 0; i < 3; i++) {
            int barY = y + 30 + i * 6;
            g2.drawLine(x + 8, barY, x + w - 8, barY);
        }
        
        // Borde
        g2.setColor(activo ? coreColor.darker() : new Color(80, 80, 80));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(x, y, w, h, 8, 8);
        
        // LED indicador
        g2.setColor(activo ? Color.GREEN : Color.RED);
        g2.fillOval(x + w - 8, y + 4, 4, 4);
    }
    
    private void dibujarTrabajosEnIsla(Graphics2D g2, int islaId, int ix, int iy, int iw, int ih) {
        List<Job> jobsEnIsla = new ArrayList<>();
        for (Job j : trabajos) {
            if (j.islaAsignada == islaId && j.estaEjecutando()) {
                jobsEnIsla.add(j);
            }
        }
        
        int jobX = ix + iw - 180;
        int jobY = iy + 95;
        
        for (int i = 0; i < jobsEnIsla.size() && i < 3; i++) {
            Job j = jobsEnIsla.get(i);
            dibujarTarjetaTrabajo(g2, jobX, jobY + i * 45, 160, 40, j);
        }
    }
    
    private void dibujarTarjetaTrabajo(Graphics2D g2, int x, int y, int w, int h, Job job) {
        // Fondo de tarjeta
        Color cardColor = switch (job.estado) {
            case RUN -> new Color(40, 100, 40);
            case BARRIER -> new Color(100, 100, 40);
            case COMM -> new Color(100, 40, 100);
            default -> new Color(60, 60, 60);
        };
        
        GradientPaint cardGrad = new GradientPaint(x, y, cardColor.brighter(),
                x, y + h, cardColor);
        g2.setPaint(cardGrad);
        g2.fillRoundRect(x, y, w, h, 12, 12);
        
        // Borde
        g2.setColor(cardColor.brighter());
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(x, y, w, h, 12, 12);
        
        // ID del trabajo
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14f));
        g2.setColor(Color.WHITE);
        g2.drawString(job.id, x + 8, y + 18);
        
        // Estado
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 10f));
        g2.setColor(new Color(200, 200, 200));
        String estadoStr = job.estado.name();
        g2.drawString(estadoStr, x + 8, y + 32);
        
        // Indicador de GPUs
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 10f));
        g2.drawString("G:" + job.g, x + w - 50, y + 18);
        g2.drawString("B:" + job.b, x + w - 50, y + 32);
        
        // Barra de progreso simulada
        if (job.estado == Job.Estado.RUN || job.estado == Job.Estado.COMM) {
            float progress = ((frameCount + job.id.hashCode()) % 100) / 100f;
            int barW = (int) ((w - 16) * progress);
            
            g2.setColor(new Color(0, 200, 255, 150));
            g2.fillRoundRect(x + 8, y + h - 8, barW, 4, 2, 2);
        }
    }
    
    private void dibujarPanelColas(Graphics2D g2, int x, int y, int w, int h) {
        // Fondo del panel
        g2.setColor(new Color(30, 30, 50, 200));
        g2.fillRoundRect(x, y, w, h, 15, 15);
        
        g2.setColor(new Color(100, 100, 200));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(x, y, w, h, 15, 15);
        
        // Título
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 16f));
        g2.setColor(new Color(200, 200, 255));
        g2.drawString("COLAS", x + 10, y + 25);
        
        // Cola de alta prioridad
        int qY = y + 40;
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 12f));
        g2.setColor(new Color(255, 200, 100));
        g2.drawString("Alta Prioridad:", x + 10, qY);
        
        qY += 20;
        int index = 0;
        for (Job j : planificador.getColaAlta()) {
            if (index >= 5) break;
            dibujarJobEnCola(g2, x + 10, qY, w - 20, j, true);
            qY += 35;
            index++;
        }
        
        // Cola normal
        qY += 10;
        g2.setColor(new Color(150, 200, 255));
        g2.drawString("Normal:", x + 10, qY);
        
        qY += 20;
        index = 0;
        for (Job j : planificador.getColaNormal()) {
            if (index >= 5) break;
            dibujarJobEnCola(g2, x + 10, qY, w - 20, j, false);
            qY += 35;
            index++;
        }
    }
    
    private void dibujarJobEnCola(Graphics2D g2, int x, int y, int w, Job job, boolean alta) {
        Color bgColor = alta ? new Color(80, 60, 40) : new Color(40, 60, 80);
        
        g2.setColor(bgColor);
        g2.fillRoundRect(x, y, w, 28, 8, 8);
        
        g2.setColor(alta ? new Color(255, 200, 100) : new Color(150, 200, 255));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(x, y, w, 28, 8, 8);
        
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 11f));
        g2.setColor(Color.WHITE);
        g2.drawString(job.id, x + 8, y + 12);
        
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 9f));
        g2.setColor(new Color(200, 200, 200));
        g2.drawString("GPUs:" + job.g + " Tok:" + job.b, x + 8, y + 24);
        
        // Icono de espera animado
        float angle = (frameCount * 0.1f) % 360;
        g2.setColor(new Color(255, 255, 100, 150));
        Arc2D arc = new Arc2D.Float(x + w - 20, y + 8, 12, 12, angle, 270, Arc2D.OPEN);
        g2.draw(arc);
    }
    
    private void dibujarEstadoGlobal(Graphics2D g2, int cx, int y) {
        int w = 500, h = 50;
        int x = cx - w / 2;
        
        // Fondo con sombra
        g2.setColor(new Color(0, 0, 0, 100));
        g2.fillRoundRect(x + 3, y - h / 2 + 3, w, h, 15, 15);
        
        GradientPaint bgGrad = new GradientPaint(x, y - h / 2, new Color(40, 40, 80),
                x, y + h / 2, new Color(20, 20, 40));
        g2.setPaint(bgGrad);
        g2.fillRoundRect(x, y - h / 2, w, h, 15, 15);
        
        g2.setColor(new Color(100, 150, 255));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(x, y - h / 2, w, h, 15, 15);
        
        // Tokens globales
        int B_USED = TOKENS_GLOBAL - planificador.getTokensGlobal();
        dibujarBarraRecurso(g2, x + 10, y - h / 2 + 8, 220, 16,
                           planificador.getTokensGlobal(), TOKENS_GLOBAL,
                           "Tokens Globales", new Color(0, 200, 255));
        
        // Ventanas K
        int K_USED = VENTANAS_K - planificador.getVentanasLibres();
        dibujarBarraRecurso(g2, x + 240, y - h / 2 + 8, 120, 16,
                           planificador.getVentanasLibres(), VENTANAS_K,
                           "Ventanas", new Color(255, 150, 0));
        
        // Estadísticas de colas
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 11f));
        g2.setColor(Color.WHITE);
        int colaAlta = planificador.getColaAlta().size();
        int colaNormal = planificador.getColaNormal().size();
        g2.drawString("Cola A:" + colaAlta, x + 370, y - h / 2 + 18);
        g2.drawString("Cola N:" + colaNormal, x + 420, y - h / 2 + 18);
    }
    
    private void dibujarInfoModo(Graphics2D g2, int W, int H) {
        String modoText = "MODO: " + currentMode.toString();
        
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 13f));
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(modoText);
        
        int x = W - tw - 20;
        int y = 25;
        
        // Fondo
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRoundRect(x - 8, y - 16, tw + 16, 24, 8, 8);
        
        // Texto
        g2.setColor(new Color(150, 255, 150));
        g2.drawString(modoText, x, y);
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