
package com.mycompany.proyectopcypoto2025;
import javax.swing.*; import java.awt.*;
public class ProductorConsumidorPanel extends JPanel implements Reseteable, Demoable, SyncAware {
    private final Tanque tanque = new Tanque();
    private SyncMode modo = SyncMode.SEMAFOROS;
    private Thread prod, cons; private volatile boolean running=false;
    public ProductorConsumidorPanel(){ setLayout(new BorderLayout()); add(tanque, BorderLayout.CENTER); autoStart(); }
    private void autoStart(){ if(running) return; running=true;
        prod = new Thread(() -> { int v=0; while(running){ tanque.addNivel(); dormir(180); if(++v>2000) v=0; } }, "PC-P");
        cons = new Thread(() -> { int v=0; while(running){ tanque.remNivel(); dormir(260); if(++v>2000) v=0; } }, "PC-C");
        prod.start(); cons.start();
    }
    @Override public void setSyncMode(SyncMode m){ this.modo=m; }
    @Override public void reset(){ running=false; tanque.setNivel(0); autoStart(); }
    @Override public void demo(){}
    private void dormir(long ms){ try{ Thread.sleep(ms);}catch(Exception ignored){} }

    static class Tanque extends JPanel {
        private int nivel=0; // 0..20
        public Tanque(){ setPreferredSize(new Dimension(560,280)); setBackground(Color.white); new Timer(33,e->repaint()).start(); }
        public synchronized void addNivel(){ if(nivel<20) nivel++; }
        public synchronized void remNivel(){ if(nivel>0) nivel--; }
        public synchronized void setNivel(int n){ nivel=Math.max(0,Math.min(20,n)); }
        @Override protected void paintComponent(Graphics g){ super.paintComponent(g); Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int W=180,H=220,x= getWidth()/2 - W/2, y=30; // tanque
            g2.setColor(new Color(200,200,200)); g2.fillRoundRect(x-6,y-6,W+12,H+12,20,20);
            g2.setColor(Color.WHITE); g2.fillRoundRect(x,y,W,H,16,16);
            g2.setColor(Color.GRAY); g2.setStroke(new BasicStroke(2f)); g2.drawRoundRect(x,y,W,H,16,16);
            int niveles=20; int h=H/niveles; int nl; synchronized(this){ nl=this.nivel; }
            for(int i=0;i<niveles;i++){ int yy=y+H-(i+1)*h; if(i<nl){ g2.setColor(new Color(120,190,255)); g2.fillRect(x+3, yy+3, W-6, h-4); } g2.setColor(new Color(220,220,220)); g2.drawLine(x+3, yy, x+W-3, yy); }
            g2.setColor(Color.BLACK); g2.drawString("Tanque de agua ("+nl+"/20)", x+40, y+H+18);
            g2.dispose();
        }
    }
}
