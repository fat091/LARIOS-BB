package com.mycompany.proyectopcypoto2025;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
<<<<<<< HEAD
import java.util.concurrent.ThreadLocalRandom;
=======
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Container;
>>>>>>> 23e934d68df5d3d253bd9fa0253db12338618c8b

public class GpuClusterPanel extends JPanel implements Reseteable, Demoable, SyncAware {
    
    private PlanificadorGPU planificador;
    private final List<Thread> replicaThreads = new ArrayList<>();
    private final List<Job> trabajos = new ArrayList<>();
    private volatile boolean running = false;
    private Timer animTimer;
    private int jobCounter = 0;
<<<<<<< HEAD
    private int frameCount = 0; // Para animación de brillo/parpadeo
    
    private SyncMode currentMode = SyncMode.MONITORES; 
=======
    
    private SyncMode currentMode = SyncMode.MONITORES; // Inicia en modo compatible (Monitores)
>>>>>>> 23e934d68df5d3d253bd9fa0253db12338618c8b

    // Parámetros del clúster
    private final int NUM_ISLAS = 3; 
    private final int GPUS_POR_ISLA = 8; 
    private final int TOKENS_GLOBAL = 30; 
    private final int TOKENS_ISLA = 12; 
    private final int VENTANAS_K = 3; 

    public GpuClusterPanel() {
        setLayout(new BorderLayout());
<<<<<<< HEAD
        setBackground(new Color(20, 20, 40)); // Fondo oscuro de Data Center
=======
        setBackground(new Color(240, 240, 250));
>>>>>>> 23e934d68df5d3d253bd9fa0253db12338618c8b
        
        planificador = new PlanificadorGPU(NUM_ISLAS, GPUS_POR_ISLA, TOKENS_GLOBAL, TOKENS_ISLA, VENTANAS_K);
        
        lanzarTrabajosDemo();
        
<<<<<<< HEAD
        // Timer de repintado
        animTimer = new Timer(1000, e -> {
            frameCount++;
            repaint();
        }); 
=======
        animTimer = new Timer(50, e -> repaint());
>>>>>>> 23e934d68df5d3d253bd9fa0253db12338618c8b
        animTimer.start();
    }
    
