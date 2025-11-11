
package com.mycompany.proyectopcypoto2025;
import javax.swing.*; import java.awt.*; import java.util.LinkedList; import java.util.Queue;
public class BarberoDormilonPanel extends JPanel implements Reseteable, Demoable, SyncAware {
    private final Sala sala=new Sala(); private volatile boolean running=false; private Thread bar, clientes; private SyncMode modo=SyncMode.SEMAFOROS;
    public BarberoDormilonPanel(){ setLayout(new BorderLayout()); add(sala,BorderLayout.CENTER); start(); }
    private void start(){ if(running) return; running=true;
        bar = new Thread(() -> { while(running){ Integer id=null; synchronized(sala){ if(!sala.queue.isEmpty()) id=sala.queue.poll(); } if(id!=null){ sala.sillaAct=id; dormir(300); sala.sillaAct=-1; } else dormir(80);} },"Barbero");
        clientes = new Thread(() -> { int id=1; while(running){ synchronized(sala){ if(sala.queue.size()<3) sala.queue.add(id); } id++; dormir(180);} },"Clientes");
        bar.start(); clientes.start();
    }
    @Override public void setSyncMode(SyncMode m){ this.modo=m; }
    @Override public void reset(){ running=false; synchronized(sala){ sala.queue.clear(); sala.sillaAct=-1; } start(); }
    @Override public void demo(){}
    private void dormir(long ms){ try{ Thread.sleep(ms);}catch(Exception ignored){} }

    static class Sala extends JPanel{
        final Queue<Integer> queue=new LinkedList<>(); int sillaAct=-1;
        public Sala(){ setPreferredSize(new Dimension(560,280)); setBackground(Color.white); new Timer(33,e->repaint()).start(); }
        @Override protected void paintComponent(Graphics g){ super.paintComponent(g); Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int sx=40, sy=160; int gap=110;
            for(int i=0;i<3;i++){ g2.setColor(new Color(230,230,230)); g2.fillRoundRect(sx+i*gap, sy, 60, 40, 12,12); g2.setColor(Color.GRAY); g2.drawRoundRect(sx+i*gap, sy, 60, 40, 12,12); }
            int i=0; synchronized(this){ for(Integer id: queue){ if(i>=3) break; DrawUtil.person(g2, sx+i*gap+30, sy-10, new Color(200,220,255)); g2.setColor(Color.BLACK); g2.drawString("#"+id, sx+i*gap+22, sy+56); i++; } }
            int bx=420, by=140; g2.setColor(new Color(255,240,200)); g2.fillRoundRect(bx, by, 80, 60, 16,16); g2.setColor(Color.GRAY); g2.drawRoundRect(bx, by, 80, 60, 16,16);
            g2.setColor(Color.BLACK); g2.drawString("Barbero", bx+10, by-8);
            DrawUtil.person(g2, bx+40, by-30, new Color(120,220,160));
            int act; synchronized(this){ act=sillaAct; }
            if(act>0){ DrawUtil.person(g2, bx+40, by+20, new Color(200,210,255)); g2.setColor(Color.BLACK); g2.drawString("#"+act, bx+32, by+94); }
            g2.dispose();
        }
    }
}
