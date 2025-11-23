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

    private final int NUM_ISLAS = 3;
    private final int GPUS_POR_ISLA = 8;
    private final int TOKENS_GLOBAL = 30;
    private final int TOKENS_ISLA = 12;
    private final int VENTANAS_K = 3;
    
    private float[] particleOffsets = new float[50];

    public GpuClusterPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(20, 20, 40));
        
        for (int i = 0; i < particleOffsets.length; i++) {
            particleOffsets[i] = (float) (Math.random() * Math.PI * 2);
        }
        
        planificador = new PlanificadorGPU(NUM_ISLAS, GPUS_POR_ISLA, TOKENS_GLOBAL, TOKENS_ISLA, VENTANAS_K);
        
        lanzarTrabajosDemo();
        
        animTimer = new Timer(50, e -> {
            frameCount++;
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
        
        GradientPaint bgGrad = new GradientPaint(0, 0, new Color(15, 15, 30),
                0, H, new Color(25, 15, 35));
        g2.setPaint(bgGrad);
        g2.fillRect(0, 0, W, H);
        
        dibujarParticulasFondo(g2, W, H);
        
        int leftWidth = (int)(W * 0.60);
        int rightWidth = W - leftWidth;
        
        int islandHeight = (H - 100) / NUM_ISLAS;
        
        for (int i = 0; i < NUM_ISLAS; i++) {
            int y = 50 + i * islandHeight;
            dibujarIsla(g2, i, 20, y, leftWidth - 40, islandHeight - 15);
        }
        
        dibujarPanelColas(g2, leftWidth + 10, 50, rightWidth - 30, H - 100);
        
        dibujarEstadoGlobal(g2, leftWidth / 2, H - 25);

        dibujarInfoModo(g2, W, 20);
        
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
        g2.setColor(new Color(0, 0, 0, 100));
        g2.fillRoundRect(x + 5, y + 5, w, h, 15, 15);
        
        GradientPaint islandGrad = new GradientPaint(x, y, new Color(35, 35, 55),
                x, y + h, new Color(20, 20, 35));
        g2.setPaint(islandGrad);
        g2.fillRoundRect(x, y, w, h, 15, 15);
        
        g2.setColor(new Color(0, 200, 255, 200));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(x, y, w, h, 15, 15);
        
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 16f));
        g2.setColor(new Color(0, 255, 200));
        g2.drawString("ISLA GPU " + id, x + 12, y + 22);
        
        int gpusLibres = planificador.getGpuLibres().getOrDefault(id, 0);
        int gpusUsados = GPUS_POR_ISLA - gpusLibres;
        int tokensLibres = planificador.getTokensIsla().getOrDefault(id, 0);
        
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 11f));
        g2.setColor(new Color(180, 180, 200));
        g2.drawString("GPUs: " + gpusUsados + "/" + GPUS_POR_ISLA, x + 12, y + 38);
        
        int barY = y + 45;
        dibujarBarraRecurso(g2, x + 12, barY, w - 24, 14,
                           tokensLibres, TOKENS_ISLA, "Tokens", new Color(255, 200, 0));
        
        int gx = x + 12;
        int gy = y + 68;
        int availableWidth = w - 24;
        int gpuWidth = 48;
        int gpuGap = Math.max(4, (availableWidth - (GPUS_POR_ISLA * gpuWidth)) / (GPUS_POR_ISLA - 1));
        
        for (int i = 0; i < GPUS_POR_ISLA; i++) {
            int currentGx = gx + i * (gpuWidth + gpuGap);
            boolean libre = i < gpusLibres;
            
            Color coreColor = libre ? new Color(0, 255, 100) : new Color(255, 50, 50);
            dibujarGPUMejorado(g2, currentGx, gy, coreColor, libre, gpuWidth);
        }
        
        dibujarTrabajosEnIsla(g2, id, x, gy + 65, w, h - (gy + 65 - y));
    }
    
    private void dibujarBarraRecurso(Graphics2D g2, int x, int y, int w, int h,
                                     int valor, int maximo, String label, Color color) {
        g2.setColor(new Color(20, 20, 30));
        g2.fillRoundRect(x, y, w, h, 8, 8);
        
        float ratio = (float) valor / maximo;
        int barW = Math.max(0, Math.min(w, (int) (w * ratio)));
        
        if (barW > 0) {
            GradientPaint barGrad = new GradientPaint(x, y, color.brighter(),
                    x + barW, y, color);
            g2.setPaint(barGrad);
            g2.fillRoundRect(x, y, barW, h, 8, 8);
        }
        
        g2.setColor(new Color(100, 100, 150));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(x, y, w, h, 8, 8);
        
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 10f));
        g2.setColor(Color.WHITE);
        String text = label + ": " + valor + "/" + maximo;
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        
        int textX = x + Math.max(2, (w - textWidth) / 2);
        int textY = y + (h + fm.getAscent()) / 2 - 2;
        
        Shape oldClip = g2.getClip();
        g2.setClip(x, y, w, h);
        g2.drawString(text, textX, textY);
        g2.setClip(oldClip);
    }
    
    private void dibujarGPUMejorado(Graphics2D g2, int x, int y, Color coreColor, boolean activo, int w) {
        int h = 55;
        
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillRoundRect(x + 2, y + 2, w, h, 8, 8);
        
        GradientPaint gpuGrad = new GradientPaint(x, y, new Color(50, 50, 65),
                x, y + h, new Color(30, 30, 45));
        g2.setPaint(gpuGrad);
        g2.fillRoundRect(x, y, w, h, 8, 8);
        
        g2.setColor(new Color(20, 20, 30));
        for (int i = 0; i < 3; i++) {
            g2.fillRect(x + 4, y + 4 + i * 3, w - 8, 2);
        }
        
        int coreSize = 20;
        int coreX = x + (w - coreSize) / 2;
        int coreY = y + 15;
        
        if (activo) {
            float pulse = (float) (Math.sin(frameCount * 0.15) * 0.4 + 0.6);
            
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, pulse * 0.3f));
            g2.setColor(coreColor);
            g2.fillOval(coreX - 6, coreY - 6, coreSize + 12, coreSize + 12);
            
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, pulse * 0.5f));
            g2.fillOval(coreX - 3, coreY - 3, coreSize + 6, coreSize + 6);
            
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }
        
        RadialGradientPaint coreGrad = new RadialGradientPaint(
            coreX + coreSize/2f, coreY + coreSize/2f, coreSize/2f,
            new float[]{0.0f, 0.7f, 1.0f},
            new Color[]{coreColor.brighter(), coreColor, coreColor.darker()}
        );
        g2.setPaint(coreGrad);
        g2.fillOval(coreX, coreY, coreSize, coreSize);
        
        g2.setColor(coreColor.darker());
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(coreX, coreY, coreSize, coreSize);
        
        g2.setColor(new Color(60, 60, 80));
        g2.setStroke(new BasicStroke(1.2f));
        for (int i = 0; i < 3; i++) {
            int lineY = y + 40 + i * 4;
            g2.drawLine(x + 6, lineY, x + w - 6, lineY);
        }
        
        g2.setColor(activo ? coreColor.darker() : new Color(60, 60, 70));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(x, y, w, h, 8, 8);
        
        int ledSize = 5;
        g2.setColor(activo ? new Color(0, 255, 100) : new Color(255, 50, 50));
        g2.fillOval(x + w - ledSize - 4, y + 4, ledSize, ledSize);
        
        if (activo) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            g2.fillOval(x + w - ledSize - 5, y + 3, ledSize + 2, ledSize + 2);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }
    }
    
    private void dibujarTrabajosEnIsla(Graphics2D g2, int islaId, int ix, int iy, int iw, int ih) {
        List<Job> jobsEnIsla = new ArrayList<>();
        for (Job j : trabajos) {
            if (j.islaAsignada == islaId && j.estaEjecutando()) {
                jobsEnIsla.add(j);
            }
        }
        
        if (jobsEnIsla.isEmpty()) return;
        
        int jobX = ix + 10;
        int jobY = iy;
        int maxJobsToShow = Math.min(3, jobsEnIsla.size());
        int jobSpacing = 3;
        
        for (int i = 0; i < maxJobsToShow; i++) {
            Job j = jobsEnIsla.get(i);
            dibujarTarjetaTrabajo(g2, jobX, jobY + i * (28 + jobSpacing), iw - 20, 25, j);
        }
        
        if (jobsEnIsla.size() > maxJobsToShow) {
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 10f));
            g2.setColor(new Color(150, 150, 200));
            g2.drawString("+" + (jobsEnIsla.size() - maxJobsToShow) + " más...", 
                         jobX + 5, jobY + maxJobsToShow * (28 + jobSpacing) + 15);
        }
    }
    
    private void dibujarTarjetaTrabajo(Graphics2D g2, int x, int y, int w, int h, Job job) {
        Color cardColor = switch (job.estado) {
            case RUN -> new Color(0, 120, 80);
            case BARRIER -> new Color(180, 140, 0);
            case COMM -> new Color(140, 0, 140);
            default -> new Color(60, 60, 60);
        };
        
        g2.setColor(new Color(0, 0, 0, 100));
        g2.fillRoundRect(x + 2, y + 2, w, h, 8, 8);
        
        g2.setColor(cardColor.darker());
        g2.fillRoundRect(x, y, w, h, 8, 8);
        
        g2.setColor(cardColor.brighter());
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(x, y, w, h, 8, 8);
        
        Shape oldClip = g2.getClip();
        g2.setClip(x, y, w, h);
        
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 11f));
        g2.setColor(Color.WHITE);
        g2.drawString(job.id, x + 6, y + 13);
        
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 9f));
        g2.setColor(new Color(200, 200, 200));
        String estadoAbr = switch(job.estado) {
            case RUN -> "RUN";
            case BARRIER -> "BAR";
            case COMM -> "COM";
            default -> "???";
        };
        g2.drawString(estadoAbr, x + 6, y + h - 5);
        
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 9f));
        g2.setColor(new Color(150, 255, 150));
        g2.drawString("G:" + job.g, x + w - 45, y + 13);
        g2.setColor(new Color(255, 200, 100));
        g2.drawString("B:" + job.b, x + w - 45, y + h - 5);
        
        if (job.estado == Job.Estado.RUN || job.estado == Job.Estado.COMM) {
            float progress = ((frameCount * 2 + job.id.hashCode()) % 200) / 200f;
            int maxBarW = w - 55;
            int barW = Math.max(0, Math.min(maxBarW, (int) (maxBarW * progress)));
            
            g2.setColor(new Color(0, 255, 200, 180));
            g2.fillRoundRect(x + 40, y + h - 8, barW, 3, 2, 2);
        }
        
        g2.setClip(oldClip);
    }
    
    private void dibujarPanelColas(Graphics2D g2, int x, int y, int w, int h) {
        g2.setColor(new Color(25, 25, 40, 220));
        g2.fillRoundRect(x, y, w, h, 12, 12);
        
        g2.setColor(new Color(0, 150, 255));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(x, y, w, h, 12, 12);
        
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 15f));
        g2.setColor(new Color(0, 200, 255));
        g2.drawString("COLAS DE TRABAJOS", x + 10, y + 22);
        
        g2.setColor(new Color(100, 100, 150, 100));
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(x + 10, y + 28, x + w - 10, y + 28);
        
        int qY = y + 40;
        
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 13f));
        g2.setColor(new Color(255, 180, 0));
        g2.drawString("⚡ Alta Prioridad", x + 10, qY);
        
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 10f));
        g2.setColor(new Color(200, 160, 100));
        int colaAltaSize = planificador.getColaAlta().size();
        g2.drawString("(" + colaAltaSize + " en espera)", x + 130, qY);
        
        qY += 18;
        int index = 0;
        for (Job j : planificador.getColaAlta()) {
            if (index >= 6) {
                g2.setFont(g2.getFont().deriveFont(Font.ITALIC, 9f));
                g2.setColor(new Color(150, 150, 180));
                g2.drawString("+" + (colaAltaSize - 6) + " más...", x + 15, qY + 5);
                break;
            }
            dibujarJobEnCola(g2, x + 10, qY, w - 20, j, true);
            qY += 32;
            index++;
        }
        
        qY += 8;
        g2.setColor(new Color(100, 100, 150, 100));
        g2.drawLine(x + 10, qY, x + w - 10, qY);
        qY += 18;
        
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 13f));
        g2.setColor(new Color(100, 200, 255));
        g2.drawString("⏱ Normal", x + 10, qY);
        
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 10f));
        g2.setColor(new Color(120, 180, 220));
        int colaNormalSize = planificador.getColaNormal().size();
        g2.drawString("(" + colaNormalSize + " en espera)", x + 100, qY);
        
        qY += 18;
        index = 0;
        for (Job j : planificador.getColaNormal()) {
            if (index >= 6) {
                g2.setFont(g2.getFont().deriveFont(Font.ITALIC, 9f));
                g2.setColor(new Color(150, 150, 180));
                g2.drawString("+" + (colaNormalSize - 6) + " más...", x + 15, qY + 5);
                break;
            }
            dibujarJobEnCola(g2, x + 10, qY, w - 20, j, false);
            qY += 32;
            index++;
        }
    }
    
    private void dibujarJobEnCola(Graphics2D g2, int x, int y, int w, Job job, boolean alta) {
        Color bgColor = alta ? new Color(60, 50, 30) : new Color(30, 50, 60);
        Color borderColor = alta ? new Color(255, 180, 0) : new Color(100, 180, 255);
        
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillRoundRect(x + 2, y + 2, w, 26, 6, 6);
        
        g2.setColor(bgColor);
        g2.fillRoundRect(x, y, w, 26, 6, 6);
        
        float dashPhase = (frameCount * 0.5f) % 10;
        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 
                                     10.0f, new float[]{5, 5}, dashPhase));
        g2.drawRoundRect(x, y, w, 26, 6, 6);
        
        Shape oldClip = g2.getClip();
        g2.setClip(x, y, w, 26);
        
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 11f));
        g2.setColor(Color.WHITE);
        g2.drawString(job.id, x + 8, y + 14);
        
        float angle = (frameCount * 0.15f) % 360;
        g2.setColor(new Color(255, 255, 100, 180));
        g2.setStroke(new BasicStroke(1.5f));
        int clockX = x + w - 30;
        int clockY = y + 6;
        Arc2D arc = new Arc2D.Float(clockX, clockY, 14, 14, angle, 300, Arc2D.OPEN);
        g2.draw(arc);
        
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 9f));
        g2.setColor(new Color(180, 180, 200));
        String recursos = "G:" + job.g + " T:" + job.b;
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(recursos);
        int textX = x + (w - textWidth - 40) / 2 + 25;
        g2.drawString(recursos, textX, y + 14);
        
        if (alta) {
            g2.setColor(new Color(255, 200, 0));
            int[] xPoints = {x + w - 48, x + w - 52, x + w - 44};
            int[] yPoints = {y + 8, y + 16, y + 16};
            g2.fillPolygon(xPoints, yPoints, 3);
        }
        
        g2.setClip(oldClip);
    }
    
    private void dibujarEstadoGlobal(Graphics2D g2, int cx, int y) {
        int w = Math.min(500, getWidth() - 40);
        int h = 50;
        int x = cx - w / 2;
        
        g2.setColor(new Color(0, 0, 0, 100));
        g2.fillRoundRect(x + 3, y - h / 2 + 3, w, h, 15, 15);
        
        GradientPaint bgGrad = new GradientPaint(x, y - h / 2, new Color(40, 40, 80),
                x, y + h / 2, new Color(20, 20, 40));
        g2.setPaint(bgGrad);
        g2.fillRoundRect(x, y - h / 2, w, h, 15, 15);
        
        g2.setColor(new Color(100, 150, 255));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(x, y - h / 2, w, h, 15, 15);
        
        Shape oldClip = g2.getClip();
        g2.setClip(x, y - h / 2, w, h);
        
        int barWidth = Math.min(220, (w - 30) / 2);
        
        int B_USED = TOKENS_GLOBAL - planificador.getTokensGlobal();
        dibujarBarraRecurso(g2, x + 10, y - h / 2 + 8, barWidth, 16,
                           planificador.getTokensGlobal(), TOKENS_GLOBAL,
                           "Tokens Globales", new Color(0, 200, 255));
        
        int K_USED = VENTANAS_K - planificador.getVentanasLibres();
        dibujarBarraRecurso(g2, x + barWidth + 20, y - h / 2 + 8, Math.min(120, w - barWidth - 140), 16,
                           planificador.getVentanasLibres(), VENTANAS_K,
                           "Ventanas", new Color(255, 150, 0));
        
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 10f));
        g2.setColor(Color.WHITE);
        int colaAlta = planificador.getColaAlta().size();
        int colaNormal = planificador.getColaNormal().size();
        
        int statsX = x + barWidth + Math.min(120, w - barWidth - 140) + 30;
        if (statsX + 80 < x + w - 10) {
            g2.drawString("Cola A:" + colaAlta, statsX, y - h / 2 + 18);
            g2.drawString("Cola N:" + colaNormal, statsX, y - h / 2 + 32);
        }
        
        g2.setClip(oldClip);
    }
    
    private void dibujarInfoModo(Graphics2D g2, int W, int y) {
        String modoText = currentMode.toString();
        
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 12f));
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth("MODO: " + modoText);
        
        int x = W - tw - 25;
        
        g2.setColor(new Color(20, 20, 35, 200));
        g2.fillRoundRect(x - 10, y - 14, tw + 20, 22, 8, 8);
        
        g2.setColor(new Color(100, 200, 255));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(x - 10, y - 14, tw + 20, 22, 8, 8);
        
        g2.setColor(new Color(120, 120, 140));
        g2.drawString("MODO:", x, y);
        g2.setColor(new Color(0, 255, 150));
        g2.drawString(modoText, x + fm.stringWidth("MODO: "), y);
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

    // MÉTODOS PARA CONECTAR CON EL SISTEMA DE MÉTRICAS
    public MetricasGpuCollector getMetricasCollector() {
        if (planificador != null && planificador.getMetricas() != null) {
            return planificador.getMetricas();
        }
        return null;
    }

    public void setMetricasCollector(MetricasGpuCollector metricas) {
        if (planificador != null) {
            planificador.setMetricasCollector(metricas);
        }
    }
}