package com.mycompany.proyectopcypoto2025;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Container;

public class GpuClusterPanel extends JPanel implements Reseteable, Demoable, SyncAware {
    
    private PlanificadorGPU planificador;
    private final List<Thread> replicaThreads = new ArrayList<>();
    private final List<Job> trabajos = new ArrayList<>();
    private volatile boolean running = false;
    private Timer animTimer;
    private int jobCounter = 0;
    
    private SyncMode currentMode = SyncMode.MONITORES; // Inicia en modo compatible (Monitores)

    // Parámetros del clúster
    private final int NUM_ISLAS = 3; 
    private final int GPUS_POR_ISLA = 8; 
    private final int TOKENS_GLOBAL = 30; 
    private final int TOKENS_ISLA = 12; 
    private final int VENTANAS_K = 3; 

    public GpuClusterPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(240, 240, 250));
        
        planificador = new PlanificadorGPU(NUM_ISLAS, GPUS_POR_ISLA, TOKENS_GLOBAL, TOKENS_ISLA, VENTANAS_K);
        
        lanzarTrabajosDemo();
        
        animTimer = new Timer(50, e -> repaint());
        animTimer.start();
    }
    
    private void lanzarTrabajosDemo() {
        if(running) return;
        running = true;
        
        for (int i = 0; i < 5; i++) {
            final int jobIndex = i; 
            
            jobCounter++;
            int g = ThreadLocalRandom.current().nextInt(2, 6); // 2 a 5 GPUs
            int b = ThreadLocalRandom.current().nextInt(1, 4); // 1 a 3 Tokens
            boolean alta = jobIndex % 3 == 0; 
            Job newJob = new Job(jobCounter, g, b, 1, alta);
            trabajos.add(newJob);
            
            // Lanzar hilos de réplica (CORRECCIÓN DEL ERROR DE COMPILACIÓN)
            for(int r = 0; r < g; r++) {
                final int replicaId = r; // << CORRECCIÓN: Variable efectivamente final >>
                
                Thread t = new Thread(() -> replicaCiclo(newJob, replicaId), newJob.id + "-R" + replicaId);
                replicaThreads.add(t);
                t.start();
            }
        }
    }
    
    // Nuevo método para simular fallos de sincronización
    private void pausaCondicional(Job J) throws InterruptedException {
        // Monitores y Barreras: Ejecución normal
        if (currentMode == SyncMode.MONITORES || currentMode == SyncMode.BARRERAS) {
            return;
        }

        if (currentMode == SyncMode.SEMAFOROS) {
            // Simular Inanición / Starvation en WAIT
            if (J.estado == Job.Estado.WAIT) {
                dormir(1000); 
            }
        } else if (currentMode == SyncMode.MUTEX) {
            // Simular Live-Lock en COMM por falta de espera condicional
            if (J.estado == Job.Estado.COMM) {
                throw new InterruptedException("Livelock simulado.");
            }
        } else if (currentMode == SyncMode.VAR_CONDICION) {
            // Simular Race Condition (asignación atómica rota)
            if (J.islaAsignada != -1 && J.estado == Job.Estado.RUN) {
                if (ThreadLocalRandom.current().nextDouble() < 0.1) {
                    J.reportarFallo(); 
                    J.estado = Job.Estado.PREEMPTED;
                }
            }
        }
    }
    
    // Implementa el ciclo de la réplica (página 4 del PDF)
    private void replicaCiclo(Job J, int r) {
        try {
            if (r == 0) { // Hilo líder solicita el gang
                pausaCondicional(J); 
                planificador.solicitarGang(J);
            }
            
            while (J.islaAsignada == -1 && J.estado == Job.Estado.WAIT) {
                pausaCondicional(J); 
                dormir(20);
            }

            if (J.islaAsignada == -1 || J.estado == Job.Estado.PREEMPTED) return;
            
            // Barrera inicial (start)
            planificador.entrarBarrera(J);

            while (J.estaEjecutando()) {
                pausaCondicional(J);
                // 1. Fase Compute (Entrenar)
                dormir(ThreadLocalRandom.current().nextInt(100, 250)); 
                
                pausaCondicional(J);
                // 2. Barrera (step) -> punto seguro/preempción
                planificador.entrarBarrera(J);
                
                if (J.estado == Job.Estado.PREEMPTED) break; 
                
                pausaCondicional(J);
                // 3. Colectiva (AllReduce)
                planificador.solicitarVentanaColectiva(J);
                dormir(ThreadLocalRandom.current().nextInt(80, 160)); // allReduceGradientes
                planificador.liberarVentanaColectiva(J);
            }
            
            // Barrera final
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
        
        // 1. Dibujar Islas GPU
        for (int i = 0; i < NUM_ISLAS; i++) {
            int y = 30 + i * islandHeight;
            dibujarIsla(g2, i, 20, y, W - 40, islandHeight - 10);
        }
        
        // 2. Dibujar Estado Global
        dibujarEstadoGlobal(g2, W/2, H - 20);

        // 3. Dibujar Trabajos
        dibujarTrabajos(g2, W, H);
        
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
        
        g2.dispose();
    }
    
    private void dibujarIsla(Graphics2D g2, int id, int x, int y, int w, int h) {
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
        g2.drawString("GPUs: " + gpusLibres + "/" + GPUS_POR_ISLA, x + 10, y + h - 18);
        g2.drawString("Tokens: " + tokensLibres + "/" + TOKENS_ISLA, x + 100, y + h - 18);
        
        // Dibujo de las GPUs
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
    
    private void dibujarTrabajos(Graphics2D g2, int W, int H) {
        
        for (Job J : trabajos) {
            int isla = J.islaAsignada;
            int statusX, statusY;

            if (J.estado == Job.Estado.WAIT) {
                // En cola
                int colaOffset = J.prioridadAlta ? 0 : 100;
                int index = 0;
                
                // Aproxima la posición en la cola
                if (J.prioridadAlta) {
                    index = new ArrayList<>(planificador.getColaAlta()).indexOf(J);
                } else {
                    index = new ArrayList<>(planificador.getColaNormal()).indexOf(J);
                }
                
                statusX = 50 + colaOffset;
                statusY = H - 80 + index * 20;

            } else if (isla != -1) {
                // Asignado a Isla (lo ponemos a la derecha para ver el flujo)
                int islandY = 30 + isla * ((H - 60) / NUM_ISLAS);
                statusX = W - 100;
                statusY = islandY + 50;

            } else {
                // Preempted o Finalizado (lo ponemos abajo a la derecha)
                statusX = W - 50;
                statusY = H - 50;
            }
            
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
    }
    
    @Override public void demo() {}
    
    private void dormir(long ms) { 
        try { Thread.sleep(ms); } catch(InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } 
    }
}