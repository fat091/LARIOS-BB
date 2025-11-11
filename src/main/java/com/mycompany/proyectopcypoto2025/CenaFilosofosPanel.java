
package com.mycompany.proyectopcypoto2025;
import javax.swing.*; import java.awt.*; import java.util.concurrent.Semaphore;
public class CenaFilosofosPanel extends JPanel implements Reseteable, Demoable, SyncAware, DeadlockAware {
    private final Mesa mesa = new Mesa(); private SyncMode modo=SyncMode.SEMAFOROS; private volatile boolean running=false; private Thread[] filos;
    private volatile boolean deadlocked=false;
    public CenaFilosofosPanel(){ setLayout(new BorderLayout()); add(mesa, BorderLayout.CENTER); start(); }
    private void start(){ if(running) return; running=true; filos=new Thread[5]; for(int i=0;i<5;i++){ final int id=i; filos[i]=new Thread(() -> ciclo(id), "F"+id); filos[i].start(); } }
    private final Semaphore camarero = new Semaphore(4,true); private final Semaphore[] ten = {new Semaphore(1),new Semaphore(1),new Semaphore(1),new Semaphore(1),new Semaphore(1)};
    private void ciclo(int id){ while(running && !deadlocked){ mesa.setEstado(id,Mesa.Estado.PENSANDO); dormir(220);
        try{ mesa.setEstado(id,Mesa.Estado.HAMBRIENTO);
            if(modo==SyncMode.SEMAFOROS) camarero.acquire();
            ten[id].acquire(); ten[(id+1)%5].acquire();
            mesa.ocupaTenedor(id,true); mesa.ocupaTenedor((id+1)%5,true);
            mesa.setEstado(id,Mesa.Estado.COMIENDO); dormir(260);
        } catch(Exception ignored){} finally {
            mesa.ocupaTenedor(id,false); mesa.ocupaTenedor((id+1)%5,false);
            ten[(id+1)%5].release(); ten[id].release();
            if(modo==SyncMode.SEMAFOROS) camarero.release();
        } } }
    @Override public void setSyncMode(SyncMode m){ this.modo=m; }
    @Override public void setDeadlock(boolean on){ deadlocked = on; if(on){ running=false; mesa.forceDeadlock(); } else { mesa.reset(); start(); } }
    @Override public void reset(){ running=false; mesa.reset(); start(); }
    @Override public void demo(){}
    private void dormir(long ms){ try{ Thread.sleep(ms);}catch(Exception ignored){} }

    static class Mesa extends JPanel {
        enum Estado{PENSANDO,HAMBRIENTO,COMIENDO}
        private final Estado[] est=new Estado[]{Estado.PENSANDO,Estado.PENSANDO,Estado.PENSANDO,Estado.PENSANDO,Estado.PENSANDO};
        private final boolean[] forkBusy=new boolean[5];
        public Mesa(){ setPreferredSize(new Dimension(560,300)); setBackground(Color.white); new Timer(33,e->repaint()).start(); }
        public synchronized void setEstado(int i, Estado e){ est[i]=e; }
        public synchronized void ocupaTenedor(int i, boolean b){ forkBusy[i]=b; }
        public synchronized void reset(){ for(int i=0;i<5;i++){ est[i]=Estado.PENSANDO; forkBusy[i]=false; } }
        public synchronized void forceDeadlock(){ for(int i=0;i<5;i++){ est[i]=Estado.HAMBRIENTO; forkBusy[i]=true; } }
        @Override protected void paintComponent(Graphics g){ super.paintComponent(g); Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int cx=getWidth()/2, cy=getHeight()/2; int R=110;
            // mesa
            g2.setColor(new Color(240,240,240)); g2.fillOval(cx-70, cy-45, 140, 90); g2.setColor(Color.GRAY); g2.drawOval(cx-70, cy-45, 140, 90);
            synchronized(this){
                // tenedores entre filósofos, ocultar si ocupado
                for(int i=0;i<5;i++){
                    double ang=i*2*Math.PI/5 - Math.PI/2 + Math.PI/5;
                    int x=cx+(int)(Math.cos(ang)*R*0.8), y=cy+(int)(Math.sin(ang)*R*0.8);
                    if(!forkBusy[i]){ g2.setColor(new Color(180,180,180)); g2.fillRoundRect(x-10,y-3,20,6,6,6); g2.setColor(Color.DARK_GRAY); g2.drawRoundRect(x-10,y-3,20,6,6,6); }
                }
                // filósofos
                for(int i=0;i<5;i++){
                    double ang=i*2*Math.PI/5 - Math.PI/2;
                    int x=cx+(int)(Math.cos(ang)*R), y=cy+(int)(Math.sin(ang)*R);
                    Color c = switch(est[i]){ case PENSANDO->new Color(180,200,255); case HAMBRIENTO->new Color(255,220,120); default->new Color(120,220,160); };
                    DrawUtil.person(g2, x, y, c);
                    g2.setColor(Color.BLACK); g2.drawString("F"+i, x-6, y+50);
                }
            }
            g2.dispose();
        }
    }
}
