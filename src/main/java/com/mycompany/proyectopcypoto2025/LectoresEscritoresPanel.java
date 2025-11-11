package com.mycompany.proyectopcypoto2025;
import javax.swing.*; 
import java.awt.*;
import java.awt.geom.*;

public class LectoresEscritoresPanel extends JPanel implements Reseteable, Demoable, SyncAware {
    private final Pizarra piz = new Pizarra(); 
    private volatile boolean running = false; 
    private Thread writer, reader; 
    private SyncMode modo = SyncMode.MONITORES;
    
    public LectoresEscritoresPanel() { 
        setLayout(new BorderLayout()); 
        add(piz, BorderLayout.CENTER); 
        start(); 
    }
    
    private void start() { 
        if(running) return; 
        running = true;
        
        writer = new Thread(() -> { 
            while(running) { 
                piz.modo = "ESCRITURA"; 
                piz.escritor = true; 
                piz.valor++; 
                dormir(280); 
                piz.escritor = false; 
                piz.modo = "LECTURA"; 
                dormir(140);
            } 
        }, "W");
        
        reader = new Thread(() -> { 
            while(running) { 
                if(!piz.escritor) { 
                    piz.lector = true; 
                    dormir(160); 
                    piz.lector = false; 
                } 
                dormir(100);
            } 
        }, "R");
        
        writer.start(); 
        reader.start();
    }
    
    @Override public void setSyncMode(SyncMode m) { this.modo = m; }
    @Override public void reset() { running = false; piz.reset(); start(); }
    @Override public void demo() {}
    private void dormir(long ms) { 
        try { Thread.sleep(ms); } catch(Exception ignored) {} 
    }

    static class Pizarra extends JPanel {
        String modo = "LECTURA"; 
        boolean escritor = false, lector = false; 
        int valor = 0; 
        int t = 0;
        private int boardGlow = 0;
        private int writeAnim = 0;
        private Timer animTimer;
        
        public Pizarra() { 
            setPreferredSize(new Dimension(560, 260)); 
            setBackground(Color.white); 
            
            animTimer = new Timer(50, e -> {
                t++; 
                boardGlow = (boardGlow + 1) % 100;
                writeAnim = (writeAnim + 1) % 60;
                repaint();
            });
            animTimer.start();
        }
        
        public void reset() { 
            modo = "LECTURA"; 
            escritor = false; 
            lector = false; 
            valor = 0; 
        }
        
        @Override 
        protected void paintComponent(Graphics g) { 
            super.paintComponent(g); 
            Graphics2D g2 = (Graphics2D) g.create(); 
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Pizarra mejorada con marco
            int px = 120, py = 40, pw = 320, ph = 80;
            
            // Marco de madera
            g2.setColor(new Color(139, 69, 19));
            g2.fillRoundRect(px - 8, py - 8, pw + 16, ph + 16, 14, 14);
            
            // Pizarra con brillo si está siendo escrita
            if(escritor) {
                float alpha = (float)(Math.sin(boardGlow * 0.1) * 0.2 + 0.8);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g2.setColor(new Color(50, 120, 60));
            } else {
                g2.setColor(new Color(30, 100, 50));
            }
            g2.fillRoundRect(px, py, pw, ph, 10, 10);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            
            // Borde de la pizarra
            g2.setColor(new Color(220, 220, 220)); 
            g2.setStroke(new BasicStroke(2f)); 
            g2.drawRoundRect(px, py, pw, ph, 10, 10);
            
            // Contenido de la pizarra
            g2.setColor(Color.WHITE); 
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 18f)); 
            String texto = modo + " (valor=" + valor + ")"; 
            FontMetrics fm = g2.getFontMetrics(); 
            g2.drawString(texto, px + (pw - fm.stringWidth(texto))/2, py + ph/2 + 6);
            
            // Marcador de escritura animado
            if(escritor) {
                int markerX = px + 20 + (writeAnim % 280);
                g2.setColor(Color.YELLOW);
                g2.fillRect(markerX, py + ph/2 + 12, 2, 14);
            }
            
            // Borrador y marcadores
            g2.setColor(new Color(200, 200, 200));
            g2.fillRect(px + pw - 60, py + ph + 12, 30, 12);
            
            g2.setColor(Color.BLUE);
            g2.fillRect(px + pw - 100, py + ph + 12, 6, 14);
            g2.setColor(Color.RED);
            g2.fillRect(px + pw - 92, py + ph + 12, 6, 14);
            g2.setColor(Color.GREEN);
            g2.fillRect(px + pw - 84, py + ph + 12, 6, 14);
            