    private void lanzarTrabajosDemo() {
        if(running) return;
        running = true;
        
        for (int i = 0; i < 5; i++) {
            final int jobIndex = i; 
            
            jobCounter++;
<<<<<<< HEAD
            int g = ThreadLocalRandom.current().nextInt(2, 6); 
            int b = ThreadLocalRandom.current().nextInt(1, 4); 
=======
            int g = ThreadLocalRandom.current().nextInt(2, 6); // 2 a 5 GPUs
            int b = ThreadLocalRandom.current().nextInt(1, 4); // 1 a 3 Tokens
>>>>>>> 23e934d68df5d3d253bd9fa0253db12338618c8b
            boolean alta = jobIndex % 3 == 0; 
            Job newJob = new Job(jobCounter, g, b, 1, alta);
            trabajos.add(newJob);
            
<<<<<<< HEAD
            for(int r = 0; r < g; r++) {
                final int replicaId = r; 
=======
            // Lanzar hilos de réplica (CORRECCIÓN DEL ERROR DE COMPILACIÓN)
            for(int r = 0; r < g; r++) {
                final int replicaId = r; // << CORRECCIÓN: Variable efectivamente final >>
>>>>>>> 23e934d68df5d3d253bd9fa0253db12338618c8b
                
                Thread t = new Thread(() -> replicaCiclo(newJob, replicaId), newJob.id + "-R" + replicaId);
                replicaThreads.add(t);
                t.start();
            }
        }
    }
    
<<<<<<< HEAD
=======
    // Nuevo método para simular fallos de sincronización
>>>>>>> 23e934d68df5d3d253bd9fa0253db12338618c8b
    private void pausaCondicional(Job J) throws InterruptedException {
        // Monitores y Barreras: Ejecución normal
        if (currentMode == SyncMode.MONITORES || currentMode == SyncMode.BARRERAS) {
            return;
        }

        if (currentMode == SyncMode.SEMAFOROS) {
<<<<<<< HEAD
            // Simular Inanición / Starvation en WAIT 
=======
            // Simular Inanición / Starvation en WAIT
>>>>>>> 23e934d68df5d3d253bd9fa0253db12338618c8b
            if (J.estado == Job.Estado.WAIT) {
                dormir(1000); 
            }
        } else if (currentMode == SyncMode.MUTEX) {
<<<<<<< HEAD
            // Simular Live-Lock en COMM 
            if (J.estado == Job.Estado.COMM) {
                if (ThreadLocalRandom.current().nextDouble() < 0.2) {
                    throw new InterruptedException("Livelock simulado.");
                }
            }
        } else if (currentMode == SyncMode.VAR_CONDICION) {
            // Simular Race Condition (falla la asignación RUN aleatoriamente)
            if (J.islaAsignada != -1 && J.estado == Job.Estado.RUN) {
                if (ThreadLocalRandom.current().nextDouble() < 0.05) {
=======
            // Simular Live-Lock en COMM por falta de espera condicional
            if (J.estado == Job.Estado.COMM) {
                throw new InterruptedException("Livelock simulado.");
            }
        } else if (currentMode == SyncMode.VAR_CONDICION) {
            // Simular Race Condition (asignación atómica rota)
            if (J.islaAsignada != -1 && J.estado == Job.Estado.RUN) {
                if (ThreadLocalRandom.current().nextDouble() < 0.1) {
>>>>>>> 23e934d68df5d3d253bd9fa0253db12338618c8b
                    J.reportarFallo(); 
                    J.estado = Job.Estado.PREEMPTED;
                }
            }
        }
    }
    
<<<<<<< HEAD
    private void replicaCiclo(Job J, int r) {
        try {
            if (r == 0) { 
=======
    // Implementa el ciclo de la réplica (página 4 del PDF)
    private void replicaCiclo(Job J, int r) {
        try {
            if (r == 0) { // Hilo líder solicita el gang
>>>>>>> 23e934d68df5d3d253bd9fa0253db12338618c8b
                pausaCondicional(J); 
                planificador.solicitarGang(J);
            }
            
            while (J.islaAsignada == -1 && J.estado == Job.Estado.WAIT) {
                pausaCondicional(J); 
                dormir(20);
            }

            if (J.islaAsignada == -1 || J.estado == Job.Estado.PREEMPTED) return;
            
<<<<<<< HEAD
=======
            // Barrera inicial (start)
>>>>>>> 23e934d68df5d3d253bd9fa0253db12338618c8b
            planificador.entrarBarrera(J);

            while (J.estaEjecutando()) {
                pausaCondicional(J);
                // 1. Fase Compute (Entrenar)
<<<<<<< HEAD
                dormir(ThreadLocalRandom.current().nextInt(400, 800)); 
                
                pausaCondicional(J);
                // 2. Barrera (step)
=======
                dormir(ThreadLocalRandom.current().nextInt(100, 250)); 
                
                pausaCondicional(J);
                // 2. Barrera (step) -> punto seguro/preempción
>>>>>>> 23e934d68df5d3d253bd9fa0253db12338618c8b
                planificador.entrarBarrera(J);
                
                if (J.estado == Job.Estado.PREEMPTED) break; 
                
                pausaCondicional(J);
                // 3. Colectiva (AllReduce)
                planificador.solicitarVentanaColectiva(J);
<<<<<<< HEAD
                dormir(ThreadLocalRandom.current().nextInt(200, 400)); 
                planificador.liberarVentanaColectiva(J);
            }
            
=======
                dormir(ThreadLocalRandom.current().nextInt(80, 160)); // allReduceGradientes
                planificador.liberarVentanaColectiva(J);
            }
            
            // Barrera final
>>>>>>> 23e934d68df5d3d253bd9fa0253db12338618c8b
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
    
<<<<<<< HEAD
    // =========================================================
    // MÉTODOS DE DIBUJO CON ICONOGRAFÍA PROFESIONAL (GPU/USB)
    // =========================================================

    private void dibujarGPU(Graphics2D g2, int x, int y, Color coreColor, boolean isJob) {
        int w = 40, h = 18;
        
        // 1. Cuerpo de la GPU (gris oscuro, chasis)
        g2.setColor(new Color(50, 50, 60));
        g2.fillRoundRect(x, y, w, h, 6, 6);
        
        // 2. Núcleo/Ventilador (Iluminación de estado)
        int coreSize = 10;
        int coreX = x + (w - coreSize) / 2;
        int coreY = y + (h - coreSize) / 2;
        
        // Efecto de Brillo/Sombra
        if (!isJob) {
            g2.setColor(new Color(90, 90, 100));
            g2.fillRoundRect(x + 2, y + 2, w - 4, h - 4, 4, 4);
        }

        // 3. Iluminación del núcleo
        float alpha = 1.0f;
        if (coreColor.equals(Color.RED) || coreColor.equals(Color.YELLOW)) {
            alpha = (float) (0.5 + 0.5 * Math.abs(Math.sin(frameCount * 0.5))); 
        } else if (coreColor.equals(Color.GREEN) || coreColor.equals(new Color(100, 200, 100))) {
            alpha = (float) (0.8 + 0.2 * Math.abs(Math.sin(frameCount * 0.1)));
        }
        
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.setColor(coreColor);
        g2.fillOval(coreX, coreY, coreSize, coreSize);
        
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        g2.setColor(Color.BLACK);
        g2.drawRoundRect(x, y, w, h, 6, 6);
    }
    
=======
>>>>>>> 23e934d68df5d3d253bd9fa0253db12338618c8b
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int W = getWidth(), H = getHeight();
        int islandHeight = (H - 60) / NUM_ISLAS;
        
<<<<<<< HEAD
        // 1. Dibujar Islas GPU 
=======
        // 1. Dibujar Islas GPU
>>>>>>> 23e934d68df5d3d253bd9fa0253db12338618c8b
        for (int i = 0; i < NUM_ISLAS; i++) {
            int y = 30 + i * islandHeight;
            dibujarIsla(g2, i, 20, y, W - 40, islandHeight - 10);
        }
        
        // 2. Dibujar Estado Global
        dibujarEstadoGlobal(g2, W/2, H - 20);

        // 3. Dibujar Trabajos
        dibujarTrabajos(g2, W, H);
        
<<<<<<< HEAD
=======
        // 4. Actualizar el Grafo de Recursos con el estado actual
        Container parent = this.getTopLevelAncestor();
        if (parent instanceof JFrame frame) {
            // Búsqueda de la instancia del GrafoPanel
            for (Component comp : frame.getContentPane().getComponents()) {
                if (comp instanceof JSplitPane split1) {
                    for (Component comp2 : split1.getComponents()) {
                        if (comp2 instanceof JSplitPane split2) {
                            Component topPanel = split2.getTopComponent();
                            if (topPanel instanceof JPanel wrapPanel) {
                                for (Component innerComp : wrapPanel.getComponents()) {
                                    if (innerComp instanceof GrafoPanel gp) {
                                        // Actualiza la animación del grafo si es el problema activo
                                        gp.getGrafo().updateGpuClusterGraph(trabajos); 
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
>>>>>>> 23e934d68df5d3d253bd9fa0253db12338618c8b
        g2.dispose();
    }
    
    private void dibujarIsla(Graphics2D g2, int id, int x, int y, int w, int h) {
<<<<<<< HEAD
        // Recuadro de la Isla (Azul oscuro Data Center)
        g2.setColor(new Color(30, 30, 80));
        g2.fillRoundRect(x, y, w, h, 15, 15);
        g2.setColor(new Color(100, 100, 255));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(x, y, w, h, 15, 15);
        
        // Título
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 16f));
        g2.setColor(Color.WHITE);
        g2.drawString("Isla GPU " + id, x + 10, y + 25);
        
        int gpusLibres = planificador.getGpuLibres().getOrDefault(id, 0);
        int tokensLibres = planificador.getTokensIsla().getOrDefault(id, 0);
        
        // Métricas de la Isla (Texto blanco)
        g2.setFont(g2.getFont().deriveFont(10f));
        g2.setColor(new Color(180, 180, 255));
=======
        // Recuadro de la Isla
        g2.setColor(new Color(220, 230, 255));
        g2.fillRoundRect(x, y, w, h, 15, 15);
        g2.setColor(new Color(100, 100, 200));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(x, y, w, h, 15, 15);
        
        // Título de la Isla
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14f));
        g2.drawString("Isla GPU " + id, x + 10, y + 20);
        
        // Métricas de la Isla
        int gpusLibres = planificador.getGpuLibres().getOrDefault(id, 0);
        int tokensLibres = planificador.getTokensIsla().getOrDefault(id, 0);
        
        g2.setFont(g2.getFont().deriveFont(10f));
        g2.setColor(Color.DARK_GRAY);
>>>>>>> 23e934d68df5d3d253bd9fa0253db12338618c8b
        g2.drawString("GPUs: " + gpusLibres + "/" + GPUS_POR_ISLA, x + 10, y + h - 18);
        g2.drawString("Tokens: " + tokensLibres + "/" + TOKENS_ISLA, x + 100, y + h - 18);
        
        // Dibujo de las GPUs
<<<<<<< HEAD
        int gx = x + 10;
        int gy = y + 40;
        int gpuGap = 45; 
        
        for(int i = 0; i < GPUS_POR_ISLA; i++) {
            int currentGx = gx + i * gpuGap;
            boolean libre = i < gpusLibres;
            
            Color coreColor = libre ? new Color(0, 255, 100) : new Color(255, 50, 50);
            dibujarGPU(g2, currentGx, gy, coreColor, false);
        }
    }

=======
        int gpuW = 18, gpuH = 10;
        int gx = x + 10;
        int gy = y + 30;
        for(int i = 0; i < GPUS_POR_ISLA; i++) {
            int currentGx = gx + i * (gpuW + 4);
            boolean libre = i < gpusLibres;
            
            g2.setColor(libre ? new Color(150, 255, 150) : new Color(255, 150, 150));
            g2.fillRoundRect(currentGx, gy, gpuW, gpuH, 4, 4);
            g2.setColor(Color.BLACK);
            g2.drawRoundRect(currentGx, gy, gpuW, gpuH, 4, 4);
        }
    }
    
    private void dibujarEstadoGlobal(Graphics2D g2, int cx, int y) {
        g2.setColor(new Color(255, 240, 200));
        g2.fillRoundRect(cx - 150, y - 10, 300, 20, 10, 10);
        g2.setColor(new Color(200, 150, 0));
        g2.drawRoundRect(cx - 150, y - 10, 300, 20, 10, 10);
        
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 12f));
        g2.setColor(Color.DARK_GRAY);
        String globalState = String.format("Global: Tokens %d/%d | Ventanas %d/%d", 
                planificador.getTokensGlobal(), TOKENS_GLOBAL, 
                planificador.getVentanasLibres(), VENTANAS_K);
        g2.drawString(globalState, cx - 145, y + 5);
        
        // Colas de espera
        g2.setFont(g2.getFont().deriveFont(10f));
        g2.setColor(Color.RED);
        g2.drawString("A:"+planificador.getColaAlta().size(), cx - 180, y + 5);
        g2.setColor(Color.BLUE);
        g2.drawString("N:"+planificador.getColaNormal().size(), cx + 160, y + 5);
    }
    
>>>>>>> 23e934d68df5d3d253bd9fa0253db12338618c8b
    private void dibujarTrabajos(Graphics2D g2, int W, int H) {
        
        for (Job J : trabajos) {
            int isla = J.islaAsignada;
            int statusX, statusY;

            if (J.estado == Job.Estado.WAIT) {
<<<<<<< HEAD
                int colaOffset = J.prioridadAlta ? 0 : 100;
                int index = new ArrayList<>(J.prioridadAlta ? planificador.getColaAlta() : planificador.getColaNormal()).indexOf(J);
=======
                // En cola
                int colaOffset = J.prioridadAlta ? 0 : 100;
                int index = 0;
                
                // Aproxima la posición en la cola
                if (J.prioridadAlta) {
                    index = new ArrayList<>(planificador.getColaAlta()).indexOf(J);
                } else {
                    index = new ArrayList<>(planificador.getColaNormal()).indexOf(J);
                }
                
>>>>>>> 23e934d68df5d3d253bd9fa0253db12338618c8b
                statusX = 50 + colaOffset;
                statusY = H - 80 + index * 20;

            } else if (isla != -1) {
<<<<<<< HEAD
=======
                // Asignado a Isla (lo ponemos a la derecha para ver el flujo)
>>>>>>> 23e934d68df5d3d253bd9fa0253db12338618c8b
                int islandY = 30 + isla * ((H - 60) / NUM_ISLAS);
                statusX = W - 100;
                statusY = islandY + 50;

            } else {
<<<<<<< HEAD
=======
                // Preempted o Finalizado (lo ponemos abajo a la derecha)
>>>>>>> 23e934d68df5d3d253bd9fa0253db12338618c8b
                statusX = W - 50;
                statusY = H - 50;
            }
            
<<<<<<< HEAD
            Color jobCoreColor = switch(J.estado) {
                case WAIT -> Color.GRAY; 
                case RUN -> Color.GREEN; 
                case BARRIER -> Color.YELLOW; 
                case COMM -> Color.MAGENTA; 
                case PREEMPTED, ABORTED -> Color.RED;
                case FINISHED -> Color.DARK_GRAY;
            };
            
            dibujarGPU(g2, statusX, statusY, jobCoreColor, true); 
            
            g2.setColor(Color.WHITE); 
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 10f));
            String info = J.id + " " + J.estado.name().substring(0, 3);
            
            g2.drawString(info, statusX + 45, statusY + 12); 
        }
    }
    
    private void dibujarEstadoGlobal(Graphics2D g2, int cx, int y) {
        int B_USED = TOKENS_GLOBAL - planificador.getTokensGlobal();
        int K_USED = VENTANAS_K - planificador.getVentanasLibres();
        
        // 1. Fondo del Display Global
        g2.setColor(new Color(10, 10, 10));
        g2.fillRect(cx - 160, y - 18, 320, 36);
        g2.setColor(new Color(0, 255, 255));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRect(cx - 160, y - 18, 320, 36);

        // 2. Display de Tokens Globales (Barra de progreso)
        double tokenRatio = (double)B_USED / TOKENS_GLOBAL;
        int barW = (int)(300 * tokenRatio);
        
        g2.setColor(Color.BLUE.darker());
        g2.fillRect(cx - 150, y - 15, 300, 10);
        g2.setColor(new Color(0, 255, 255)); // Cian
        g2.fillRect(cx - 150, y - 15, barW, 10);
        
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 9f));
        g2.setColor(Color.WHITE);
        g2.drawString("GLOBAL TOKENS USED: " + B_USED + "/" + TOKENS_GLOBAL, cx - 145, y - 6);

        // 3. Display de Ventanas y Colas
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 12f));
        g2.setColor(Color.WHITE);
        String status = String.format("WINDOWS: %d/%d | A:%d N:%d", 
                K_USED, VENTANAS_K, planificador.getColaAlta().size(), planificador.getColaNormal().size());
        g2.drawString(status, cx - 145, y + 15);
    }

    @Override 
    public void setSyncMode(SyncMode m) { 
        // FIX: Se llama a reset() para que el nuevo modo de sincronización tenga efecto inmediato.
        this.currentMode = m; 
        reset(); 
=======
            Color jobColor = switch(J.estado) {
                case WAIT -> J.prioridadAlta ? new Color(255, 180, 180) : new Color(200, 200, 255);
                case RUN -> new Color(150, 255, 150);
                case BARRIER -> new Color(255, 255, 100);
                case COMM -> new Color(255, 150, 255);
                case PREEMPTED, ABORTED -> new Color(100, 100, 100);
                case FINISHED -> Color.LIGHT_GRAY;
            };
            
            g2.setColor(jobColor);
            g2.fillOval(statusX, statusY, 18, 18);
            g2.setColor(Color.BLACK);
            g2.drawOval(statusX, statusY, 18, 18);
            
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 10f));
            String info = J.id + " " + J.estado.name().substring(0, 3);
            g2.drawString(info, statusX + 22, statusY + 14);
        }
    }

    @Override 
    public void setSyncMode(SyncMode m) { 
        this.currentMode = m;
        // Reiniciamos la simulación para que se ejecute con el nuevo modo
        reset(); 
        
        String msg = switch (m) {
            case MONITORES, BARRERAS -> "ÉXITO: Monitores/Barreras permiten Gang Scheduling, Puntos Seguros y Control de Recursos. (Ejecución Normal).";
            case SEMAFOROS -> "FALLO SIMULADO: Semáforos puros pueden llevar a INANICIÓN (Starvation) en la asignación compleja de recursos (Gang Scheduling).";
            case MUTEX -> "FALLO SIMULADO: Mutex puro causará LIVE-LOCK en la fase de Comunicación (COMM) al bloquear la espera condicional de Tokens.";
            case VAR_CONDICION -> "FALLO SIMULADO: Var. Condición sin Mutex causará RACES CONDITIONS en la asignación atómica de recursos.";
            default -> "Modo seleccionado. Ejecutando simulación...";
        };
        JOptionPane.showMessageDialog(this, msg, "Efecto del Modo de Sincronización", JOptionPane.INFORMATION_MESSAGE);
>>>>>>> 23e934d68df5d3d253bd9fa0253db12338618c8b
    }
    
    @Override 
    public void reset() { 
        running = false;
<<<<<<< HEAD
        // Detener hilos y limpiar listas
        replicaThreads.forEach(Thread::interrupt);
        replicaThreads.clear();
        trabajos.clear();
        
        planificador = new PlanificadorGPU(NUM_ISLAS, GPUS_POR_ISLA, TOKENS_GLOBAL, TOKENS_ISLA, VENTANAS_K);
        jobCounter = 0;
        lanzarTrabajosDemo();
        
        // Forzar el repintado del panel.
        repaint();
=======
        replicaThreads.forEach(Thread::interrupt);
        replicaThreads.clear();
        trabajos.clear();
        planificador = new PlanificadorGPU(NUM_ISLAS, GPUS_POR_ISLA, TOKENS_GLOBAL, TOKENS_ISLA, VENTANAS_K);
        jobCounter = 0;
        lanzarTrabajosDemo();
>>>>>>> 23e934d68df5d3d253bd9fa0253db12338618c8b
    }
    
    @Override public void demo() {}
    
    private void dormir(long ms) { 
        try { Thread.sleep(ms); } catch(InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } 
    }
}