            // Estado de la pizarra
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 12f));
            if(escritor) {
                g2.setColor(Color.RED);
                g2.drawString("OCUPADA - ESCRIBIENDO", px + 20, py + ph + 36);
            } else {
                g2.setColor(new Color(0, 150, 0));
                g2.drawString("DISPONIBLE PARA LECTURA", px + 20, py + ph + 36);
            }
            
            // Lector (izquierda) - mejorado
            int lx = 220, ly = 170;
            dibujarLector(g2, lx, ly, lector);
            g2.setColor(Color.BLACK); 
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 12f));
            g2.drawString("Lector", lx - 18, ly + 50);
            
            // Escritor (derecha) - mejorado
            int ex = 340, ey = 170;
            dibujarEscritor(g2, ex, ey, escritor);
            g2.setColor(Color.BLACK); 
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 12f));
            g2.drawString("Escritor", ex - 24, ey + 50);
            
            g2.dispose();
        }
        
        private void dibujarLector(Graphics2D g2, int x, int y, boolean activo) {
            Color color = activo ? new Color(120, 220, 160) : new Color(100, 150, 255);
            
            // Cuerpo
            g2.setColor(color);
            g2.fillRect(x - 12, y - 4, 24, 28);
            
            // Cabeza
            g2.setColor(new Color(255, 220, 180));
            g2.fillOval(x - 14, y - 24, 28, 28);
            
            // Pelo
            g2.setColor(new Color(50, 30, 20));
            g2.fillArc(x - 14, y - 26, 28, 20, 0, 180);
            
            // Anteojos
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(x - 10, y - 16, 8, 8);
            g2.drawOval(x + 2, y - 16, 8, 8);
            g2.drawLine(x - 2, y - 12, x + 2, y - 12);
            
            // Ojos
            g2.setColor(Color.WHITE);
            g2.fillOval(x - 8, y - 16, 6, 6);
            g2.fillOval(x + 3, y - 16, 6, 6);
            g2.setColor(Color.BLACK);
            g2.fillOval(x - 6, y - 14, 3, 3);
            g2.fillOval(x + 5, y - 14, 3, 3);
            
            // Boca
            g2.drawLine(x - 6, y - 6, x + 6, y - 6);
            
            // Libro si está leyendo
            if(activo) {
                g2.setColor(new Color(255, 220, 180));
                g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x - 12, y + 14, x - 6, y + 22);
                g2.drawLine(x + 12, y + 14, x + 6, y + 22);
                
                g2.setColor(new Color(200, 150, 100));
                g2.fillRect(x - 10, y + 18, 20, 14);
                g2.setColor(Color.BLACK);
                g2.drawRect(x - 10, y + 18, 20, 14);
                g2.drawLine(x, y + 18, x, y + 32);
            } else {
                g2.setColor(new Color(255, 220, 180));
                g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x - 12, y + 10, x - 18, y + 20);
                g2.drawLine(x + 12, y + 10, x + 18, y + 20);
            }
            
            // Piernas
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(5f));
            g2.drawLine(x - 6, y + 24, x - 6, y + 38);
            g2.drawLine(x + 6, y + 24, x + 6, y + 38);
        }
        
        private void dibujarEscritor(Graphics2D g2, int x, int y, boolean activo) {
            Color color = activo ? new Color(120, 220, 160) : new Color(255, 100, 100);
            
            // Cuerpo
            g2.setColor(color);
            g2.fillRect(x - 12, y - 4, 24, 28);
            
            // Cabeza
            g2.setColor(new Color(255, 220, 180));
            g2.fillOval(x - 14, y - 24, 28, 28);
            
            // Pelo rizado
            g2.setColor(new Color(100, 70, 40));
            for(int i = 0; i < 4; i++) {
                g2.fillOval(x - 12 + i * 7, y - 26, 6, 12);
            }
            
            // Ojos
            g2.setColor(Color.WHITE);
            g2.fillOval(x - 8, y - 16, 6, 6);
            g2.fillOval(x + 2, y - 16, 6, 6);
            g2.setColor(Color.BLACK);
            g2.fillOval(x - 6, y - 14, 3, 3);
            g2.fillOval(x + 4, y - 14, 3, 3);
            
            // Boca
            if(activo) {
                g2.drawArc(x - 6, y - 8, 12, 8, 0, -180);
            } else {
                g2.drawLine(x - 6, y - 6, x + 6, y - 6);
            }
            
            // Marcador si está escribiendo
            if(activo) {
                g2.setColor(new Color(255, 220, 180));
                g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x + 12, y + 10, x + 22, y + 2);
                
                g2.setColor(Color.BLUE);
                g2.fillRect(x + 21, y, 2, 10);
                g2.setColor(Color.BLUE.darker());
                g2.fillOval(x + 20, y - 1, 4, 4);
            } else {
                g2.setColor(new Color(255, 220, 180));
                g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x + 12, y + 10, x + 18, y + 20);
            }
            
            // Brazo izquierdo
            g2.drawLine(x - 12, y + 10, x - 18, y + 20);
            
            // Piernas
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(5f));
            g2.drawLine(x - 6, y + 24, x - 6, y + 38);
            g2.drawLine(x + 6, y + 24, x + 6, y + 38);
        }
    }
}